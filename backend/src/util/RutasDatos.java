package util;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

// Encuentra la carpeta data/ aunque el servidor se lance desde otra ruta
public final class RutasDatos {

 private static Path directorioCache;

 private RutasDatos() {
}

 public static Path directorioDatos() {
 if (directorioCache != null) {
return directorioCache;
}

 List<Path> candidatos = new ArrayList<>();
candidatos.add(Paths.get("backend", "data"));
 candidatos.add(Paths.get("proyecto-equipo-1", "backend", "data"));
candidatos.add(Paths.get("data"));

 Path elegido = null;
 long ultimaModificacion = 0;

for (Path dir : candidatos) {
 if (!Files.isDirectory(dir)) {
continue;
 }
 Path ofertas = dir.resolve("ofertas.json");
 if (Files.exists(ofertas)) {
try {
long mod = Files.getLastModifiedTime(ofertas).toMillis();
if (mod >= ultimaModificacion) {
ultimaModificacion = mod;
elegido = dir.toAbsolutePath().normalize();
 }
} catch (Exception ignored) {
elegido = dir.toAbsolutePath().normalize();
 }
 } else if (elegido == null && Files.exists(dir.resolve("usuarios.json"))) {
elegido = dir.toAbsolutePath().normalize(); }
 }

if (elegido == null) {
elegido = Paths.get("backend", "data").toAbsolutePath().normalize();
 try {
 Files.createDirectories(elegido);
 } catch (Exception e) {
System.err.println("No se pudo crear " + elegido + ": " + e.getMessage());
}
}

directorioCache = elegido;
 System.out.println("[SIVC] Datos en: " + directorioCache);
return directorioCache;
}

public static String archivo(String nombreArchivo) {
return directorioDatos().resolve(nombreArchivo).toString();
}
}
