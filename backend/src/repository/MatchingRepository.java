package repository;

import model.Miembro;
import util.JsonUtils;
import util.RutasDatos;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class MatchingRepository {
    private String jsonProfilesPath;
    private String jsonReputationPath;

    public MatchingRepository() {
        this.jsonProfilesPath = RutasDatos.archivo("usuarios.json");
        this.jsonReputationPath = RutasDatos.archivo("reputacion.json");
    }

    public MatchingRepository(String customProfilesPath, String customReputationPath) {
        this.jsonProfilesPath = customProfilesPath;
        this.jsonReputationPath = customReputationPath;
    }

    public List<Miembro> obtenerCandidatosComunidad() {
        try {
            File file = new File(jsonProfilesPath);
            if (!file.exists()) {
                return new ArrayList<>();
            }
            byte[] encoded = Files.readAllBytes(Paths.get(jsonProfilesPath));
            String content = new String(encoded, StandardCharsets.UTF_8);
            return JsonUtils.fromJsonList(content, Miembro.class);
        } catch (IOException e) {
            System.err.println("Error reading candidates file " + jsonProfilesPath + ": " + e.getMessage());
            return new ArrayList<>();
        }
    }

    public List<Miembro> filtrarPorReputacionMinima(double estrellasMinimas) {
        List<Miembro> candidatos = obtenerCandidatosComunidad();
        List<Miembro> filtrados = new ArrayList<>();
        for (Miembro m : candidatos) {
            if (m.getReputacion() >= estrellasMinimas) {
                filtrados.add(m);
            }
        }
        return filtrados;
    }

    public String getJsonProfilesPath() {
        return jsonProfilesPath;
    }

    public String getJsonReputationPath() {
        return jsonReputationPath;
    }
}
