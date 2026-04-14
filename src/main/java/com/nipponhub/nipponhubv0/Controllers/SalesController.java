package com.nipponhub.nipponhubv0.Controllers;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.nipponhub.nipponhubv0.DTO.SalesDto;
import com.nipponhub.nipponhubv0.Models.Sales;
import com.nipponhub.nipponhubv0.Services.SalesService;

import lombok.AllArgsConstructor;

@RestController
@RequestMapping("/api/v0/Sales")
@AllArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
public class SalesController {

    private final SalesService SalesService;

    /**
     * POST /api/v0/Sales/register
     * Enregistre une Sales, calcule le gain et déduit les stocks.
     */
    @PostMapping("/register")
    public ResponseEntity<SalesDto> registerSales(@RequestBody Sales Sales) {
        SalesDto res = SalesService.registerSales(Sales);
        return new ResponseEntity<>(res, HttpStatus.CREATED);
    }

    /**
     * GET /api/v0/Sales/all
     * Liste toutes les Saless effectuées.
     */
    @GetMapping("/allVan")
    public ResponseEntity<List<SalesDto>> getAllSaless() {
        return ResponseEntity.ok(SalesService.getAllSales());
    }

    /**
     * GET /api/v0/Sales/{id}
     * Récupère une Sales spécifique par son identifiant.
     */
    @GetMapping("/{id}")
    public ResponseEntity<SalesDto> getSalesById(@PathVariable Long id) {
        return ResponseEntity.ok(SalesService.getSalesById(id));
    }

    /**
     * GET /api/v0/Sales/search?date=YYYY-MM-DD
     * Recherche les Saless par date spécifique.
     */
    @GetMapping("/search")
    public ResponseEntity<List<SalesDto>> getSalessByDate(
            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(SalesService.getSalessByDate(date));
    }
}
