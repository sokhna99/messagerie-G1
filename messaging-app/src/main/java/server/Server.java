package server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Serveur TCP de messagerie.
 * Écoute sur le port 5000, accepte les connexions et crée un thread par client.
 * RG11 : thread par client via ClientHandler
 */
public class Server {

    private static final int PORT = 5000;

    // Map thread-safe des clients connectés : username → ClientHandler
    // ConcurrentHashMap garantit la sécurité en environnement multi-thread
    private final Map<String, ClientHandler> activeClients = new ConcurrentHashMap<>();

    public void start() {
        ServerLogger.log("Serveur démarré sur le port " + PORT);

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                Socket clientSocket = serverSocket.accept();
                ServerLogger.log("Nouvelle connexion entrante : " + clientSocket.getInetAddress());

                // RG11 : un thread par client
                ClientHandler handler = new ClientHandler(clientSocket, activeClients);
                Thread thread = new Thread(handler);
                thread.setDaemon(true);
                thread.start();
            }
        } catch (IOException e) {
            ServerLogger.logErreur("Server", "Erreur serveur : " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        new Server().start();
    }
}
