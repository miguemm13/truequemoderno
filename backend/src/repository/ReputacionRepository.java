package repository;

import model.Calificacion;
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

// Calificaciones y promedio de reputacion en reputacion.json
public class ReputacionRepository {

    private final String filePath;

    public ReputacionRepository() {
        this.filePath = RutasDatos.archivo("reputacion.json");
        inicializarArchivo();
    }

    public ReputacionRepository(String customPath) {
        this.filePath = customPath;
        inicializarArchivo();
    }

    private void inicializarArchivo() {
        try {
            File file = new File(filePath);
            File parentDir = file.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }
            if (!file.exists()) {
                Files.write(Paths.get(filePath), "[]".getBytes(StandardCharsets.UTF_8));
            }
        } catch (IOException e) {
            System.err.println("Error initializing reputacion persistence file: " + e.getMessage());
        }
    }

    /**
     * Reads all ratings from the JSON file.
     */
    public synchronized List<Calificacion> obtenerTodas() {
        try {
            File file = new File(filePath);
            if (!file.exists()) {
                return new ArrayList<>();
            }
            byte[] encoded = Files.readAllBytes(Paths.get(filePath));
            String content = new String(encoded, StandardCharsets.UTF_8);
            return JsonUtils.fromJsonList(content, Calificacion.class);
        } catch (IOException e) {
            System.err.println("Error reading file " + filePath + ": " + e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * HU-06: Persists the rating and the updated member profile reputation atomically
     * using temp files and atomic rename moves.
     */
    public synchronized boolean persistirReputacionAtomica(Calificacion calif, Miembro perfil, MiembroRepository miembroRepo) {
        try {
            // 1. Update member list with the new reputation/reviews count
            List<Miembro> miembros = miembroRepo.obtenerTodos();
            for (int i = 0; i < miembros.size(); i++) {
                if (miembros.get(i).getId().equals(perfil.getId())) {
                    miembros.set(i, perfil);
                    break;
                }
            }

            // 2. Append new rating to rating list
            List<Calificacion> calificaciones = obtenerTodas();
            calificaciones.add(calif);

            // 3. Serialize updated structures
            String newMiembrosJson = JsonUtils.toJson(miembros);
            String newCalificacionesJson = JsonUtils.toJson(calificaciones);

            // 4. Save to temporary files
            String userTmpPath = miembroRepo.getFilePath() + ".tmp";
            String repTmpPath = this.filePath + ".tmp";

            Files.write(Paths.get(userTmpPath), newMiembrosJson.getBytes(StandardCharsets.UTF_8));
            Files.write(Paths.get(repTmpPath), newCalificacionesJson.getBytes(StandardCharsets.UTF_8));

            // 5. Atomic Replace Move (renames the tmp file to the production file)
            Files.move(Paths.get(userTmpPath), Paths.get(miembroRepo.getFilePath()), 
                       java.nio.file.StandardCopyOption.REPLACE_EXISTING, 
                       java.nio.file.StandardCopyOption.ATOMIC_MOVE);

            Files.move(Paths.get(repTmpPath), Paths.get(this.filePath), 
                       java.nio.file.StandardCopyOption.REPLACE_EXISTING, 
                       java.nio.file.StandardCopyOption.ATOMIC_MOVE);

            return true;
        } catch (IOException e) {
            System.err.println("SIVC Reputacion Commit: Atomic save failed. Rolling back updates. " + e.getMessage());
            // Cleanup temp files if they exist
            try {
                Files.deleteIfExists(Paths.get(miembroRepo.getFilePath() + ".tmp"));
                Files.deleteIfExists(Paths.get(this.filePath + ".tmp"));
            } catch (IOException rollbackEx) {
                // Ignore rollback cleanup exceptions
            }
            return false;
        }
    }
}
