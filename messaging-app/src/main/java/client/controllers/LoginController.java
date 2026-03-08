package client.controllers;

import client.network.ServerConnection;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * Contrôleur de la vue de connexion.
 */
public class LoginController {

    @FXML private TextField     usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label         errorLabel;
    @FXML private Button        loginButton;

    private final ServerConnection connection = new ServerConnection();

    @FXML
    private void handleLogin() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            showError("Veuillez remplir tous les champs.");
            return;
        }

        loginButton.setDisable(true);
        errorLabel.setText("");

        new Thread(() -> {
            try {
                connection.connect();
                connection.send("LOGIN " + username + "||" + password);
                String response = connection.readLine();

                Platform.runLater(() -> {
                    loginButton.setDisable(false);
                    if (response != null && response.startsWith("OK")) {
                        openChat(username);
                    } else {
                        showError(response != null ? response.replace("ERROR: ", "") : "Erreur de connexion.");
                        connection.disconnect();
                    }
                });

            } catch (IOException e) {
                Platform.runLater(() -> {
                    loginButton.setDisable(false);
                    showError("Impossible de joindre le serveur. Vérifiez qu'il est démarré.");
                });
            }
        }).start();
    }

    @FXML
    private void goToRegister() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/register.fxml"));
        Parent root = loader.load();
        Stage stage = (Stage) usernameField.getScene().getWindow();
        stage.setScene(new Scene(root, 500, 600));
        stage.setTitle("Inscription — App Messagerie");
    }

    private void openChat(String username) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/chat.fxml"));
            Parent root = loader.load();
            ChatController chatCtrl = loader.getController();
            chatCtrl.initialize(username, connection);

            Stage stage = (Stage) usernameField.getScene().getWindow();
            stage.setScene(new Scene(root, 900, 600));
            stage.setTitle("App Messagerie — " + username);
        } catch (IOException e) {
            showError("Erreur ouverture du chat : " + e.getMessage());
        }
    }

    private void showError(String msg) {
        errorLabel.setText(msg);
    }
}
