package controller;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpExchange;
import model.Billetera;
import model.Miembro;
import model.OfertaTalento;
import model.Transaccion;
import repository.MiembroRepository;
import repository.OfertaRepository;
import repository.TransaccionRepository;
import util.JsonUtils;

import java.io.IOException;
import java.util.List;

// Solicitudes, aceptacion y cierre de trueques entre miembros
public class TransaccionController extends BaseController {

    private final TransaccionRepository repository;
    private final MiembroRepository miembroRepository;
    private final OfertaRepository ofertaRepository;

    public TransaccionController(TransaccionRepository repository, 
                                 MiembroRepository miembroRepository, 
                                 OfertaRepository ofertaRepository) {
        this.repository = repository;
        this.miembroRepository = miembroRepository;
        this.ofertaRepository = ofertaRepository;
    }

    @Override
    protected void procesarSolicitud(HttpExchange exchange) throws Exception {
        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();

        if ("POST".equalsIgnoreCase(method)) {
            if ("/api/transacciones/solicitar".equals(path)) {
                String body = leerBody(exchange);
                if (body == null || body.trim().isEmpty()) {
                    enviarError(exchange, 400, "El cuerpo de la solicitud no puede estar vacio.");
                    return;
                }
                iniciarSolicitud(exchange, body);
            } else if ("/api/transacciones/gestionar".equals(path)) {
                String body = leerBody(exchange);
                if (body == null || body.trim().isEmpty()) {
                    enviarError(exchange, 400, "El cuerpo de la solicitud no puede estar vacio.");
                    return;
                }
                procesarGestion(exchange, body);
            } else if ("/api/transacciones/confirmar".equals(path)) {
                String body = leerBody(exchange);
                if (body == null || body.trim().isEmpty()) {
                    enviarError(exchange, 400, "El cuerpo de la solicitud no puede estar vacio.");
                    return;
                }
                procesarTransferencia(exchange, body);
            } else {
                enviarError(exchange, 404, "Ruta POST no encontrada.");
            }
        } else if ("GET".equalsIgnoreCase(method)) {
            if ("/api/transacciones/usuario".equals(path)) {
                obtenerTransaccionesUsuario(exchange);
            } else {
                enviarError(exchange, 404, "Ruta GET no encontrada.");
            }
        } else {
            enviarError(exchange, 405, "Metodo no permitido.");
        }
    }

    private void obtenerTransaccionesUsuario(HttpExchange exchange) throws IOException {
        String query = exchange.getRequestURI().getQuery();
        if (query == null || !query.contains("idUsuario=")) {
            enviarError(exchange, 400, "Debe proveer idUsuario en la query.");
            return;
        }
        String idUsuario = query.split("idUsuario=")[1].split("&")[0];
        
        List<Transaccion> todas = repository.obtenerTodas();
        List<Transaccion> filtradas = new java.util.ArrayList<>();
        for (Transaccion t : todas) {
            if (idUsuario.equals(t.getId_receptor()) || idUsuario.equals(t.getId_prestador())) {
                filtradas.add(t);
            }
        }
        enviarRespuesta(exchange, 200, JsonUtils.toJson(filtradas));
    }

    /**
     * HU-03: Inicia una solicitud de intercambio de servicio y verifica la solvencia del saldo preventivo.
     */
    private void iniciarSolicitud(HttpExchange exchange, String body) throws IOException {
        JsonObject json;
        try {
            json = JsonParser.parseString(body).getAsJsonObject();
        } catch (Exception e) {
            retornarErrorValidacion(exchange, "JSON invalido.");
            return;
        }

        if (json.get("idReceptor") == null || json.get("idReceptor").getAsString().trim().isEmpty() ||
            json.get("idOferta") == null || json.get("idOferta").getAsString().trim().isEmpty() ||
            json.get("horas") == null) {
            retornarErrorValidacion(exchange, "Todos los campos son obligatorios (idReceptor, idOferta, horas).");
            return;
        }

        String idReceptor = json.get("idReceptor").getAsString().trim();
        String idOferta = json.get("idOferta").getAsString().trim();
        
        double horas;
        try {
            horas = json.get("horas").getAsDouble();
        } catch (NumberFormatException | ClassCastException e) {
            retornarErrorValidacion(exchange, "El valor de horas debe ser numerico.");
            return;
        }

        // 1. Lee receptor Billetera para usuarios.json
        Miembro receptor = null;
        List<Miembro> miembros = miembroRepository.obtenerTodos();
        for (Miembro m : miembros) {
            if (m.getId().equals(idReceptor)) {
                receptor = m;
                break;
            }
        }
        if (receptor == null) {
            retornarErrorValidacion(exchange, "El idReceptor especificado no corresponde a ningun miembro registrado.");
            return;
        }

        // 2. Lee Oferta para ofertas.json asi identifica al Prestador
        OfertaTalento oferta = null;
        List<OfertaTalento> ofertas = ofertaRepository.obtenerTodas();
        for (OfertaTalento o : ofertas) {
            if (o.getId_oferta().equals(idOferta)) {
                oferta = o;
                break;
            }
        }
        if (oferta == null) {
            retornarErrorValidacion(exchange, "La oferta especificada no existe.");
            return;
        }

        String idPrestador = oferta.getId_creador();

        if (idReceptor.equals(idPrestador)) {
            retornarErrorValidacion(exchange, "No puedes solicitar tu propia oferta.");
            return;
        }

        // Saldo proyectado: no pasar de -2.00 h (limite de la billetera)
        double totalHorasComprometidas = 0.0;
        List<Transaccion> txs = repository.obtenerTodas();
        for (Transaccion t : txs) {
            if (t.getId_receptor().equals(idReceptor) && 
                ("Solicitada".equalsIgnoreCase(t.getEstado()) || "Iniciada".equalsIgnoreCase(t.getEstado()))) {
                totalHorasComprometidas += t.getHoras();
            }
        }

        Billetera billeteraReceptor = receptor.getBilletera();
        if (billeteraReceptor.getLimite() == 0.0) {
            billeteraReceptor.setLimite(-2.00);
        }

        double balanceProyectado = billeteraReceptor.getSaldo() - totalHorasComprometidas - horas;
        if (balanceProyectado < billeteraReceptor.getLimite()) {
            retornarErrorValidacion(exchange,
                "Llegaste al tope de deuda (-2.00 h). Libera solicitudes pendientes o finaliza intercambios antes de pedir mas horas.");
            return;
        }

        // 4. Crea una nueva transaccion con estado "Solicitada" y asigna receptor y prestador
        String idTrans = generarSiguienteId();
        Transaccion transaccion = new Transaccion(idTrans, horas, "Solicitada");
        transaccion.asignarReceptor(idReceptor);
        transaccion.asignarPrestador(idPrestador);
        transaccion.setId_oferta(idOferta);

        // 5. Guarda la transaccion en transacciones.json de manera atomica
        boolean guardadoExitoso = repository.guardarTransaccionAtomica(transaccion);
        if (!guardadoExitoso) {
            enviarError(exchange, 500, "Error de escritura en transacciones.json. Reintente.");
            return;
        }

        // Respond with success
        enviarRespuesta(exchange, 201, JsonUtils.toJson(transaccion));
    }

    /**
     * HU-04: Procesa la gestion de una solicitud de trueque (aceptar o rechazar) y actualiza el estado de la transaccion y la oferta.
     * La decision solo puede ser tomada por el prestador de la oferta.
     */
    private void procesarGestion(HttpExchange exchange, String body) throws IOException {
        JsonObject json;
        try {
            json = JsonParser.parseString(body).getAsJsonObject();
        } catch (Exception e) {
            retornarErrorValidacion(exchange, "JSON invalido.");
            return;
        }

        if (json.get("idPrestador") == null || json.get("idPrestador").getAsString().trim().isEmpty() ||
            json.get("idTrans") == null || json.get("idTrans").getAsString().trim().isEmpty() ||
            json.get("decision") == null || json.get("decision").getAsString().trim().isEmpty()) {
            retornarErrorValidacion(exchange, "Todos los campos son obligatorios (idPrestador, idTrans, decision).");
            return;
        }

        String idPrestadorInput = json.get("idPrestador").getAsString().trim();
        String idTrans = json.get("idTrans").getAsString().trim();
        String decision = json.get("decision").getAsString().trim();

        // 1. Lee transaccion en transacciones.json
        Transaccion trans = null;
        List<Transaccion> txs = repository.obtenerTodas();
        for (Transaccion t : txs) {
            if (t.getId_transaccion().equals(idTrans)) {
                trans = t;
                break;
            }
        }

        if (trans == null) {
            retornarErrorValidacion(exchange, "La transaccion especificada no existe.");
            return;
        }

        // 2. Autorizacion: Solo el prestador de la oferta puede aceptar o rechazar la solicitud
        if (!verificarAutorizacion(idPrestadorInput, trans.getId_prestador())) {
            enviarError(exchange, 403, "No autorizado. Solo el prestador de esta oferta puede gestionarla.");
            return;
        }

        if ("Aceptar".equalsIgnoreCase(decision)) {
            trans.actualizarEstado("Iniciada");
            
            boolean guardadoExitoso = repository.guardarTransaccionAtomica(trans);
            if (!guardadoExitoso) {
                enviarError(exchange, 500, "Error al guardar los cambios en la transaccion.");
                return;
            }

            cambiarEstadoOferta(trans.getId_oferta(), "En Proceso");
            enviarRespuesta(exchange, 200, JsonUtils.toJson(trans));

        } else if ("Rechazar".equalsIgnoreCase(decision)) {
            trans.actualizarEstado("Cancelada por Prestador");

            Miembro receptor = null;
            List<Miembro> miembros = miembroRepository.obtenerTodos();
            for (Miembro m : miembros) {
                if (m.getId().equals(trans.getId_receptor())) {
                    receptor = m;
                    break;
                }
            }

            Billetera billeteraReceptor = (receptor != null) ? receptor.getBilletera() : new Billetera(0.0, -2.00);
            
            billeteraReceptor.liberarSaldoPreventivo(trans.getHorasPactadas());

            boolean guardadoExitoso = repository.persistirCambiosRechazo(trans, billeteraReceptor);
            if (!guardadoExitoso) {
                enviarError(exchange, 500, "Error al persistir cambios del rechazo.");
                return;
            }
            cambiarEstadoOferta(trans.getId_oferta(), "Activa");
            enviarRespuesta(exchange, 200, JsonUtils.toJson(trans));

        } else {
            retornarErrorValidacion(exchange, "Decision invalida. Debe ser 'Aceptar' o 'Rechazar'.");
        }
    }

    /**
     * hu 5 delivery de proceso y tranferencia atomica 
     */
    private void procesarTransferencia(HttpExchange exchange, String body) throws IOException {
        JsonObject json;
        try {
            json = JsonParser.parseString(body).getAsJsonObject();
        } catch (Exception e) {
            retornarErrorValidacion(exchange, "JSON invalido.");
            return;
        }

        if (json.get("idMiembroSesion") == null || json.get("idMiembroSesion").getAsString().trim().isEmpty() ||
            json.get("idTrans") == null || json.get("idTrans").getAsString().trim().isEmpty()) {
            retornarErrorValidacion(exchange, "Todos los campos son obligatorios (idMiembroSesion, idTrans).");
            return;
        }

        String idMiembroSesion = json.get("idMiembroSesion").getAsString().trim();
        String idTrans = json.get("idTrans").getAsString().trim();

        // 1. Lee transaccion en transacciones.json
        Transaccion trans = null;
        List<Transaccion> txs = repository.obtenerTodas();
        for (Transaccion t : txs) {
            if (t.getId_transaccion().equals(idTrans)) {
                trans = t;
                break;
            }
        }

        if (trans == null) {
            retornarErrorValidacion(exchange, "La transaccion especificada no existe.");
            return;
        }

        // 2. Autorizacion: Solo el receptor de la solicitud puede confirmar el cumplimiento del trueque
        if (!verificarAutorizacion(idMiembroSesion, trans.getId_receptor())) {
            enviarError(exchange, 403, "No autorizado. Solo el receptor de esta solicitud puede confirmar cumplimiento.");
            return;
        }

        // 3. Valida que la transaccion este en estado "Iniciada" para poder cerrarse
        if (!"Iniciada".equalsIgnoreCase(trans.getEstado())) {
            retornarErrorValidacion(exchange, "Accion rechazada. La transaccion debe estar en estado 'Iniciada' para poder finalizarse.");
            return;
        }

        // 4. Revisa que el receptor tenga saldo suficiente para cubrir la transferencia de horas pactadas
        Miembro receptor = null;
        Miembro prestador = null;
        List<Miembro> miembros = miembroRepository.obtenerTodos();
        for (Miembro m : miembros) {
            if (m.getId().equals(trans.getId_receptor())) {
                receptor = m;
            } else if (m.getId().equals(trans.getId_prestador())) {
                prestador = m;
            }
        }

        if (receptor == null || prestador == null) {
            retornarErrorValidacion(exchange, "Error al cargar los miembros vinculados a la transaccion.");
            return;
        }

        Billetera bRec = receptor.getBilletera();
        Billetera bPre = prestador.getBilletera();

        if (bRec.getLimite() == 0.0) bRec.setLimite(-2.00);
        if (bPre.getLimite() == 0.0) bPre.setLimite(-2.00);

        // 5. Solvencia: el receptor no puede quedar por debajo del limite de -2.00 horas
        double balanceProyectadoCierre = bRec.getSaldo() - trans.getHorasPactadas();
        if (balanceProyectadoCierre < bRec.getLimite()) {
            retornarErrorValidacion(exchange,
                "No puedes cerrar este trueque: tu saldo quedaria por debajo de -2.00 horas.");
            return;
        }

        // 6. Transfiere horas de manera atomica: debita al receptor y acredita al prestador
        bRec.debitarHoras(trans.getHorasPactadas());
        bPre.acreditarHoras(trans.getHorasPactadas());

        // 7. Transiciona el estado de la transaccion a "Finalizado con exito"
        trans.actualizarEstado("Finalizado con exito");

        // 8. Atomic commit: guarda cambios en transacciones.json y actualiza saldos en usuarios.json
        boolean commitExitoso = repository.ejecutarTransferenciaAtomica(trans, bRec, bPre, miembroRepository);
        if (!commitExitoso) {
            enviarError(exchange, 500, "Error en el commit de la transferencia. Integridad de saldos preservada.");
            return;
        }

        cambiarEstadoOferta(trans.getId_oferta(), "Finalizada");

        JsonObject respuesta = new JsonObject();
        respuesta.add("transaccion", JsonParser.parseString(JsonUtils.toJson(trans)).getAsJsonObject());
        respuesta.addProperty("saldoActualizado", bRec.getSaldo());
        enviarRespuesta(exchange, 200, respuesta.toString());
    }

    private void cambiarEstadoOferta(String idOferta, String nuevoEstado) {
        if (idOferta == null) {
            return;
        }
        for (model.OfertaTalento o : ofertaRepository.obtenerTodas()) {
            if (idOferta.equals(o.getId_oferta())) {
                o.setEstado(nuevoEstado);
                ofertaRepository.actualizarOferta(o);
                return;
            }
        }
    }

    /**
     * HU-04: Verifica que el miembro que intenta calificar a otro haya participado en la transaccion y que esta haya finalizado con exito.
     */
    private boolean verificarAutorizacion(String idSesion, String idTransUsuario) {
        if (idSesion == null || idTransUsuario == null) {
            return false;
        }
        return idSesion.equals(idTransUsuario);
    }

    private synchronized String generarSiguienteId() {
        List<Transaccion> transacciones = repository.obtenerTodas();
        int maxId = 0;
        for (Transaccion t : transacciones) {
            String idStr = t.getId_transaccion();
            if (idStr != null && idStr.startsWith("TX-")) {
                try {
                    int num = Integer.parseInt(idStr.substring(3));
                    if (num > maxId) {
                        maxId = num;
                    }
                } catch (NumberFormatException e) {
                    // Ignore non-numeric IDs
                }
            }
        }
        return String.format("TX-%03d", maxId + 1);
    }
}
