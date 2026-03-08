package model;

/**
 * Cycle de vie d'un message.
 * ENVOYE → RECU (destinataire connecté) → LU (destinataire a ouvert la conversation)
 */
public enum MessageStatus {
    ENVOYE,
    RECU,
    LU
}
