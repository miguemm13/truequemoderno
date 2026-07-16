package util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

// Serializa y deserializa JSON con Gson
public class JsonUtils {

    private static final Gson gson = new GsonBuilder()
            .setPrettyPrinting()
            .serializeNulls()
            .create();

    public static String toJson(Object obj) {
        return gson.toJson(obj);
    }

    public static <T> T fromJson(String json, Class<T> clazz) {
        if (json == null || json.trim().isEmpty()) {
            return null;
        }
        return gson.fromJson(json, clazz);
    }

    @SuppressWarnings("unchecked")
    public static <T> List<T> fromJsonList(String json, Class<T> clazz) {
        if (json == null || json.trim().isEmpty() || json.trim().equals("[]")) {
            return new ArrayList<>();
        }
        try {
            Object array = gson.fromJson(json, Array.newInstance(clazz, 0).getClass());
            return new ArrayList<>(Arrays.asList((T[]) array));
        } catch (Exception e) {
            System.err.println("Error parsing JSON list: " + e.getMessage());
            return new ArrayList<>();
        }
    }
}
