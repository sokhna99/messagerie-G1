package dao;

import model.Message;
import model.MessageStatus;
import org.hibernate.Session;
import org.hibernate.Transaction;
import util.HibernateUtil;

import java.util.List;

/**
 * DAO pour l'entité Message.
 */
public class MessageDAO {

    /** Persiste un nouveau message. */
    public Message save(Message message) {
        Transaction tx = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();
            session.persist(message);
            tx.commit();
            return message;
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            throw new RuntimeException("Erreur save Message : " + e.getMessage(), e);
        }
    }

    /** Met à jour le statut d'un message. */
    public void updateStatut(Long messageId, MessageStatus statut) {
        Transaction tx = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();
            session.createMutationQuery(
                    "UPDATE Message m SET m.statut = :statut WHERE m.id = :id")
                    .setParameter("statut", statut)
                    .setParameter("id", messageId)
                    .executeUpdate();
            tx.commit();
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            throw new RuntimeException("Erreur updateStatut : " + e.getMessage(), e);
        }
    }

    /**
     * Retourne la conversation entre deux utilisateurs, triée chronologiquement.
     * RG8 : ordre croissant par dateEnvoi
     */
    public List<Message> findConversation(String username1, String username2) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery(
                    "FROM Message m WHERE " +
                    "(m.sender.username = :u1 AND m.receiver.username = :u2) OR " +
                    "(m.sender.username = :u2 AND m.receiver.username = :u1) " +
                    "ORDER BY m.dateEnvoi ASC", Message.class)
                    .setParameter("u1", username1)
                    .setParameter("u2", username2)
                    .list();
        }
    }

    /**
     * Retourne les messages en attente (ENVOYE) pour un utilisateur qui vient de se connecter.
     * RG6 : livraison différée
     */
    public List<Message> findPendingMessages(String receiverUsername) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery(
                    "FROM Message m WHERE m.receiver.username = :username " +
                    "AND m.statut = :statut ORDER BY m.dateEnvoi ASC", Message.class)
                    .setParameter("username", receiverUsername)
                    .setParameter("statut", MessageStatus.ENVOYE)
                    .list();
        }
    }
}
