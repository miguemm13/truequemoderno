package controller;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpExchange;
import model.Calificacion;
import model.Miembro;
import model.Transaccion;
import repository.MiembroRepository;
import repository.ReputacionRepository;
import repository.TransaccionRepository;
import util.JsonUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

// Envio y consulta de calificaciones entre 1 y 5 estrellas
public class ReputacionController extends BaseController {

    private final ReputacionRepository repository;
    private final MiembroRepository miembroRepository;
    private final TransaccionRepository transaccionRepository;

    public ReputacionController(ReputacionRepository repository,
                                MiembroRepository miembroRepository,
                                TransaccionRepository transaccionRepository) {
        this.repository = repository;
        this.miembroRepository = miembroRepository;
        this.transaccionRepository = transaccionRepository;
    }

    @Override
    protected void procesarSolicitud(HttpExchange exchange) throws Exception {
        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();

        if ("POST".equalsIgnoreCase(method)) {
            if ("/api/reputacion/calificar".equals(path)) {
                String body = leerBody(exchange);
                if (body == null || body.trim().isEmpty()) {
                    enviarError(exchange, 400, "El cuerpo de la solicitud no puede estar vacio.");
                    return;
                }
                procesarEvaluacion(exchange, body);
            } else {
                enviarError(exchange, 404, "Ruta POST no encontrada.");
            }
        } else if ("GET".equalsIgnoreCase(method)) {
            if ("/api/reputacion/usuario".equals(path)) {
                obtenerCalificacionesUsuario(exchange);
            } else {
                enviarError(exchange, 404, "Ruta GET no encontrada.");
            }
        } else {
            enviarError(exchange, 405, "Metodo no permitido.");
        }
    }

    
    private void procesarEvaluacion(HttpExchange exchange, String body) throws IOException {
        JsonObject json;
        try {
            json = JsonParser.parseString(body).getAsJsonObject();
        } catch (Exception e) {
            retornarErrorValidacion(exchange, "JSON invalido.");
            return;
        }

        if (json.get("idEvaluador") == null || json.get("idEvaluador").getAsString().trim().isEmpty() ||
            json.get("idTrans") == null || json.get("idTrans").getAsString().trim().isEmpty() ||
            json.get("estrellas") == null || json.get("comentario") == null) {
            retornarErrorValidacion(exchange, "Todos los campos son obligatorios (idEvaluador, idTrans, estrellas, comentario).");
            return;
        }

        String idEvaluador = json.get("idEvaluador").getAsString().trim();
        String idTrans = json.get("idTrans").getAsString().trim();
        int estrellas;
        try {
            estrellas = json.get("estrellas").getAsInt();
        } catch (NumberFormatException | ClassCastException e) {
            retornarErrorValidacion(exchange, "El valor de estrellas debe ser entero.");
            return;
        }

        if (estrellas < 1 || estrellas > 5) {
            retornarErrorValidacion(exchange, "La puntuacion debe estar entre 1 y 5 estrellas.");
            return;
        }

        String comentario = json.get("comentario").getAsString().trim();

        Transaccion trans = null;
        List<Transaccion> txs = transaccionRepository.obtenerTodas();
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

        if (!verificarElegibilidad(idEvaluador, trans)) {
            enviarError(exchange, 403, "No autorizado. Solo participantes pueden calificar y el intercambio debe estar 'Finalizado con exito'.");
            return;
        }

        List<Calificacion> califs = repository.obtenerTodas();
        for (Calificacion c : califs) {
            if (c.getId_transaccion().equals(idTrans) && c.getId_evaluador().equals(idEvaluador)) {
                retornarErrorValidacion(exchange, "Accion no permitida. Ya has enviado una calificacion para este intercambio.");
                return;
            }
        }

        String idCalificado = idEvaluador.equals(trans.getId_receptor()) 
                ? trans.getId_prestador() 
                : trans.getId_receptor();

        Miembro calificadoMiembro = null;
        List<Miembro> miembros = miembroRepository.obtenerTodos();
        for (Miembro m : miembros) {
            if (m.getId().equals(idCalificado)) {
                calificadoMiembro = m;
                break;
            }
        }

        if (calificadoMiembro == null) {
            retornarErrorValidacion(exchange, "El miembro calificado no existe.");
            return;
        }

        calificadoMiembro.recalcularPromedio(estrellas);

        String idCalificacion = generarSiguienteId();
        Calificacion calif = new Calificacion(idCalificacion, idTrans, idEvaluador, idCalificado, estrellas, comentario);

        boolean guardadoExitoso = repository.persistirReputacionAtomica(calif, calificadoMiembro, miembroRepository);
        if (!guardadoExitoso) {
            enviarError(exchange, 500, "Error de escritura en persistencia. Reintente.");
            return;
        }

        enviarRespuesta(exchange, 201, JsonUtils.toJson(calif));
    }

    
    private boolean verificarElegibilidad(String idEvaluador, Transaccion trans) {
        if (trans == null || idEvaluador == null) {
            return false;
        }
        if (!"Finalizado con exito".equalsIgnoreCase(trans.getEstado())) {
            return false;
        }
        return idEvaluador.equals(trans.getId_receptor()) || idEvaluador.equals(trans.getId_prestador());
    }

    
    private void obtenerCalificacionesUsuario(HttpExchange exchange) throws IOException {
        String query = exchange.getRequestURI().getQuery();
        if (query == null) {
            enviarError(exchange, 400, "Faltan parametros de consulta.");
            return;
        }

        String idUsuario = null;
        String idConsultante = null;
        String idEvaluador = null;

        String[] pairs = query.split("&");
        for (String pair : pairs) {
            String[] kv = pair.split("=");
            if (kv.length == 2) {
                if ("idUsuario".equalsIgnoreCase(kv[0])) {
                    idUsuario = kv[1];
                } else if ("idConsultante".equalsIgnoreCase(kv[0])) {
                    idConsultante = kv[1];
                } else if ("idEvaluador".equalsIgnoreCase(kv[0])) {
                    idEvaluador = kv[1];
                }
            }
        }

        if ((idUsuario == null || idUsuario.trim().isEmpty()) && (idEvaluador == null || idEvaluador.trim().isEmpty())) {
            enviarError(exchange, 400, "Debe indicar idUsuario o idEvaluador en la consulta.");
            return;
        }

        List<Calificacion> todas = repository.obtenerTodas();
        List<Calificacion> filtradas = new ArrayList<>();

        if (idEvaluador != null && !idEvaluador.trim().isEmpty()) {
            idEvaluador = idEvaluador.trim();
            for (Calificacion c : todas) {
                if (c.getId_evaluador().equals(idEvaluador)) {
                    filtradas.add(c);
                }
            }
        } else {
            idUsuario = idUsuario.trim();
            if (idConsultante != null) {
                idConsultante = idConsultante.trim();
            }

            for (Calificacion c : todas) {
                if (c.getId_calificado().equals(idUsuario)) {
                    boolean tieneCalificacionCorrespondiente = false;
                    
                    if (idConsultante != null && idConsultante.equals(c.getId_evaluador())) {
                        // El autor de la calificación siempre puede ver su propio comentario
                        tieneCalificacionCorrespondiente = true;
                    } else {
                        // Para cualquier otro consultante (incluido el calificado y terceros),
                        // se requiere que el calificado haya calificado al evaluador en esta transacción.
                        for (Calificacion other : todas) {
                            if (other.getId_transaccion().equals(c.getId_transaccion()) && 
                                other.getId_evaluador().equals(c.getId_calificado()) &&
                                other.getId_calificado().equals(c.getId_evaluador())) {
                                tieneCalificacionCorrespondiente = true;
                                break;
                            }
                        }
                    }

                    if (!tieneCalificacionCorrespondiente) {
                        c.setComentario("[Comentario oculto hasta que califiques al contraparte]");
                    }

                    filtradas.add(c);
                }
            }
        }

        enviarRespuesta(exchange, 200, JsonUtils.toJson(filtradas));
    }

    private synchronized String generarSiguienteId() {
        List<Calificacion> calificaciones = repository.obtenerTodas();
        int maxId = 0;
        for (Calificacion c : calificaciones) {
            String idStr = c.getId_calificacion();
            if (idStr != null && idStr.startsWith("CAL-")) {
                try {
                    int num = Integer.parseInt(idStr.substring(4));
                    if (num > maxId) {
                        maxId = num;
                    }
                } catch (NumberFormatException e) {
                }
            }
        }
        return String.format("CAL-%03d", maxId + 1);
    }
}
