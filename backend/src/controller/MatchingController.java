package controller;

import com.sun.net.httpserver.HttpExchange;
import model.Miembro;
import model.SugerenciaMatch;
import repository.MatchingRepository;
import repository.MiembroRepository;
import repository.TransaccionRepository;

import java.util.ArrayList;
import java.util.List;

public class MatchingController extends BaseController {
    private MatchingRepository matchingRepo;
    private MiembroRepository miembroRepo;
    private TransaccionRepository transRepo;

    public MatchingController(MatchingRepository matchingRepo, MiembroRepository miembroRepo, TransaccionRepository transRepo) {
        this.matchingRepo = matchingRepo;
        this.miembroRepo = miembroRepo;
        this.transRepo = transRepo;
    }

    private static boolean simuladorFalla = false;

    @Override
    protected void procesarSolicitud(HttpExchange exchange) throws Exception {
        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();

        if (path.equals("/api/matching/simular-falla") || path.endsWith("/simular-falla")) {
            String query = exchange.getRequestURI().getQuery();
            if (query != null && query.contains("activar=true")) {
                simuladorFalla = true;
            } else if (query != null && query.contains("activar=false")) {
                simuladorFalla = false;
            }
            enviarRespuesta(exchange, 200, "{\"simuladorFalla\": " + simuladorFalla + "}");
            return;
        }

        if (simuladorFalla) {
            enviarRespuesta(exchange, 503, "{\"error\": \"Servicio de Matching no disponible (Simulado)\"}");
            return;
        }

        if ("GET".equalsIgnoreCase(method)) {
            if (path.equals("/api/matching/sugerencias") || path.endsWith("/sugerencias")) {
                generarSugerenciasIntercambio(exchange);
            } else if (path.equals("/api/matching/ping") || path.endsWith("/ping")) {
                enviarRespuesta(exchange, 200, "{\"status\": \"OK\"}");
            } else {
                enviarError(exchange, 404, "Ruta no encontrada.");
            }
        } else {
            enviarError(exchange, 405, "Método no permitido.");
        }
    }

    private void generarSugerenciasIntercambio(HttpExchange exchange) throws Exception {
        String query = exchange.getRequestURI().getQuery();
        if (query == null || !query.contains("idUsuario=")) {
            enviarError(exchange, 400, "Debe indicar idUsuario.");
            return;
        }

        String idUsuario = null;
        for (String param : query.split("&")) {
            if (param.startsWith("idUsuario=")) {
                idUsuario = param.substring("idUsuario=".length()).trim();
                break;
            }
        }

        if (idUsuario == null || idUsuario.isEmpty()) {
            enviarError(exchange, 400, "idUsuario no puede estar vacío.");
            return;
        }

        Miembro perfil = null;
        for (Miembro m : miembroRepo.obtenerTodos()) {
            if (m.getId().equals(idUsuario)) {
                perfil = m;
                break;
            }
        }

        if (perfil == null) {
            enviarError(exchange, 404, "Usuario no encontrado.");
            return;
        }

        List<SugerenciaMatch> lista = ejecutarAlgoritmoCruzado(perfil);

        double balance = 0.0;
        if (perfil.getBilletera() != null) {
            balance = perfil.getBilletera().getSaldo();
        }

        aplicarPriorizacionPorSaldoNegativo(lista, balance);

        lista.sort((s1, s2) -> Double.compare(s2.getPrioridadPuntaje(), s1.getPrioridadPuntaje()));

        String responseJson = util.JsonUtils.toJson(lista);
        enviarRespuesta(exchange, 200, responseJson);
    }

    private List<SugerenciaMatch> ejecutarAlgoritmoCruzado(Miembro perfil) {
        List<SugerenciaMatch> sugerencias = new ArrayList<>();
        List<Miembro> candidatos = matchingRepo.obtenerCandidatosComunidad();
        int counter = 1;

        for (Miembro v : candidatos) {
            if (v.getId().equals(perfil.getId())) continue;
            if (!"ACTIVO".equalsIgnoreCase(v.getEstado())) continue;

            String s1 = findMatchingSkill(perfil.getHabilidades(), v.getDemandas());
            String s2 = findMatchingSkill(v.getHabilidades(), perfil.getDemandas());

            if (s1 != null && s2 != null) {
                String idSug = String.format("SUG-%03d", counter++);
                String socioCifrado = service.SeguridadService.cifrarDatos(v.getId());
                double score = 15.0 + v.getReputacion();
                sugerencias.add(new SugerenciaMatch(idSug, socioCifrado, s1, s2, score));
            }
        }

        for (Miembro v1 : candidatos) {
            if (v1.getId().equals(perfil.getId())) continue;
            if (!"ACTIVO".equalsIgnoreCase(v1.getEstado())) continue;

            String s1 = findMatchingSkill(perfil.getHabilidades(), v1.getDemandas());
            if (s1 == null) continue;

            for (Miembro v2 : candidatos) {
                if (v2.getId().equals(perfil.getId()) || v2.getId().equals(v1.getId())) continue;
                if (!"ACTIVO".equalsIgnoreCase(v2.getEstado())) continue;

                String s2 = findMatchingSkill(v1.getHabilidades(), v2.getDemandas());
                if (s2 == null) continue;

                String s3 = findMatchingSkill(v2.getHabilidades(), perfil.getDemandas());
                if (s3 == null) continue;

                double score = 10.0 + (v1.getReputacion() + v2.getReputacion()) / 2.0;

                String idSug1 = String.format("SUG-%03d", counter++);
                String socioCifrado1 = service.SeguridadService.cifrarDatos(v1.getId());
                sugerencias.add(new SugerenciaMatch(idSug1, socioCifrado1, s1, s3, score));

                String idSug2 = String.format("SUG-%03d", counter++);
                String socioCifrado2 = service.SeguridadService.cifrarDatos(v2.getId());
                sugerencias.add(new SugerenciaMatch(idSug2, socioCifrado2, s1, s3, score));
            }
        }

        return sugerencias;
    }

    private void aplicarPriorizacionPorSaldoNegativo(List<SugerenciaMatch> lista, double balance) {
        if (balance < 0.0) {
            double boost = Math.abs(balance) * 5.0;
            for (SugerenciaMatch s : lista) {
                s.setPrioridadPuntaje(s.getPrioridadPuntaje() + boost);
            }
        }
    }

    private String findMatchingSkill(List<String> offers, List<String> demands) {
        if (offers == null || demands == null) return null;
        for (String offer : offers) {
            for (String demand : demands) {
                if (offer.trim().equalsIgnoreCase(demand.trim())) {
                    return offer.trim();
                }
            }
        }
        return null;
    }
}
