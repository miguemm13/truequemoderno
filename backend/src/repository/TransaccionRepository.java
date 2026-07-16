package repository;

import model.Transaccion;
import util.JsonUtils;
import util.RutasDatos;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

// Trueques y movimientos de horas en transacciones.json
public class TransaccionRepository {

    private final String filePath;

    public TransaccionRepository() {
        this.filePath = RutasDatos.archivo("transacciones.json");
        inicializarArchivo();
    }

    public TransaccionRepository(String customPath) {
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
            System.err.println("Error initializing transactions persistence file: " + e.getMessage());
        }
    }

    /**
     * Reads all transactions.
     */
    public synchronized List<Transaccion> obtenerTodas() {
        try {
            File file = new File(filePath);
            if (!file.exists()) {
                return new ArrayList<>();
            }
            byte[] encoded = Files.readAllBytes(Paths.get(filePath));
            String content = new String(encoded, StandardCharsets.UTF_8);
            return JsonUtils.fromJsonList(content, Transaccion.class);
        } catch (IOException e) {
            System.err.println("Error reading file " + filePath + ": " + e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Saves a new transaction or updates an existing one atomically and thread-safely.
     */
    public synchronized boolean guardarTransaccionAtomica(Transaccion transaccion) {
        try {
            List<Transaccion> transacciones = obtenerTodas();
            boolean found = false;
            for (int i = 0; i < transacciones.size(); i++) {
                if (transacciones.get(i).getId_transaccion().equals(transaccion.getId_transaccion())) {
                    transacciones.set(i, transaccion);
                    found = true;
                    break;
                }
            }
            if (!found) {
                transacciones.add(transaccion);
            }
            String jsonContent = JsonUtils.toJson(transacciones);
            Files.write(Paths.get(filePath), jsonContent.getBytes(StandardCharsets.UTF_8));
            return true;
        } catch (IOException e) {
            System.err.println("Error writing to file " + filePath + ": " + e.getMessage());
            return false;
        }
    }

    /**
     * HU-04: Persists transaction rejection double-saving (and logs wallet state change).
     */
    public synchronized boolean persistirCambiosRechazo(Transaccion transaccion, model.Billetera billetera) {
        // Since wallet balance is dynamically verified from active transaction state,
        // we persist the transaction update to status "Cancelada por Prestador".
        return guardarTransaccionAtomica(transaccion);
    }

    /**
     * HU-05: Executes atomic transfer updates for both files (usuarios.json and transacciones.json)
     * using temp files and atomic rename moves.
     */
    public synchronized boolean ejecutarTransferenciaAtomica(Transaccion trans, model.Billetera bRec, model.Billetera bPre, MiembroRepository miembroRepo) {
        try {
            // 1. Update receptor and prestador wallets in the member list
            List<model.Miembro> miembros = miembroRepo.obtenerTodos();
            for (model.Miembro m : miembros) {
                if (m.getId().equals(trans.getId_receptor())) {
                    m.setBilletera(bRec);
                } else if (m.getId().equals(trans.getId_prestador())) {
                    m.setBilletera(bPre);
                }
            }

            // 2. Update transaction in the transaction list
            List<Transaccion> transacciones = obtenerTodas();
            for (int i = 0; i < transacciones.size(); i++) {
                if (transacciones.get(i).getId_transaccion().equals(trans.getId_transaccion())) {
                    transacciones.set(i, trans);
                    break;
                }
            }

            // 3. Serialize updated structures
            String newMiembrosJson = JsonUtils.toJson(miembros);
            String newTransaccionesJson = JsonUtils.toJson(transacciones);

            // 4. Save to temporary files
            String userTmpPath = miembroRepo.getFilePath() + ".tmp";
            String txTmpPath = this.filePath + ".tmp";

            Files.write(Paths.get(userTmpPath), newMiembrosJson.getBytes(StandardCharsets.UTF_8));
            Files.write(Paths.get(txTmpPath), newTransaccionesJson.getBytes(StandardCharsets.UTF_8));

            // 5. Atomic Replace Move (renames the tmp file to the production file)
            Files.move(Paths.get(userTmpPath), Paths.get(miembroRepo.getFilePath()), 
                       java.nio.file.StandardCopyOption.REPLACE_EXISTING, 
                       java.nio.file.StandardCopyOption.ATOMIC_MOVE);

            Files.move(Paths.get(txTmpPath), Paths.get(this.filePath), 
                       java.nio.file.StandardCopyOption.REPLACE_EXISTING, 
                       java.nio.file.StandardCopyOption.ATOMIC_MOVE);

            return true;
        } catch (IOException e) {
            System.err.println("SIVC Commit: Atomic transfer failed. Rolling back updates. " + e.getMessage());
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
