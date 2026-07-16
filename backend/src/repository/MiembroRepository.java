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

// Usuarios y billeteras en usuarios.json
public class MiembroRepository {

    private final String filePath;

    public MiembroRepository() {
        // Default persistence path
        this.filePath = RutasDatos.archivo("usuarios.json");
        inicializarArchivo();
    }

    public MiembroRepository(String customPath) {
        this.filePath = customPath;
        inicializarArchivo();
    }

    public String getFilePath() {
        return this.filePath;
    }

    private void inicializarArchivo() {
        try {
            File file = new File(filePath);
            // Create directories if they don't exist
            File parentDir = file.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }
            // Create file if it doesn't exist
            if (!file.exists()) {
                Files.write(Paths.get(filePath), "[]".getBytes(StandardCharsets.UTF_8));
            }
        } catch (IOException e) {
            System.err.println("Error initializing persistence file: " + e.getMessage());
        }
    }

    /**
     * Reads all members from the JSON file. Synchronized to prevent concurrent read issues.
     */
    public synchronized List<Miembro> obtenerTodos() {
        try {
            File file = new File(filePath);
            if (!file.exists()) {
                return new ArrayList<>();
            }
            byte[] encoded = Files.readAllBytes(Paths.get(filePath));
            String content = new String(encoded, StandardCharsets.UTF_8);
            return JsonUtils.fromJsonList(content, Miembro.class);
        } catch (IOException e) {
            System.err.println("Error reading file " + filePath + ": " + e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Saves a new member atomically.
     * Reads the current list, appends the new member, and writes back.
     */
    public synchronized boolean guardarRegistroAtomico(Miembro miembro) {
        try {
            List<Miembro> miembros = obtenerTodos();
            
            // Uniqueness check for email
            for (Miembro m : miembros) {
                if (m.getCorreo().equalsIgnoreCase(miembro.getCorreo())) {
                    System.err.println("Validation Error: Email already exists: " + miembro.getCorreo());
                    return false;
                }
            }
            
            miembros.add(miembro);
            String jsonContent = JsonUtils.toJson(miembros);
            Files.write(Paths.get(filePath), jsonContent.getBytes(StandardCharsets.UTF_8));
            return true;
        } catch (IOException e) {
            System.err.println("Error writing to file " + filePath + ": " + e.getMessage());
            return false;
        }
    }

    /**
     * Updates an existing member profile by replacing the old matching ID.
     */
    public synchronized boolean actualizarMiembro(Miembro miembroActualizado) {
        try {
            List<Miembro> miembros = obtenerTodos();
            boolean encontrado = false;
            
            for (int i = 0; i < miembros.size(); i++) {
                if (miembros.get(i).getId().equals(miembroActualizado.getId())) {
                    miembros.set(i, miembroActualizado);
                    encontrado = true;
                    break;
                }
            }
            
            if (!encontrado) {
                System.err.println("Error: Member with ID " + miembroActualizado.getId() + " not found to update.");
                return false;
            }
            
            String jsonContent = JsonUtils.toJson(miembros);
            Files.write(Paths.get(filePath), jsonContent.getBytes(StandardCharsets.UTF_8));
            return true;
        } catch (IOException e) {
            System.err.println("Error updating member in file " + filePath + ": " + e.getMessage());
            return false;
        }
    }

    /**
     * Helper method to look up a member by their email.
     */
    public synchronized Miembro encontrarPorCorreo(String correo) {
        if (correo == null) {
            return null;
        }
        List<Miembro> miembros = obtenerTodos();
        for (Miembro m : miembros) {
            if (m.getCorreo().equalsIgnoreCase(correo.trim())) {
                return m;
            }
        }
        return null;
    }

    /**
     * Elimina un miembro por su ID de la persistencia de datos.
     * 
     * @param idUsuario El identificador unico del usuario a eliminar.
     * @return true si se elimino con exito, false en caso contrario.
     */
    public synchronized boolean eliminarMiembro(String idUsuario) {
        try {
            List<Miembro> listaMiembros = obtenerTodos();
            boolean miembroEliminado = false;
            
            for (int i = 0; i < listaMiembros.size(); i++) {
                if (listaMiembros.get(i).getId().equals(idUsuario)) {
                    listaMiembros.remove(i);
                    miembroEliminado = true;
                    break;
                }
            }
            
            if (!miembroEliminado) {
                System.err.println("Error: Miembro con ID " + idUsuario + " no encontrado para eliminar.");
                return false;
            }
            
            String contenidoJson = JsonUtils.toJson(listaMiembros);
            Files.write(Paths.get(filePath), contenidoJson.getBytes(StandardCharsets.UTF_8));
            return true;
        } catch (IOException e) {
            System.err.println("Error al eliminar miembro en el archivo " + filePath + ": " + e.getMessage());
            return false;
        }
    }
}
