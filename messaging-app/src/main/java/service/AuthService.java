package service;

import at.favre.lib.crypto.bcrypt.BCrypt;
import dao.UserDAO;
import model.User;
import model.UserStatus;

/**
 * Service d'authentification.
 * RG1  : username unique
 * RG2  : utilisateur doit être authentifié
 * RG3  : un seul login simultané
 * RG4  : gestion du statut ONLINE/OFFLINE
 * RG9  : mot de passe haché BCrypt
 */
public class AuthService {

    private final UserDAO userDAO = new UserDAO();

    /**
     * Inscrit un nouvel utilisateur.
     * RG1 : lève une exception si le username est déjà pris.
     * RG9 : hache le mot de passe avant persistance.
     */
    public User register(String username, String rawPassword) {
        if (username == null || username.isBlank() || username.length() > 50)
            throw new IllegalArgumentException("Username invalide (max 50 caractères, non vide).");

        if (rawPassword == null || rawPassword.length() < 4)
            throw new IllegalArgumentException("Mot de passe trop court (min 4 caractères).");

        if (userDAO.findByUsername(username) != null)
            throw new IllegalArgumentException("Ce username est déjà utilisé."); // RG1

        String hashedPassword = BCrypt.withDefaults().hashToString(12, rawPassword.toCharArray()); // RG9
        User user = new User(username, hashedPassword);
        return userDAO.save(user);
    }

    /**
     * Connecte un utilisateur.
     * RG2  : vérifie les credentials.
     * RG3  : refuse si déjà ONLINE.
     * RG4  : passe le statut à ONLINE.
     */
    public User login(String username, String rawPassword) {
        User user = userDAO.findByUsername(username);

        if (user == null)
            throw new SecurityException("Identifiants incorrects."); // RG2

        BCrypt.Result result = BCrypt.verifyer().verify(rawPassword.toCharArray(), user.getPassword());
        if (!result.verified)
            throw new SecurityException("Identifiants incorrects."); // RG2

        if (user.getStatus() == UserStatus.ONLINE)
            throw new IllegalStateException("Cet utilisateur est déjà connecté."); // RG3

        userDAO.updateStatus(username, UserStatus.ONLINE); // RG4
        user.setStatus(UserStatus.ONLINE);
        return user;
    }

    /**
     * Déconnecte un utilisateur.
     * RG4 : passe le statut à OFFLINE.
     */
    public void logout(String username) {
        if (username != null && !username.isBlank()) {
            userDAO.updateStatus(username, UserStatus.OFFLINE); // RG4
        }
    }
}
