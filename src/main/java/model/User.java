package model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.io.Serializable;

/**
 * Entité JPA représentant un utilisateur.
 * RG1  : username unique
 * RG9  : password stocké haché (BCrypt)
 * RG4  : status ONLINE/OFFLINE
 */
@Entity
@Table(name = "users")
public class User implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 50)
    private String username;

    @Column(nullable = false)
    private String password; // Haché via BCrypt (RG9)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserStatus status = UserStatus.OFFLINE;

    @Column(name = "date_creation", nullable = false)
    private LocalDateTime dateCreation = LocalDateTime.now();

    // ── Constructeurs ──────────────────────────────────────────────
    public User() {}

    public User(String username, String password) {
        this.username     = username;
        this.password     = password;
        this.status       = UserStatus.OFFLINE;
        this.dateCreation = LocalDateTime.now();
    }

    // ── Getters / Setters ──────────────────────────────────────────
    public Long getId()                          { return id; }
    public String getUsername()                  { return username; }
    public void setUsername(String username)     { this.username = username; }
    public String getPassword()                  { return password; }
    public void setPassword(String password)     { this.password = password; }
    public UserStatus getStatus()                { return status; }
    public void setStatus(UserStatus status)     { this.status = status; }
    public LocalDateTime getDateCreation()       { return dateCreation; }

    @Override
    public String toString() {
        return username + " [" + status + "]";
    }
}
