package server;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Journalisation côté serveur.
 * RG12 : connexions, déconnexions, envois de messages
 */
public class ServerLogger {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static PrintWriter fileWriter;

    static {
        try {
            fileWriter = new PrintWriter(new FileWriter("server.log", true), true);
        } catch (IOException e) {
            System.err.println("[LOGGER] Impossible d'ouvrir server.log : " + e.getMessage());
        }
    }

    public static void log(String message) {
        String line = "[" + LocalDateTime.now().format(FMT) + "] " + message;
        System.out.println(line);
        if (fileWriter != null) fileWriter.println(line);
    }

    public static void logConnexion(String username) {
        log("CONNEXION    | " + username + " est connecté.");
    }

    public static void logDeconnexion(String username) {
        log("DECONNEXION  | " + username + " est déconnecté.");
    }

    public static void logMessage(String from, String to, String contenu) {
        log("MESSAGE      | " + from + " → " + to + " : " + contenu);
    }

    public static void logInscription(String username) {
        log("INSCRIPTION  | Nouvel utilisateur : " + username);
    }

    public static void logErreur(String context, String detail) {
        log("ERREUR       | [" + context + "] " + detail);
    }
}
