package repository;

import model.OfertaTalento;
import util.JsonUtils;
import util.RutasDatos;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// Persistencia de ofertas en ofertas.json
public class OfertaRepository {

    private final String filePath;

    public OfertaRepository() {
        this.filePath = RutasDatos.archivo("ofertas.json");
        System.out.println("📂 [OfertaRepository] Cargando ofertas desde la ruta absoluta: "+ new File(this.filePath).getAbsolutePath());
        inicializarArchivo();
    }

    public OfertaRepository(String customPath) {
        this.filePath = customPath;
        System.out.println("📂 [OfertaRepository] Cargando ofertas desde ruta customizada: " 
                           + new File(this.filePath).getAbsolutePath());
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
            System.err.println("Error al crear ofertas.json: " + e.getMessage());
        }
    }

    public synchronized List<OfertaTalento> obtenerTodas() {
        try {
            File file = new File(filePath);
            if (!file.exists()) {
                return new ArrayList<>();
            }
            byte[] encoded = Files.readAllBytes(Paths.get(filePath));
            String content = new String(encoded, StandardCharsets.UTF_8);
            List<OfertaTalento> lista = JsonUtils.fromJsonList(content, OfertaTalento.class);
            return deduplicarPorId(lista);
        } catch (IOException e) {
            System.err.println("Error leyendo " + filePath + ": " + e.getMessage());
            return new ArrayList<>();
        }
    }

    public synchronized boolean guardarOfertaAtomica(OfertaTalento oferta) {
        try {
            List<OfertaTalento> ofertas = obtenerTodas();
            boolean existe = false;
            for (int i = 0; i < ofertas.size(); i++) {
                if (ofertas.get(i).getId_oferta().equals(oferta.getId_oferta())) {
                    ofertas.set(i, oferta);
                    existe = true;
                    break;
                }
            }
            if (!existe) {
                ofertas.add(oferta);
            }
            return escribirArchivo(ofertas);
        } catch (IOException e) {
            System.err.println("Error guardando oferta: " + e.getMessage());
            return false;
        }
    }

    public synchronized boolean actualizarOferta(OfertaTalento oferta) {
        return guardarOfertaAtomica(oferta);
    }

    // ================== NUEVO MÉTODO ==================
    /**
     * Obtiene una oferta por su ID.
     * 
     * @param id Identificador de la oferta (ej. "OFR-001").
     * @return La oferta encontrada, o null si no existe.
     */
    public synchronized OfertaTalento obtenerPorId(String id) {
        List<OfertaTalento> ofertas = obtenerTodas();
        for (OfertaTalento o : ofertas) {
            if (o.getId_oferta().equals(id)) {
                return o;
            }
        }
        return null;
    }
    // ================== FIN NUEVO MÉTODO ==================

    private boolean escribirArchivo(List<OfertaTalento> ofertas) throws IOException {
        String jsonContent = JsonUtils.toJson(ofertas);
        Files.write(Paths.get(filePath), jsonContent.getBytes(StandardCharsets.UTF_8));
        return true;
    }

    private List<OfertaTalento> deduplicarPorId(List<OfertaTalento> lista) {
        Map<String, OfertaTalento> unicas = new LinkedHashMap<>();
        for (OfertaTalento o : lista) {
            if (o.getId_oferta() != null) {
                unicas.put(o.getId_oferta(), o);
            }
        }
        return new ArrayList<>(unicas.values());
    }

    /**
     * Elimina todas las ofertas creadas por un usuario de la persistencia de datos.
     * 
     * @param idCreador El identificador del miembro creador.
     * @return true si se realizo la operacion con exito, false en caso contrario.
     */
    public synchronized boolean eliminarOfertasPorCreador(String idCreador) {
        try {
            List<OfertaTalento> listaOfertas = obtenerTodas();
            List<OfertaTalento> ofertasFiltradas = new ArrayList<>();
            boolean huboCambios = false;
            
            for (OfertaTalento oferta : listaOfertas) {
                if (oferta.getId_creador().equals(idCreador)) {
                    huboCambios = true;
                } else {
                    ofertasFiltradas.add(oferta);
                }
            }
            
            if (huboCambios) {
                return escribirArchivo(ofertasFiltradas);
            }
            return true;
        } catch (IOException e) {
            System.err.println("Error al eliminar ofertas del creador " + idCreador + ": " + e.getMessage());
            return false;
        }
    }
}