package service;

import dao.MessageDAO;
import dao.UserDAO;
import model.Message;
import model.MessageStatus;
import model.User;

import java.util.List;

/**
 * Service de messagerie.
 * RG5  : expéditeur connecté, destinataire existant
 * RG6  : livraison différée si destinataire OFFLINE
 * RG7  : contenu non vide, max 1000 chars
 * RG8  : historique trié par dateEnvoi ASC
 */
public class MessageService {

    private final MessageDAO messageDAO = new MessageDAO();
    private final UserDAO    userDAO    = new UserDAO();

    /**
     * Crée et persiste un message.
     * Retourne le Message sauvegardé (avec ID).
     * RG5 : vérifie que le destinataire existe.
     * RG7 : valide le contenu.
     */
    public Message createMessage(String senderUsername, String receiverUsername, String contenu) {
        // RG7 : validation du contenu
        if (contenu == null || contenu.isBlank())
            throw new IllegalArgumentException("Le message ne peut pas être vide."); // RG7
        if (contenu.length() > 1000)
            throw new IllegalArgumentException("Le message ne doit pas dépasser 1000 caractères."); // RG7

        // RG5 : vérifier que le destinataire existe
        User receiver = userDAO.findByUsername(receiverUsername);
        if (receiver == null)
            throw new IllegalArgumentException("L'utilisateur destinataire n'existe pas."); // RG5

        User sender = userDAO.findByUsername(senderUsername);
        if (sender == null)
            throw new IllegalArgumentException("Expéditeur introuvable."); // RG5

        Message message = new Message(sender, receiver, contenu);
        return messageDAO.save(message);
    }

    /**
     * Marque un message comme RECU.
     */
    public void marquerRecu(Long messageId) {
        messageDAO.updateStatut(messageId, MessageStatus.RECU);
    }

    /**
     * Marque un message comme LU.
     */
    public void marquerLu(Long messageId) {
        messageDAO.updateStatut(messageId, MessageStatus.LU);
    }

    /**
     * Récupère l'historique d'une conversation, trié chronologiquement.
     * RG8 : ordre ASC par dateEnvoi
     */
    public List<Message> getHistorique(String username1, String username2) {
        return messageDAO.findConversation(username1, username2); // RG8
    }

    /**
     * Récupère les messages en attente pour un utilisateur qui vient de se connecter.
     * RG6 : livraison différée
     */
    public List<Message> getMessagesDifferés(String username) {
        return messageDAO.findPendingMessages(username); // RG6
    }
}
