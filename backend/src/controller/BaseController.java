package controller;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

// Base de todos los controladores HTTP: CORS, JSON y errores
public abstract class BaseController implements HttpHandler {

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        agregarCorsHeaders(exchange);
        System.out.println("\n📡 [BaseController] " + exchange.getRequestMethod() + " -> " + exchange.getRequestURI().getPath());
        
        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }

        try {
            procesarSolicitud(exchange);
        } catch (Exception e) {
            System.err.println("Error interno: " + e.getMessage());
            e.printStackTrace();
            enviarRespuesta(exchange, 500, "{\"error\": \"Error interno del servidor: " + e.getMessage() + "\"}");
        }
    }

    protected abstract void procesarSolicitud(HttpExchange exchange) throws Exception;

    protected String leerBody(HttpExchange exchange) throws IOException {
        try (InputStream is = exchange.getRequestBody();
             ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[1024];
            int len;
            while ((len = is.read(buffer)) != -1) {
                bos.write(buffer, 0, len);
            }
            return bos.toString("UTF-8");
        }
    }

    protected void enviarRespuesta(HttpExchange exchange, int statusCode, String responseJson) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        byte[] responseBytes = responseJson.getBytes("UTF-8");
        exchange.sendResponseHeaders(statusCode, responseBytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(responseBytes);
        }
    }

    protected void enviarError(HttpExchange exchange, int statusCode, String message) throws IOException {
        enviarRespuesta(exchange, statusCode, "{\"error\": \"" + message + "\"}");
    }

    protected void retornarErrorValidacion(HttpExchange exchange, String message) throws IOException {
        enviarRespuesta(exchange, 400, "{\"validationError\": \"" + message + "\"}");
    }

    protected boolean rutaTerminaEn(HttpExchange exchange, String sufijo) {
        String path = exchange.getRequestURI().getPath();
        return path.equals(sufijo) || path.endsWith(sufijo);
    }

    private void agregarCorsHeaders(HttpExchange exchange) {
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Authorization");
    }
}
