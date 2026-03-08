package model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.io.Serializable;

/**
 * Entité JPA représentant un message.
 * RG7  : contenu non vide, max 1000 chars
 * RG8  : trié par dateEnvoi ASC dans les requêtes
 * RG6  : stocké même si destinataire OFFLINE
 */
@Entity
@Table(name = "messages")
public class Message implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "receiver_id", nullable = false)
    private User receiver;

    @Column(nullable = false, length = 1000)
    private String contenu;

    @Column(name = "date_envoi", nullable = false)
    private LocalDateTime dateEnvoi = LocalDateTime.now();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MessageStatus statut = MessageStatus.ENVOYE;

    // ── Constructeurs ──────────────────────────────────────────────
    public Message() {}

    public Message(User sender, User receiver, String contenu) {
        this.sender    = sender;
        this.receiver  = receiver;
        this.contenu   = contenu;
        this.dateEnvoi = LocalDateTime.now();
        this.statut    = MessageStatus.ENVOYE;
    }

    // ── Getters / Setters ──────────────────────────────────────────
    public Long getId()                            { return id; }
    public User getSender()                        { return sender; }
    public User getReceiver()                      { return receiver; }
    public String getContenu()                     { return contenu; }
    public void setContenu(String contenu)         { this.contenu = contenu; }
    public LocalDateTime getDateEnvoi()            { return dateEnvoi; }
    public MessageStatus getStatut()               { return statut; }
    public void setStatut(MessageStatus statut)    { this.statut = statut; }

    @Override
    public String toString() {
        return "[" + dateEnvoi + "] " + sender.getUsername() + " → " + receiver.getUsername() + " : " + contenu;
    }
}
