package com.nipponhub.nipponhubv0.Models.Enum;

/**
 * Lifecycle of a client order (Commande).
 *
 *  PENDING   → order placed by client, awaiting admin action
 *  CONFIRMED → admin acknowledged the order (preparing / processing)
 *  DELIVERED → admin confirmed delivery
 *               ↳ triggers: stock decrement + Vente creation in DB
 *  CANCELLED → order cancelled (by admin or owner)
 */
public enum CommandeStatus {
    PENDING,
    CONFIRMED,
    DELIVERED,
    CANCELLED
}
