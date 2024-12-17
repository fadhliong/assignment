package org.crypto.assignment.controller;

import org.crypto.assignment.model.dto.TradeRequest;
import org.crypto.assignment.model.dto.TradeResponse;
import org.crypto.assignment.service.TradeService;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/trades")
public class TradeController {
    private final TradeService tradeService;

    public TradeController(TradeService tradeService) {
        this.tradeService = tradeService;
    }

    @PostMapping
    public ResponseEntity<TradeResponse> executeTrade(@Validated @RequestBody TradeRequest request) {
        return ResponseEntity.ok(tradeService.executeTrade(request));
    }
}
