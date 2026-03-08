package client.controllers;

import client.network.MessageListener;
import client.network.ServerConnection;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Contrôleur de la fenêtre de chat principale.
 * Gère la liste des utilisateurs, les conversations et la réception des messages.
 * RG10 : affichage d'erreur et passage OFFLINE en cas de perte réseau
 */
public class ChatController implements MessageListener.MessageCallback {

    @FXML private Label      currentUserLabel;
    @FXML private Label      chatWithLabel;
    @FXML private ListView<String> usersList;
    @FXML private VBox       messagesBox;
    @FXML private ScrollPane scrollPane;
    @FXML private TextField  messageInput;
    @FXML private Button     sendButton;
    @FXML private Label      statusBar;

    private String           currentUser;
    private String           selectedUser;
    private ServerConnection connection;
    private MessageListener  listener;
    private final List<String> onlineUsers = new ArrayList<>();

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("HH:mm");

    /**
     * Appelé par LoginController après authentification réussie.
     */
    public void initialize(String username, ServerConnection connection) {
        this.currentUser = username;
        this.connection  = connection;
        currentUserLabel.setText("Connecté : " + username);
        statusBar.setVisible(false);

        // Démarrer le thread d'écoute
        listener = new MessageListener(connection, this);
        Thread t = new Thread(listener);
        t.setDaemon(true);
        t.start();

        // Demander la liste des utilisateurs
        connection.send("USERS");
    }

    // ─────────────────────────────────────────────────────────────────
    // Actions utilisateur
    // ─────────────────────────────────────────────────────────────────

    @FXML
    private void handleUserSelected() {
        String selected = usersList.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        // Extraire le nom (format "username [ONLINE]")
        selectedUser = selected.split(" ")[0];
        if (selectedUser.equals(currentUser)) { selectedUser = null; return; }

        chatWithLabel.setText("Conversation avec " + selectedUser);
        messagesBox.getChildren().clear();

        // Charger l'historique
        connection.send("HISTORY " + selectedUser);
    }

    @FXML
    private void handleSend() {
        if (selectedUser == null) {
            showStatus("Sélectionnez d'abord un utilisateur.", true);
            return;
        }
        String content = messageInput.getText().trim();
        if (content.isEmpty()) return;

        // RG7 : validation côté client
        if (content.length() > 1000) {
            showStatus("Message trop long (max 1000 caractères).", true);
            return;
        }

        connection.send("SEND " + selectedUser + "||" + content);
        addMessageBubble(currentUser, content, LocalDateTime.now().format(FMT), true);
        messageInput.clear();
        scrollToBottom();
    }

    @FXML
    private void handleLogout() {
        connection.send("LOGOUT");
        listener.stop();
        connection.disconnect();
        openLogin();
    }

    // ─────────────────────────────────────────────────────────────────
    // Réception des messages du serveur (depuis le thread listener)
    // ─────────────────────────────────────────────────────────────────

    @Override
    public void onMessage(String message) {
        Platform.runLater(() -> processServerMessage(message));
    }

    @Override
    public void onDisconnect() {
        // RG10 : perte de connexion
        Platform.runLater(() -> {
            showStatus("Connexion perdue avec le serveur.", true);
            sendButton.setDisable(true);
            messageInput.setDisable(true);
        });
    }

    private void processServerMessage(String msg) {
        if (msg.startsWith("MSG ")) {
            // MSG from||content||id
            String[] parts = msg.substring(4).split("\\|\\|", 3);
            if (parts.length >= 2) {
                String from    = parts[0];
                String content = parts[1];
                if (from.equals(selectedUser)) {
                    addMessageBubble(from, content, LocalDateTime.now().format(FMT), false);
                    scrollToBottom();
                } else {
                    // Notification discrète si message d'un autre utilisateur
                    showStatus("Nouveau message de " + from, false);
                }
            }

        } else if (msg.startsWith("USERS ")) {
            // USERS user1:ONLINE user2:ONLINE ...
            onlineUsers.clear();
            String[] parts = msg.substring(6).split(" ");
            for (String part : parts) {
                String[] kv = part.split(":");
                if (kv.length == 2 && !kv[0].equals(currentUser)) {
                    onlineUsers.add(kv[0] + " [" + kv[1] + "]");
                }
            }
            refreshUsersList();

        } else if (msg.startsWith("STATUS ")) {
            // STATUS username ONLINE|OFFLINE
            String[] parts = msg.split(" ");
            if (parts.length == 3) {
                String user   = parts[1];
                String status = parts[2];
                updateUserStatus(user, status);
            }

        } else if (msg.startsWith("HISTORY_START")) {
            messagesBox.getChildren().clear();

        } else if (msg.startsWith("HISTORY_MSG ")) {
            // HISTORY_MSG from||to||content||dateEnvoi||statut||id
            String[] parts = msg.substring(12).split("\\|\\|");
            if (parts.length >= 4) {
                String from    = parts[0];
                String content = parts[2];
                String date    = parts[3].length() > 16 ? parts[3].substring(11, 16) : parts[3];
                boolean mine   = from.equals(currentUser);
                addMessageBubble(from, content, date, mine);
            }

        } else if (msg.equals("HISTORY_END")) {
            scrollToBottom();

        } else if (msg.startsWith("ERROR: ")) {
            showStatus(msg.replace("ERROR: ", ""), true);

        } else if (msg.startsWith("INFO ")) {
            showStatus(msg.substring(5), false);
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // Utilitaires UI
    // ─────────────────────────────────────────────────────────────────

    /** Ajoute une bulle de message dans la zone de conversation. */
    private void addMessageBubble(String from, String content, String time, boolean mine) {
        HBox wrapper = new HBox();
        wrapper.setMaxWidth(Double.MAX_VALUE);

        VBox bubble = new VBox(2);
        bubble.setMaxWidth(480);

        if (!mine) {
            Label nameLbl = new Label(from);
            nameLbl.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #2E75B6;");
            bubble.getChildren().add(nameLbl);
        }

        Label textLbl = new Label(content);
        textLbl.setWrapText(true);
        textLbl.setMaxWidth(460);
        textLbl.setStyle(mine
                ? "-fx-background-color: #2E75B6; -fx-text-fill: white; -fx-padding: 8 12; -fx-background-radius: 12 12 2 12; -fx-font-size: 13px;"
                : "-fx-background-color: white; -fx-text-fill: #2C2C2C; -fx-padding: 8 12; -fx-background-radius: 12 12 12 2; -fx-font-size: 13px; -fx-border-color: #ddd; -fx-border-width: 1; -fx-border-radius: 12 12 12 2;");

        Label timeLbl = new Label(time);
        timeLbl.setStyle("-fx-font-size: 10px; -fx-text-fill: #888;");

        bubble.getChildren().addAll(textLbl, timeLbl);
        bubble.setPadding(new Insets(2, 6, 2, 6));

        if (mine) {
            wrapper.setAlignment(Pos.CENTER_RIGHT);
        } else {
            wrapper.setAlignment(Pos.CENTER_LEFT);
        }

        wrapper.getChildren().add(bubble);
        messagesBox.getChildren().add(wrapper);
    }

    /** Rafraîchit la liste des utilisateurs dans le ListView. */
    private void refreshUsersList() {
        usersList.getItems().setAll(onlineUsers);
    }

    /** Met à jour le statut d'un utilisateur dans la liste. */
    private void updateUserStatus(String username, String status) {
        onlineUsers.removeIf(u -> u.startsWith(username + " "));
        if (status.equals("ONLINE")) {
            onlineUsers.add(username + " [ONLINE]");
            showStatus(username + " est maintenant en ligne.", false);
        } else {
            showStatus(username + " est maintenant hors ligne.", false);
        }
        refreshUsersList();
    }

    /** Affiche un message dans la barre de statut. */
    private void showStatus(String msg, boolean isError) {
        statusBar.setText(msg);
        statusBar.setStyle(isError
                ? "-fx-padding: 4 10; -fx-font-size: 11px; -fx-text-fill: #C0392B; -fx-background-color: #FDECEA;"
                : "-fx-padding: 4 10; -fx-font-size: 11px; -fx-text-fill: #1E8449; -fx-background-color: #EAFAF1;");
        statusBar.setVisible(true);
    }

    private void scrollToBottom() {
        scrollPane.applyCss();
        scrollPane.layout();
        scrollPane.setVvalue(1.0);
    }

    private void openLogin() {
        try {
            var loader = new javafx.fxml.FXMLLoader(getClass().getResource("/views/login.fxml"));
            var root = loader.<javafx.scene.Parent>load();
            Stage stage = (Stage) messagesBox.getScene().getWindow();
            stage.setScene(new javafx.scene.Scene(root, 500, 500));
            stage.setTitle("Connexion — App Messagerie");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
