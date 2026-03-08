package client.network;

import java.io.IOException;

/**
 * Thread qui écoute en permanence les messages envoyés par le serveur.
 * RG10 : en cas de perte, notifie le listener via l'interface onDisconnect
 */
public class MessageListener implements Runnable {

    /** Callback appelé à chaque message reçu du serveur. */
    public interface MessageCallback {
        void onMessage(String message);
        void onDisconnect();    // RG10
    }

    private final ServerConnection connection;
    private final MessageCallback  callback;
    private volatile boolean       running = true;

    public MessageListener(ServerConnection connection, MessageCallback callback) {
        this.connection = connection;
        this.callback   = callback;
    }

    @Override
    public void run() {
        try {
            String line;
            while (running && (line = connection.readLine()) != null) {
                callback.onMessage(line);
            }
        } catch (IOException e) {
            if (running) {
                // RG10 : perte de connexion inattendue
                callback.onDisconnect();
            }
        }
    }

    public void stop() {
        running = false;
    }
}
