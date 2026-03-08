package client.controllers;

import client.network.ServerConnection;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * Contrôleur de la vue d'inscription.
 * RG1 : username unique (erreur retournée par le serveur)
 * RG9 : mot de passe haché côté serveur
 */
public class RegisterController {

    @FXML private TextField     usernameField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmField;
    @FXML private Label         errorLabel;
    @FXML private Label         successLabel;
    @FXML private Button        registerButton;

    @FXML
    private void handleRegister() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();
        String confirm  = confirmField.getText();

        errorLabel.setText("");
        successLabel.setText("");

        if (username.isEmpty() || password.isEmpty()) {
            errorLabel.setText("Veuillez remplir tous les champs.");
            return;
        }
        if (!password.equals(confirm)) {
            errorLabel.setText("Les mots de passe ne correspondent pas.");
            return;
        }
        if (password.length() < 4) {
            errorLabel.setText("Le mot de passe doit faire au moins 4 caractères.");
            return;
        }

        registerButton.setDisable(true);

        new Thread(() -> {
            ServerConnection conn = new ServerConnection();
            try {
                conn.connect();
                conn.send("REGISTER " + username + "||" + password);
                String response = conn.readLine();
                conn.disconnect();

                Platform.runLater(() -> {
                    registerButton.setDisable(false);
                    if (response != null && response.startsWith("OK")) {
                        successLabel.setText("Compte créé ! Vous pouvez maintenant vous connecter.");
                        usernameField.clear();
                        passwordField.clear();
                        confirmField.clear();
                    } else {
                        errorLabel.setText(response != null ? response.replace("ERROR: ", "") : "Erreur serveur.");
                    }
                });

            } catch (IOException e) {
                Platform.runLater(() -> {
                    registerButton.setDisable(false);
                    errorLabel.setText("Impossible de joindre le serveur.");
                });
            }
        }).start();
    }

    @FXML
    private void goToLogin() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/login.fxml"));
        Parent root = loader.load();
        Stage stage = (Stage) usernameField.getScene().getWindow();
        stage.setScene(new Scene(root, 500, 500));
        stage.setTitle("Connexion — App Messagerie");
    }
}
