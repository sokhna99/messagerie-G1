package server;

import model.Message;
import model.MessageStatus;
import service.AuthService;
import service.MessageService;

import java.io.*;
import java.net.Socket;
import java.util.List;
import java.util.Map;

/**
 * Gère la communication avec un client connecté dans un thread dédié.
 * RG11 : chaque client est géré dans un thread séparé
 * RG12 : journalisation de toutes les actions
 *
 * Protocole texte (ligne par ligne) :
 *   Client → Serveur  :  COMMANDE arg1 arg2 ...
 *   Serveur → Client  :  OK ... | ERROR: message | MSG from content | ...
 */
public class    ClientHandler implements Runnable {

    // Délimiteur pour séparer les parties d'une commande contenant des espaces
    private static final String SEP = "||";

    private final Socket socket;
    private final Map<String, ClientHandler> activeClients; // utilisateurs connectés
    private final AuthService    authService    = new AuthService();
    private final MessageService messageService = new MessageService();

    private BufferedReader in;
    private PrintWriter    out;
    private String         username; // null si pas encore authentifié

    public ClientHandler(Socket socket, Map<String, ClientHandler> activeClients) {
        this.socket        = socket;
        this.activeClients = activeClients;
    }

    @Override
    public void run() { // RG11
        try {
            in  = new BufferedReader(new InputStreamReader(socket.getInputStream(),  "UTF-8"));
            out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true);

            String line;
            while ((line = in.readLine()) != null) {
                processCommand(line.trim());
            }
        } catch (IOException e) {
            // Connexion perdue
            ServerLogger.logErreur("ClientHandler", "Perte de connexion pour " + username + " : " + e.getMessage());
        } finally {
            handleDisconnect();
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // Traitement des commandes
    // ─────────────────────────────────────────────────────────────────

    private void processCommand(String line) {
        if (line.isEmpty()) return;

        String[] parts = line.split(" ", 2);
        String command = parts[0].toUpperCase();
        String args    = parts.length > 1 ? parts[1] : "";

        switch (command) {
            case "REGISTER" -> handleRegister(args);
            case "LOGIN"    -> handleLogin(args);
            case "SEND"     -> handleSend(args);
            case "HISTORY"  -> handleHistory(args);
            case "USERS"    -> handleUsers();
            case "LOGOUT"   -> handleLogout();
            default         -> send("ERROR: Commande inconnue : " + command);
        }
    }

    // ── REGISTER username||password ───────────────────────────────────
    private void handleRegister(String args) {
        String[] p = args.split("\\|\\|", 2);
        if (p.length < 2) { send("ERROR: Format : REGISTER username||password"); return; }
        String user = p[0].trim();
        String pass = p[1].trim();

        try {
            authService.register(user, pass);
            ServerLogger.logInscription(user); // RG12
            send("OK Inscription réussie pour " + user);
        } catch (Exception e) {
            send("ERROR: " + e.getMessage());
        }
    }

    // ── LOGIN username||password ──────────────────────────────────────
    private void handleLogin(String args) {
        String[] p = args.split("\\|\\|", 2);
        if (p.length < 2) { send("ERROR: Format : LOGIN username||password"); return; }
        String user = p[0].trim();
        String pass = p[1].trim();

        try {
            // RG3 : refuser si déjà dans activeClients
            if (activeClients.containsKey(user)) {
                send("ERROR: Cet utilisateur est déjà connecté.");
                return;
            }

            authService.login(user, pass); // RG2, RG4
            this.username = user;
            activeClients.put(username, this);

            ServerLogger.logConnexion(username); // RG12
            send("OK " + username);

            // RG6 : livraison des messages différés
            deliverPendingMessages();

            // Notifier les autres clients de la connexion
            broadcast("STATUS " + username + " ONLINE", username);

        } catch (Exception e) {
            send("ERROR: " + e.getMessage());
        }
    }

    // ── SEND receiver||content ────────────────────────────────────────
    private void handleSend(String args) {
        if (!isAuthenticated()) return; // RG2

        String[] p = args.split("\\|\\|", 2);
        if (p.length < 2) { send("ERROR: Format : SEND destinataire||message"); return; }
        String receiver = p[0].trim();
        String content  = p[1].trim();

        try {
            // RG7 : validation (dans MessageService)
            Message msg = messageService.createMessage(username, receiver, content);
            ServerLogger.logMessage(username, receiver, content); // RG12

            // RG5/RG6 : livraison temps réel ou différé
            ClientHandler recipientHandler = activeClients.get(receiver);
            if (recipientHandler != null) {
                recipientHandler.send("MSG " + username + "||" + content + "||" + msg.getId());
                messageService.marquerRecu(msg.getId());
                send("OK SENT " + msg.getId());
            } else {
                // RG6 : destinataire OFFLINE → message stocké, livraison différée
                send("OK STORED " + msg.getId());
            }

        } catch (Exception e) {
            send("ERROR: " + e.getMessage());
        }
    }

    // ── HISTORY otherUser ─────────────────────────────────────────────
    private void handleHistory(String args) {
        if (!isAuthenticated()) return; // RG2

        String otherUser = args.trim();
        if (otherUser.isEmpty()) { send("ERROR: Précisez l'autre utilisateur."); return; }

        try {
            List<Message> messages = messageService.getHistorique(username, otherUser); // RG8
            send("HISTORY_START " + messages.size());
            for (Message m : messages) {
                // Format : HISTORY_MSG from||to||content||dateEnvoi||statut||id
                send("HISTORY_MSG " + m.getSender().getUsername() + "||"
                        + m.getReceiver().getUsername() + "||"
                        + m.getContenu() + "||"
                        + m.getDateEnvoi() + "||"
                        + m.getStatut() + "||"
                        + m.getId());
                // Marquer comme LU si je suis le destinataire
                if (m.getReceiver().getUsername().equals(username) && m.getStatut() != MessageStatus.LU) {
                    messageService.marquerLu(m.getId());
                }
            }
            send("HISTORY_END");
        } catch (Exception e) {
            send("ERROR: " + e.getMessage());
        }
    }

    // ── USERS ─────────────────────────────────────────────────────────
    private void handleUsers() {
        if (!isAuthenticated()) return; // RG2

        StringBuilder sb = new StringBuilder("USERS");
        for (Map.Entry<String, ClientHandler> entry : activeClients.entrySet()) {
            sb.append(" ").append(entry.getKey()).append(":ONLINE");
        }
        send(sb.toString());
    }

    // ── LOGOUT ───────────────────────────────────────────────────────
    private void handleLogout() {
        send("BYE");
        handleDisconnect();
        try { socket.close(); } catch (IOException ignored) {}
    }

    // ─────────────────────────────────────────────────────────────────
    // Utilitaires
    // ─────────────────────────────────────────────────────────────────

    /** Livre les messages en attente dès la connexion. RG6 */
    private void deliverPendingMessages() {
        List<Message> pending = messageService.getMessagesDifferés(username);
        for (Message msg : pending) {
            send("MSG " + msg.getSender().getUsername() + "||" + msg.getContenu() + "||" + msg.getId());
            messageService.marquerRecu(msg.getId());
        }
        if (!pending.isEmpty()) {
            send("INFO " + pending.size() + " message(s) en attente livré(s).");
        }
    }

    /** Gère la déconnexion propre ou par perte réseau. RG4, RG12 */
    private void handleDisconnect() {
        if (username != null) {
            activeClients.remove(username);
            authService.logout(username); // RG4 : OFFLINE
            ServerLogger.logDeconnexion(username); // RG12
            broadcast("STATUS " + username + " OFFLINE", username);
            username = null;
        }
    }

    /** Envoie un message au client de ce handler. */
    public synchronized void send(String message) {
        if (out != null) out.println(message);
    }

    /** Diffuse un message à tous les clients connectés sauf l'expéditeur. */
    private void broadcast(String message, String except) {
        for (Map.Entry<String, ClientHandler> entry : activeClients.entrySet()) {
            if (!entry.getKey().equals(except)) {
                entry.getValue().send(message);
            }
        }
    }

    /** Vérifie que le client est authentifié. RG2 */
    private boolean isAuthenticated() {
        if (username == null) {
            send("ERROR: Vous devez être connecté pour effectuer cette action.");
            return false;
        }
        return true;
    }
}
