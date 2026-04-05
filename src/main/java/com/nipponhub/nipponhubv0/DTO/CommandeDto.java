package com.nipponhub.nipponhubv0.DTO;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.nipponhub.nipponhubv0.Models.Enum.CommandeStatus;

import lombok.Data;
import lombok.NoArgsConstructor;
 
/**
 * DTO used both for incoming order requests (POST) and outgoing responses (GET).
 *
 * POST /api/v0/commande/new  — only `items` and `note` are read from the request body.
 * All other fields are populated by the service on the way out.
 */
@Data
@NoArgsConstructor
public class CommandeDto {
 
    // ── Response-only fields ───────────────────────────────────────────────
    private Long   id;
    private Long   clientId;
    private String clientName;
    private String clientEmail;
    private CommandeStatus status;
    private LocalDateTime  createdAt;
    private LocalDateTime  updatedAt;
    private String confirmedBy;
    private BigDecimal total;
    private Long   venteId;        // set once DELIVERED
    private String message;
 
    // ── Request + response fields ──────────────────────────────────────────
    private List<CommandeItemDto> items;
    private String note;
}
