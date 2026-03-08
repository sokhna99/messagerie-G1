package client.network;

import java.io.*;
import java.net.Socket;

/**
 * Gère la connexion TCP vers le serveur côté client.
 * RG10 : détecte la perte de connexion et notifie le contrôleur
 */
public class ServerConnection {

    private static final String HOST = "localhost";
    private static final int    PORT = 5000;

    private Socket       socket;
    private BufferedReader in;
    private PrintWriter    out;
    private boolean        connected = false;

    /** Tente de se connecter au serveur. Lève IOException en cas d'échec. */
    public void connect() throws IOException {
        socket = new Socket(HOST, PORT);
        in  = new BufferedReader(new InputStreamReader(socket.getInputStream(),  "UTF-8"));
        out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true);
        connected = true;
    }

    /** Envoie une commande au serveur. */
    public synchronized void send(String command) {
        if (out != null) out.println(command);
    }

    /** Lit une ligne de réponse du serveur (bloquant). */
    public String readLine() throws IOException {
        return in.readLine();
    }

    /** Ferme proprement la connexion. */
    public void disconnect() {
        connected = false;
        try {
            if (socket != null) socket.close();
        } catch (IOException ignored) {}
    }

    public boolean isConnected() { return connected; }
}
