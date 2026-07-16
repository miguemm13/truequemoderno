package service;

import java.util.HashMap;
import java.util.Map;

// Categorias validas de ofertas (CAT-001 ... CAT-012)
public class CatalogoService {

    private static final Map<String, String> categorias = new HashMap<>();

    static {
        categorias.put("CAT-001", "Hogar y Bricolaje");
        categorias.put("CAT-002", "Clases y Tutorias");
        categorias.put("CAT-003", "Tecnologia y Soporte");
        categorias.put("CAT-004", "Idiomas y Traduccion");
        categorias.put("CAT-005", "Salud y Cuidado");
        categorias.put("CAT-006", "Cocina y Gastronomia");
        categorias.put("CAT-007", "Programacion y Software");
        categorias.put("CAT-008", "Diseno y Creatividad");
        categorias.put("CAT-009", "Musica y Arte");
        categorias.put("CAT-010", "Deportes y Bienestar");
        categorias.put("CAT-011", "Mascotas y Jardineria");
        categorias.put("CAT-012", "Otros");
    }

    public static boolean validarCategoria(String idCategoria) {
        if (idCategoria == null) {
            return false;
        }
        return categorias.containsKey(idCategoria.toUpperCase().trim());
    }

    public static String obtenerNombreCategoria(String idCategoria) {
        if (idCategoria == null) {
            return null;
        }
        return categorias.get(idCategoria.toUpperCase().trim());
    }

    public static Map<String, String> obtenerCategorias() {
        return new HashMap<>(categorias);
    }
}
