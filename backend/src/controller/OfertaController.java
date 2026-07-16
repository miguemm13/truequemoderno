package controller;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpExchange;
import model.OfertaTalento;
import repository.MiembroRepository;
import repository.OfertaRepository;
import service.CatalogoService;
import util.JsonUtils;
import model.Transaccion;
import repository.TransaccionRepository;

import java.io.IOException;
import java.util.List;

// Alta y consulta de ofertas del muro comunitario
public class OfertaController extends BaseController {

    private final OfertaRepository repository;
    private final MiembroRepository miembroRepository;
    private final TransaccionRepository transaccionRepository;

    public OfertaController(OfertaRepository repository, MiembroRepository miembroRepository, TransaccionRepository transaccionRepository) {
        this.repository = repository;
        this.miembroRepository = miembroRepository;
        this.transaccionRepository = transaccionRepository;
    }

@Override
protected void procesarSolicitud(HttpExchange exchange) throws Exception {
    String path = exchange.getRequestURI().getPath();
    String method = exchange.getRequestMethod();

    // 1. Limpieza de barras duplicadas o finales
    // Si viene "/api/ofertas/actualizar/" lo convierte en "/api/ofertas/actualizar"
    if (path.endsWith("/") && path.length() > 1) {
        path = path.substring(0, path.length() - 1);
    }

    System.out.println("📡 [OfertaController] Solicitud recibida: " + method + " '" + path + "'");

  if ("POST".equalsIgnoreCase(method)) {
    
    // Imprime el path exacto en tu consola de Java para que veas qué llega:
    System.out.println("DEBUG: El path recibido es exactamente -> " + path);

    // Comparamos permitiendo que sea la ruta completa o la abreviada
    if (path.equals("/api/ofertas/publicar") || path.equals("/publicar")) {
        String body = leerBody(exchange);
        if (body == null || body.trim().isEmpty()) {
            enviarError(exchange, 400, "El cuerpo de la solicitud no puede estar vacio.");
            return;
        }
        publicarOferta(exchange, body);
    } 
    // AQUÍ: Hacemos que acepte "/api/ofertas/actualizar" o "/actualizar"
    else if (path.equals("/api/ofertas/actualizar") || path.equals("/actualizar")) {
        String body = leerBody(exchange);
        if (body == null || body.trim().isEmpty()) {
            enviarError(exchange, 400, "El cuerpo de la solicitud no puede estar vacio.");
            return;
        }
        actualizarOferta(exchange, body);
    } 
    else if (path.equals("/api/ofertas/eliminar") || path.equals("/eliminar")) {
        String body = leerBody(exchange);
        if (body == null || body.trim().isEmpty()) {
            enviarError(exchange, 400, "El cuerpo de la solicitud no puede estar vacio.");
            return;
        }
        eliminarOferta(exchange, body);
    }
    else {
        // Este es el error que te estaba saliendo en Angular:
        enviarError(exchange, 404, "Ruta POST no encontrada. Path recibido: " + path);
    }
} else if ("GET".equalsIgnoreCase(method)) {
        if (path.equals("/api/ofertas/muro")) {
            obtenerMuro(exchange);
        } else if (path.equals("/api/ofertas/por-creador")) {
            obtenerPorCreador(exchange);
        } else if (path.equals("/api/ofertas/listar")) {
            listarTodas(exchange);
        } else if (path.equals("/api/ofertas/chequear-eliminacion") || path.equals("/chequear-eliminacion")) {
            chequearEliminacion(exchange);
        } 
        // 2. Manejo dinámico seguro de IDs tipo /api/ofertas/OFR-010
       else if (path.startsWith("/api/ofertas/OFR-") || path.startsWith("/OFR-")) {
            obtenerOfertaPorId(exchange, path);
        } else {
            enviarError(exchange, 404, "Ruta GET no encontrada: " + path);
        }
    } else {
        enviarError(exchange, 405, "Metodo no permitido.");
    }
}

    private void publicarOferta(HttpExchange exchange, String body) throws IOException {
        JsonObject json;
        try {
            json = JsonParser.parseString(body).getAsJsonObject();
        } catch (Exception e) {
            retornarErrorValidacion(exchange, "JSON invalido.");
            return;
        }

        if (json.get("idCreador") == null || json.get("idCreador").getAsString().trim().isEmpty() ||
            json.get("titulo") == null || json.get("titulo").getAsString().trim().isEmpty() ||
            json.get("descripcion") == null || json.get("descripcion").getAsString().trim().isEmpty() ||
            json.get("idCategoria") == null || json.get("idCategoria").getAsString().trim().isEmpty() ||
            json.get("horas") == null) {
            retornarErrorValidacion(exchange, "Todos los campos son obligatorios (idCreador, titulo, descripcion, horas, idCategoria).");
            return;
        }

        String idCreador = json.get("idCreador").getAsString().trim();
        String titulo = json.get("titulo").getAsString().trim();
        String descripcion = json.get("descripcion").getAsString().trim();
        String idCategoria = json.get("idCategoria").getAsString().trim();
        double horas;
        try {
            horas = json.get("horas").getAsDouble();
        } catch (NumberFormatException | ClassCastException e) {
            retornarErrorValidacion(exchange, "El valor de horas estimadas debe ser numerico.");
            return;
        }

        if (!CatalogoService.validarCategoria(idCategoria)) {
            retornarErrorValidacion(exchange, "Categoria invalida. Use CAT-001 a CAT-012.");
            return;
        }
        if (!validarRangoValor(horas)) {
            retornarErrorValidacion(exchange, "Valor fuera de rango. El limite permitido es entre 0.5 y 20.0 Horas de Vida.");
            return;
        }

        boolean creadorExiste = false;
        List<model.Miembro> miembros = miembroRepository.obtenerTodos();
        for (model.Miembro m : miembros) {
            if (m.getId().equals(idCreador)) {
                creadorExiste = true;
                break;
            }
        }
        if (!creadorExiste) {
            retornarErrorValidacion(exchange, "El idCreador especificado no corresponde a ningun miembro registrado.");
            return;
        }

        String nextId = generarSiguienteId();
        OfertaTalento nuevaOferta = new OfertaTalento(
                nextId,
                idCreador,
                titulo,
                descripcion,
                idCategoria,
                horas,
                "Activa"
        );

        boolean guardadoExitoso = repository.guardarOfertaAtomica(nuevaOferta);
        if (!guardadoExitoso) {
            enviarError(exchange, 500, "Error de escritura en ofertas.json. Reintente.");
            return;
        }

        enviarRespuesta(exchange, 201, JsonUtils.toJson(nuevaOferta));
    }

    private void obtenerMuro(HttpExchange exchange) throws IOException {
        List<OfertaTalento> ofertas = repository.obtenerTodas();
        List<OfertaTalento> activas = new java.util.ArrayList<>();
        for (OfertaTalento o : ofertas) {
            if (o.getEstado() != null && "Activa".equalsIgnoreCase(o.getEstado().trim())) {
                activas.add(o);
            }
        }
        enviarRespuesta(exchange, 200, JsonUtils.toJson(activas));
    }

    private void listarTodas(HttpExchange exchange) throws IOException {
        enviarRespuesta(exchange, 200, JsonUtils.toJson(repository.obtenerTodas()));
    }

    private void obtenerPorCreador(HttpExchange exchange) throws IOException {
        String query = exchange.getRequestURI().getQuery();
        if (query == null) {
            enviarError(exchange, 400, "Debe indicar idCreador en la consulta.");
            return;
        }
        
        String idCreador = null;
        String[] pairs = query.split("&");
        for (String pair : pairs) {
            String[] kv = pair.split("=");
            if (kv.length == 2) {
                if ("idCreador".equalsIgnoreCase(kv[0].trim())) {
                    idCreador = kv[1].trim();
                }
            }
        }

        if (idCreador == null || idCreador.isEmpty()) {
            enviarError(exchange, 400, "Debe indicar idCreador en la consulta.");
            return;
        }

        List<OfertaTalento> delUsuario = new java.util.ArrayList<>();
        for (OfertaTalento o : repository.obtenerTodas()) {
            if (idCreador.equals(o.getId_creador())) {
                delUsuario.add(o);
            }
        }
        enviarRespuesta(exchange, 200, JsonUtils.toJson(delUsuario));
    }

    // ================== NUEVOS MÉTODOS PARA EDICIÓN ==================

    /**
     * Obtiene una oferta por su ID (GET /api/ofertas/{id})
     */
    private void obtenerOfertaPorId(HttpExchange exchange, String path) throws IOException {
        String id = path.substring(path.lastIndexOf('/') + 1);
        OfertaTalento oferta = repository.obtenerPorId(id);
        if (oferta == null) {
            enviarError(exchange, 404, "Oferta no encontrada.");
            return;
        }
        enviarRespuesta(exchange, 200, JsonUtils.toJson(oferta));
    }

    /**
     * Actualiza una oferta existente (POST /api/ofertas/actualizar)
     */
    private void actualizarOferta(HttpExchange exchange, String body) throws IOException {
        JsonObject json;
        try {
            json = JsonParser.parseString(body).getAsJsonObject();
        } catch (Exception e) {
            retornarErrorValidacion(exchange, "JSON invalido.");
            return;
        }

        if (json.get("idOferta") == null || json.get("idOferta").getAsString().trim().isEmpty() ||
            json.get("titulo") == null || json.get("titulo").getAsString().trim().isEmpty() ||
            json.get("descripcion") == null || json.get("descripcion").getAsString().trim().isEmpty() ||
            json.get("idCategoria") == null || json.get("idCategoria").getAsString().trim().isEmpty() ||
            json.get("horas") == null) {
            retornarErrorValidacion(exchange, "Todos los campos son obligatorios (idOferta, titulo, descripcion, horas, idCategoria).");
            return;
        }

        String idOferta = json.get("idOferta").getAsString().trim();
        String titulo = json.get("titulo").getAsString().trim();
        String descripcion = json.get("descripcion").getAsString().trim();
        String idCategoria = json.get("idCategoria").getAsString().trim();
        double horas;
        try {
            horas = json.get("horas").getAsDouble();
        } catch (NumberFormatException e) {
            retornarErrorValidacion(exchange, "Horas debe ser numerico.");
            return;
        }

        if (!CatalogoService.validarCategoria(idCategoria)) {
            retornarErrorValidacion(exchange, "Categoria invalida.");
            return;
        }
        if (horas < 0.5 || horas > 20.0) {
            retornarErrorValidacion(exchange, "Horas fuera de rango (0.5 - 20.0).");
            return;
        }

        OfertaTalento ofertaExistente = repository.obtenerPorId(idOferta);
        if (ofertaExistente == null) {
            enviarError(exchange, 404, "Oferta no encontrada.");
            return;
        }

        if (json.get("idUsuario") == null || json.get("idUsuario").getAsString().trim().isEmpty()) {
            retornarErrorValidacion(exchange, "Se requiere idUsuario para autenticar la edicion.");
            return;
        }
        String idUsuario = json.get("idUsuario").getAsString().trim();
        if (!ofertaExistente.getId_creador().equals(idUsuario)) {
            enviarError(exchange, 403, "No eres el creador de esta oferta.");
            return;
        }

        ofertaExistente.setTitulo(titulo);
        ofertaExistente.setDescripcion(descripcion);
        ofertaExistente.setCategoria(idCategoria);
        ofertaExistente.setHoras_estimadas(horas);

        boolean actualizado = repository.actualizarOferta(ofertaExistente);
        if (!actualizado) {
            enviarError(exchange, 500, "Error al guardar los cambios.");
            return;
        }

        enviarRespuesta(exchange, 200, JsonUtils.toJson(ofertaExistente));
    }

    // ================== FIN NUEVOS MÉTODOS ==================

    private void chequearEliminacion(HttpExchange exchange) throws IOException {
        String query = exchange.getRequestURI().getQuery();
        if (query == null) {
            enviarError(exchange, 400, "Debe indicar idOferta en la consulta.");
            return;
        }

        String idOferta = null;
        String[] pairs = query.split("&");
        for (String pair : pairs) {
            String[] kv = pair.split("=");
            if (kv.length == 2) {
                if ("idOferta".equalsIgnoreCase(kv[0].trim())) {
                    idOferta = kv[1].trim();
                }
            }
        }

        if (idOferta == null || idOferta.isEmpty()) {
            enviarError(exchange, 400, "Debe indicar idOferta en la consulta.");
            return;
        }

        List<Transaccion> txs = transaccionRepository.obtenerTodas();
        boolean tieneEnCurso = false;
        int pendientesCount = 0;

        for (Transaccion t : txs) {
            if (idOferta.equals(t.getId_oferta())) {
                if ("Iniciada".equalsIgnoreCase(t.getEstado())) {
                    tieneEnCurso = true;
                } else if ("Solicitada".equalsIgnoreCase(t.getEstado())) {
                    pendientesCount++;
                }
            }
        }

        JsonObject resp = new JsonObject();
        resp.addProperty("tieneEnCurso", tieneEnCurso);
        resp.addProperty("pendientesCount", pendientesCount);
        enviarRespuesta(exchange, 200, resp.toString());
    }

    private void eliminarOferta(HttpExchange exchange, String body) throws IOException {
        JsonObject json;
        try {
            json = JsonParser.parseString(body).getAsJsonObject();
        } catch (Exception e) {
            retornarErrorValidacion(exchange, "JSON invalido.");
            return;
        }

        if (json.get("idOferta") == null || json.get("idOferta").getAsString().trim().isEmpty() ||
            json.get("idUsuario") == null || json.get("idUsuario").getAsString().trim().isEmpty()) {
            retornarErrorValidacion(exchange, "Todos los campos son obligatorios (idOferta, idUsuario).");
            return;
        }

        String idOferta = json.get("idOferta").getAsString().trim();
        String idUsuario = json.get("idUsuario").getAsString().trim();

        OfertaTalento oferta = repository.obtenerPorId(idOferta);
        if (oferta == null) {
            enviarError(exchange, 404, "Oferta no encontrada.");
            return;
        }

        if (!oferta.getId_creador().equals(idUsuario)) {
            enviarError(exchange, 403, "No eres el creador de esta oferta.");
            return;
        }

        List<Transaccion> txs = transaccionRepository.obtenerTodas();
        boolean tieneEnCurso = false;
        java.util.List<Transaccion> pendientes = new java.util.ArrayList<>();

        for (Transaccion t : txs) {
            if (idOferta.equals(t.getId_oferta())) {
                if ("Iniciada".equalsIgnoreCase(t.getEstado())) {
                    tieneEnCurso = true;
                } else if ("Solicitada".equalsIgnoreCase(t.getEstado())) {
                    pendientes.add(t);
                }
            }
        }

        if (tieneEnCurso) {
            enviarError(exchange, 400, "No puedes eliminar una oferta con intercambios en curso. Finaliza el proceso antes de retirar el servicio.");
            return;
        }

        // Cancel pending requests
        for (Transaccion t : pendientes) {
            t.actualizarEstado("Cancelada por Prestador");
            transaccionRepository.guardarTransaccionAtomica(t);
        }

        oferta.setEstado("Retirada");
        boolean guardadoExitoso = repository.actualizarOferta(oferta);
        if (!guardadoExitoso) {
            enviarError(exchange, 500, "Error al actualizar el estado de la oferta.");
            return;
        }

        JsonObject resp = new JsonObject();
        resp.addProperty("success", true);
        resp.addProperty("mensaje", "Publicacion eliminada con exito");
        enviarRespuesta(exchange, 200, resp.toString());
    }

    private boolean validarRangoValor(double horas) {
        return horas >= 0.5 && horas <= 20.0;
    }

    private synchronized String generarSiguienteId() {
        List<OfertaTalento> ofertas = repository.obtenerTodas();
        int maxId = 0;
        for (OfertaTalento o : ofertas) {
            String idStr = o.getId_oferta();
            if (idStr != null && idStr.startsWith("OFR-")) {
                try {
                    int num = Integer.parseInt(idStr.substring(4));
                    if (num > maxId) {
                        maxId = num;
                    }
                } catch (NumberFormatException e) {
                    // Ignorar
                }
            }
        }
        return String.format("OFR-%03d", maxId + 1);
    }
}