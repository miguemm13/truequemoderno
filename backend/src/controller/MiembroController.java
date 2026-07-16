package controller;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpExchange;
import model.Billetera;
import model.Miembro;
import repository.MiembroRepository;
import repository.OfertaRepository;
import service.SeguridadService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

// Registro, login y listado de miembros
public class MiembroController extends BaseController {

    private final MiembroRepository repository;
    private final OfertaRepository ofertaRepository;
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@(.+)$");

    public MiembroController(MiembroRepository repository, OfertaRepository ofertaRepository) {
        this.repository = repository;
        this.ofertaRepository = ofertaRepository;
    }

    @Override
    protected void procesarSolicitud(HttpExchange exchange) throws Exception {
        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();

        if ("POST".equalsIgnoreCase(method)) {
            String body = leerBody(exchange);
            if (body == null || body.trim().isEmpty()) {
                enviarError(exchange, 400, "El cuerpo de la solicitud no puede estar vacio.");
                return;
            }

            switch (path) {
                case "/api/miembros/registro":
                    registrarMiembro(exchange, body);
                    break;
                case "/api/miembros/login":
                    loginMiembro(exchange, body);
                    break;
                case "/api/miembros/recuperar/pregunta":
                    obtenerPreguntaSeguridad(exchange, body);
                    break;
                case "/api/miembros/recuperar/verificar":
                    recuperarContrasena(exchange, body);
                    break;
                case "/api/miembros/actualizar-perfil":
                    actualizarPerfil(exchange, body);
                    break;
                case "/api/miembros/eliminar-perfil":
                    eliminarPerfil(exchange, body);
                    break;
                default:
                    enviarError(exchange, 404, "Ruta no encontrada.");
                    break;
            }
        } else if ("GET".equalsIgnoreCase(method)) {
            if ("/api/miembros/listar".equals(path)) {
                listarMiembros(exchange);
            } else if (rutaTerminaEn(exchange, "/saldo")) {
                obtenerSaldoUsuario(exchange);
            } else {
                enviarError(exchange, 404, "Ruta GET no encontrada.");
            }
        } else {
            enviarError(exchange, 405, "Metodo no permitido.");
        }
    }

    private void obtenerSaldoUsuario(HttpExchange exchange) throws IOException {
        String query = exchange.getRequestURI().getQuery();
        if (query == null || !query.contains("idUsuario=")) {
            enviarError(exchange, 400, "Debe indicar idUsuario.");
            return;
        }
        String idUsuario = query.split("idUsuario=")[1].split("&")[0].trim();
        Miembro miembro = null;
        for (Miembro m : repository.obtenerTodos()) {
            if (m.getId().equals(idUsuario)) {
                miembro = m;
                break;
            }
        }
        if (miembro == null) {
            retornarErrorValidacion(exchange, "Usuario no encontrado.");
            return;
        }
        JsonObject resp = new JsonObject();
        resp.addProperty("id", miembro.getId());
        resp.addProperty("saldo", miembro.getBilletera().getSaldo());
        resp.addProperty("reputacion", miembro.getReputacion());
        resp.addProperty("telefono", miembro.getTelefono());

        enviarRespuesta(exchange, 200, resp.toString());
    }

    private void listarMiembros(HttpExchange exchange) throws IOException {
        List<Miembro> miembros = repository.obtenerTodos();
        JsonArray array = new JsonArray();
        for (Miembro m : miembros) {
            JsonObject obj = new JsonObject();
            obj.addProperty("id", m.getId());
            String nombreVisible;
            try {
                nombreVisible = SeguridadService.descifrarDatos(m.getNombre());
                if (nombreVisible == null || nombreVisible.trim().isEmpty()) {
                    nombreVisible = "Miembro " + m.getId();
                }
            } catch (Exception e) {
                nombreVisible = "Miembro " + m.getId();
            }
            obj.addProperty("nombre", nombreVisible);
            obj.addProperty("telefono", m.getTelefono());
            array.add(obj);
        }
        enviarRespuesta(exchange, 200, array.toString());
    }

    
    private void registrarMiembro(HttpExchange exchange, String body) throws IOException {
        JsonObject json;
        try {
            json = JsonParser.parseString(body).getAsJsonObject();
        } catch (Exception e) {
            retornarErrorValidacion(exchange, "JSON invalido.");
            return;
        }

        String errorValidacion = validarIntegridad(json);
        if (errorValidacion != null) {
            retornarErrorValidacion(exchange, errorValidacion);
            return;
        }

        String nombre = json.get("nombre").getAsString().trim();
        String correo = json.get("correo").getAsString().trim().toLowerCase();
        String contrasena = json.get("contrasena").getAsString();
        String preguntaSeguridad = json.get("preguntaSeguridad").getAsString().trim();
        String respuestaSeguridad = json.get("respuestaSeguridad").getAsString().trim();
        String telefono = json.get("telefono") != null ? json.get("telefono").getAsString().trim() : null;


        List<String> habilidades = new ArrayList<>();
        if (json.get("habilidades") != null && json.get("habilidades").isJsonArray()) {
            JsonArray habArray = json.getAsJsonArray("habilidades");
            for (JsonElement el : habArray) {
                String h = el.getAsString().trim();
                if (!h.isEmpty()) {
                    habilidades.add(h);
                }
            }
        }

        List<String> demandas = new ArrayList<>();
        if (json.get("demandas") != null && json.get("demandas").isJsonArray()) {
            JsonArray demArray = json.getAsJsonArray("demandas");
            for (JsonElement el : demArray) {
                String d = el.getAsString().trim();
                if (!d.isEmpty()) {
                    demandas.add(d);
                }
            }
        }

        if (repository.encontrarPorCorreo(correo) != null) {
            retornarErrorValidacion(exchange, "El correo electronico ya esta registrado.");
            return;
        }

        String nextId = generarSiguienteId();

        String nombreCifrado = SeguridadService.cifrarDatos(nombre);
        String respuestaCifrada = SeguridadService.cifrarDatos(respuestaSeguridad);
        String contrasenaHashed = SeguridadService.hashSHA256(contrasena);

        Miembro nuevoMiembro = new Miembro(
                nextId,
                nombreCifrado,
                correo,
                contrasenaHashed,
                habilidades,
                "ACTIVO",
                preguntaSeguridad,
                respuestaCifrada);
        nuevoMiembro.setDemandas(demandas);
        nuevoMiembro.setTelefono(telefono);

        Billetera nuevaBilletera = new Billetera(0.00, -2.00);
        nuevoMiembro.asignarBilletera(nuevaBilletera);

        boolean guardadoExitoso = repository.guardarRegistroAtomico(nuevoMiembro);
        if (!guardadoExitoso) {
            enviarError(exchange, 500, "Error de escritura en persistencia. Reintente.");
            return;
        }

        JsonObject response = new JsonObject();
        response.addProperty("mensaje", "Registro exitoso.");
        response.addProperty("id", nuevoMiembro.getId());
        response.addProperty("nombre", nombre);
        response.addProperty("correo", nuevoMiembro.getCorreo());
        response.addProperty("saldo", nuevoMiembro.getBilletera().getSaldo());
        response.addProperty("limite", nuevoMiembro.getBilletera().getLimite());

        enviarRespuesta(exchange, 201, response.toString());
    }


    private void loginMiembro(HttpExchange exchange, String body) throws IOException {
        JsonObject json;
        try {
            json = JsonParser.parseString(body).getAsJsonObject();
        } catch (Exception e) {
            enviarError(exchange, 400, "JSON invalido.");
            return;
        }

        if (json.get("correo") == null || json.get("correo").getAsString().trim().isEmpty() ||
                json.get("contrasena") == null || json.get("contrasena").getAsString().isEmpty()) {
            enviarError(exchange, 400, "Debe ingresar correo y contrasena.");
            return;
        }

        String correo = json.get("correo").getAsString().trim().toLowerCase();
        String contrasena = json.get("contrasena").getAsString();

        Miembro miembro = repository.encontrarPorCorreo(correo);
        if (miembro == null) {
            enviarError(exchange, 401, "Credenciales incorrectas.");
            return;
        }

        String contrasenaHashedInput = SeguridadService.hashSHA256(contrasena);
        if (!miembro.getContrasena().equals(contrasenaHashedInput)) {
            enviarError(exchange, 401, "Credenciales incorrectas.");
            return;
        }

        String nombreDesencriptado = SeguridadService.descifrarDatos(miembro.getNombre());

        JsonObject response = new JsonObject();
        response.addProperty("mensaje", "Login exitoso.");
        response.addProperty("id", miembro.getId());
        response.addProperty("nombre", nombreDesencriptado);
        response.addProperty("correo", miembro.getCorreo());
        response.addProperty("habilidades", miembro.getHabilidades().toString());
        response.addProperty("demandas", miembro.getDemandas().toString());
        response.addProperty("estado", miembro.getEstado());
        response.addProperty("telefono", miembro.getTelefono());

        JsonObject walletJson = new JsonObject();
        walletJson.addProperty("saldo", miembro.getBilletera().getSaldo());
        walletJson.addProperty("limite", miembro.getBilletera().getLimite());
        response.add("billetera", walletJson);

        enviarRespuesta(exchange, 200, response.toString());
    }

   
    private void obtenerPreguntaSeguridad(HttpExchange exchange, String body) throws IOException {
        JsonObject json;
        try {
            json = JsonParser.parseString(body).getAsJsonObject();
        } catch (Exception e) {
            enviarError(exchange, 400, "JSON invalido.");
            return;
        }

        if (json.get("correo") == null || json.get("correo").getAsString().trim().isEmpty()) {
            enviarError(exchange, 400, "El correo es obligatorio.");
            return;
        }

        String correo = json.get("correo").getAsString().trim().toLowerCase();
        Miembro miembro = repository.encontrarPorCorreo(correo);
        if (miembro == null) {
            enviarError(exchange, 404, "El correo no coincide con ningun miembro registrado.");
            return;
        }

        JsonObject response = new JsonObject();
        response.addProperty("correo", miembro.getCorreo());
        response.addProperty("preguntaSeguridad", miembro.getPreguntaSeguridad());

        enviarRespuesta(exchange, 200, response.toString());
    }

    
    private void recuperarContrasena(HttpExchange exchange, String body) throws IOException {
        JsonObject json;
        try {
            json = JsonParser.parseString(body).getAsJsonObject();
        } catch (Exception e) {
            enviarError(exchange, 400, "JSON invalido.");
            return;
        }

        if (json.get("correo") == null || json.get("correo").getAsString().trim().isEmpty() ||
                json.get("respuestaSeguridad") == null || json.get("respuestaSeguridad").getAsString().trim().isEmpty()
                ||
                json.get("nuevaContrasena") == null || json.get("nuevaContrasena").getAsString().isEmpty()) {
            enviarError(exchange, 400,
                    "Todos los campos son obligatorios (correo, respuestaSeguridad, nuevaContrasena).");
            return;
        }

        String correo = json.get("correo").getAsString().trim().toLowerCase();
        String respuestaSeguridadInput = json.get("respuestaSeguridad").getAsString().trim();
        String nuevaContrasena = json.get("nuevaContrasena").getAsString();

        Miembro miembro = repository.encontrarPorCorreo(correo);
        if (miembro == null) {
            enviarError(exchange, 404, "Miembro no encontrado.");
            return;
        }

        String respuestaGuardada = SeguridadService.descifrarDatos(miembro.getRespuestaSeguridad());

        if (!respuestaGuardada.equalsIgnoreCase(respuestaSeguridadInput)) {
            retornarErrorValidacion(exchange, "La respuesta de seguridad es incorrecta.");
            return;
        }

        miembro.setContrasena(SeguridadService.hashSHA256(nuevaContrasena));

        boolean actualizadoExitoso = repository.actualizarMiembro(miembro);
        if (!actualizadoExitoso) {
            enviarError(exchange, 500, "Error guardando la nueva contrasena.");
            return;
        }

        JsonObject response = new JsonObject();
        response.addProperty("mensaje", "Contrasena restablecida con exito.");
        enviarRespuesta(exchange, 200, response.toString());
    }

   
    private String validarIntegridad(JsonObject json) {
        if (json.get("nombre") == null || json.get("nombre").getAsString().trim().isEmpty()) {
            return "El nombre completo es obligatorio.";
        }
        if (json.get("correo") == null || json.get("correo").getAsString().trim().isEmpty()) {
            return "El correo electronico es obligatorio.";
        }
        String correo = json.get("correo").getAsString().trim();
        if (!EMAIL_PATTERN.matcher(correo).matches()) {
            return "El correo electronico ingresado no tiene un formato valido.";
        }
        if (json.get("contrasena") == null || json.get("contrasena").getAsString().length() < 6) {
            return "La contrasena debe tener al menos 6 caracteres.";
        }
        if (json.get("preguntaSeguridad") == null || json.get("preguntaSeguridad").getAsString().trim().isEmpty()) {
            return "La pregunta de seguridad es obligatoria.";
        }
        if (json.get("respuestaSeguridad") == null || json.get("respuestaSeguridad").getAsString().trim().isEmpty()) {
            return "La respuesta de seguridad es obligatoria.";
        }
        return null;
    }

    
    private synchronized String generarSiguienteId() {
        List<Miembro> miembros = repository.obtenerTodos();
        int maxId = 0;
        for (Miembro m : miembros) {
            String idStr = m.getId(); // USR-???
            if (idStr != null && idStr.startsWith("USR-")) {
                try {
                    int num = Integer.parseInt(idStr.substring(4));
                    if (num > maxId) {
                        maxId = num;
                    }
                } catch (NumberFormatException e) {
                    // Ignore non-numeric IDs
                }
            }
        }
        return String.format("USR-%03d", maxId + 1);
    }

    /**
     * Actualiza la clave y/o la pregunta de seguridad de un miembro.
     * Toda la logica, comentarios y variables estan en espanol.
     */
    private void actualizarPerfil(HttpExchange exchange, String body) throws IOException {
        JsonObject jsonObjeto;
        try {
            jsonObjeto = JsonParser.parseString(body).getAsJsonObject();
        } catch (Exception e) {
            retornarErrorValidacion(exchange, "JSON invalido.");
            return;
        }

        if (jsonObjeto.get("idUsuario") == null || jsonObjeto.get("idUsuario").getAsString().trim().isEmpty()) {
            retornarErrorValidacion(exchange, "El idUsuario es obligatorio.");
            return;
        }

        String idUsuario = jsonObjeto.get("idUsuario").getAsString().trim();
        String nuevaClave = jsonObjeto.get("nuevaClave") != null ? jsonObjeto.get("nuevaClave").getAsString() : null;
        String preguntaSeguridad = jsonObjeto.get("preguntaSeguridad") != null ? jsonObjeto.get("preguntaSeguridad").getAsString().trim() : null;
        String respuestaSeguridad = jsonObjeto.get("respuestaSeguridad") != null ? jsonObjeto.get("respuestaSeguridad").getAsString().trim() : null;

        // Buscar al miembro por ID
        Miembro miembroEncontrado = null;
        for (Miembro miembroTemp : repository.obtenerTodos()) {
            if (miembroTemp.getId().equals(idUsuario)) {
                miembroEncontrado = miembroTemp;
                break;
            }
        }

        if (miembroEncontrado == null) {
            enviarError(exchange, 404, "Miembro no encontrado.");
            return;
        }

        // Si se provee una nueva clave
        if (nuevaClave != null && !nuevaClave.isEmpty()) {
            if (nuevaClave.length() < 6) {
                retornarErrorValidacion(exchange, "La contrasena debe tener al menos 6 caracteres.");
                return;
            }
            String claveHasheada = SeguridadService.hashSHA256(nuevaClave);
            miembroEncontrado.setContrasena(claveHasheada);
        }

        // Si se provee pregunta y respuesta de seguridad
        if (preguntaSeguridad != null && !preguntaSeguridad.isEmpty()) {
            if (respuestaSeguridad == null || respuestaSeguridad.isEmpty()) {
                retornarErrorValidacion(exchange, "Si cambia la pregunta de seguridad, la respuesta es obligatoria.");
                return;
            }
            String respuestaCifrada = SeguridadService.cifrarDatos(respuestaSeguridad);
            miembroEncontrado.setPreguntaSeguridad(preguntaSeguridad);
            miembroEncontrado.setRespuestaSeguridad(respuestaCifrada);
        }

        boolean exitoActualizacion = repository.actualizarMiembro(miembroEncontrado);
        if (!exitoActualizacion) {
            enviarError(exchange, 500, "Error al actualizar los datos del miembro en la persistencia.");
            return;
        }

        JsonObject respuestaJson = new JsonObject();
        respuestaJson.addProperty("mensaje", "Perfil actualizado con exito.");
        enviarRespuesta(exchange, 200, respuestaJson.toString());
    }

    /**
     * Elimina el perfil del miembro, sus ofertas del muro y su billetera.
     * Requiere que envie la confirmacion 'ELIMINAR' en mayusculas.
     */
    private void eliminarPerfil(HttpExchange exchange, String body) throws IOException {
        JsonObject jsonObjeto;
        try {
            jsonObjeto = JsonParser.parseString(body).getAsJsonObject();
        } catch (Exception e) {
            retornarErrorValidacion(exchange, "JSON invalido.");
            return;
        }

        if (jsonObjeto.get("idUsuario") == null || jsonObjeto.get("idUsuario").getAsString().trim().isEmpty() ||
            jsonObjeto.get("confirmacion") == null || jsonObjeto.get("confirmacion").getAsString().trim().isEmpty()) {
            retornarErrorValidacion(exchange, "El idUsuario y la confirmacion son obligatorios.");
            return;
        }

        String idUsuario = jsonObjeto.get("idUsuario").getAsString().trim();
        String confirmacion = jsonObjeto.get("confirmacion").getAsString().trim();

        if (!"ELIMINAR".equals(confirmacion)) {
            retornarErrorValidacion(exchange, "Debe ingresar exactamente la palabra ELIMINAR en mayusculas.");
            return;
        }

        // Verificar si el miembro existe
        Miembro miembroAELiminar = null;
        for (Miembro miembroTemp : repository.obtenerTodos()) {
            if (miembroTemp.getId().equals(idUsuario)) {
                miembroAELiminar = miembroTemp;
                break;
            }
        }

        if (miembroAELiminar == null) {
            enviarError(exchange, 404, "Miembro no encontrado.");
            return;
        }

        // Proceder a eliminar de la base de datos (miembros)
        boolean miembroEliminado = repository.eliminarMiembro(idUsuario);
        if (!miembroEliminado) {
            enviarError(exchange, 500, "Error al eliminar el miembro de la persistencia.");
            return;
        }

        // Eliminar también las publicaciones (ofertas) del creador en cascada
        boolean ofertasEliminadas = ofertaRepository.eliminarOfertasPorCreador(idUsuario);
        if (!ofertasEliminadas) {
            System.err.println("Advertencia: No se pudieron eliminar todas las ofertas del usuario " + idUsuario);
        }

        JsonObject respuestaJson = new JsonObject();
        respuestaJson.addProperty("mensaje", "Perfil y publicaciones eliminados correctamente.");
        enviarRespuesta(exchange, 200, respuestaJson.toString());
    }
}
