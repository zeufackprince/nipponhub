package com.nipponhub.nipponhubv0.Models.Enum;

/**
 * ROLES HIERARCHY
 * ─────────────────────────────────────────────────
 *  ADMIN   – system administrator, full access
 *  OWNER   – business owner, same scope as ADMIN
 *  CLIENT  – authenticated customer, can place / view own orders
 *  USER    – legacy alias for CLIENT (kept for backwards compat)
 *  CUSTOMER– legacy alias for CLIENT
 *  PARTNER – partner / supplier
 *  GUEST   – read-only, rarely needed (public endpoints cover most reads)
 * ─────────────────────────────────────────────────
 */
public enum UserRole {
    ADMIN,
    OWNER,
    CLIENT,  // legacy – treated as CLIENT
    PARTNER,
    MANAGER, // future use
}
