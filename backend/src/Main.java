import com.sun.net.httpserver.HttpServer;
import controller.MiembroController;
import controller.OfertaController;
import controller.TransaccionController;
import controller.ReputacionController;
import controller.MatchingController;
import repository.MiembroRepository;
import repository.OfertaRepository;
import repository.TransaccionRepository;
import repository.ReputacionRepository;
import repository.MatchingRepository;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

// Punto de entrada: levanta el API REST en el puerto 8080
public class Main {

    private static final int PORT = 8080;

   public static void main(String[] args) {
    try {
        System.out.println("Iniciando Servidor SIVC Backend...");

        MiembroRepository miembroRepository = new MiembroRepository();
        OfertaRepository ofertaRepository = new OfertaRepository();
        TransaccionRepository transaccionRepository = new TransaccionRepository();
        ReputacionRepository reputacionRepository = new ReputacionRepository();
        MatchingRepository matchingRepository = new MatchingRepository();

        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);

        // CONFIGURACIÓN DE CONTEXTOS
        server.createContext("/api/miembros", new MiembroController(miembroRepository, ofertaRepository));
        
        // 🔴 NOTA: Añadimos "/" al final para que capture rutas como "/actualizar" o "/OFR-009"
        server.createContext("/api/ofertas/", new OfertaController(ofertaRepository, miembroRepository, transaccionRepository)); 
        
        server.createContext("/api/transacciones",
                new TransaccionController(transaccionRepository, miembroRepository, ofertaRepository));
        server.createContext("/api/reputacion",
                new ReputacionController(reputacionRepository, miembroRepository, transaccionRepository));
        server.createContext("/api/matching",
                new MatchingController(matchingRepository, miembroRepository, transaccionRepository));//sedsdsd
        
        // ❌ SE ELIMINÓ EL DUPLICADO QUE ESTABA AQUÍ
           
        server.setExecutor(Executors.newCachedThreadPool());
        server.start();

        System.out.println("=================================================");
        System.out.println("  SIVC Backend iniciado exitosamente.");
        System.out.println("  Escuchando en: http://localhost:" + PORT + "/");
        System.out.println("=================================================");

    } catch (IOException e) {
        System.err.println("Error al iniciar el servidor en el puerto " + PORT + ": " + e.getMessage());
        e.printStackTrace();
        System.exit(1);
    }
  }
}