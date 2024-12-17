package org.crypto.assignment.controller;

import lombok.extern.log4j.Log4j2;
import org.crypto.assignment.model.entity.Price;
import org.crypto.assignment.service.PriceResponseService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/pricing")
@Log4j2
public class PriceController {

    private final PriceResponseService priceResponseService;

    public PriceController(PriceResponseService priceResponseService) {
        this.priceResponseService = priceResponseService;
    }

    @GetMapping("/latest")
    public ResponseEntity<Price> getLatestPrices() {
        Price latestPriceResponse = priceResponseService.getLatestPriceResponse();
        return ResponseEntity.ok(latestPriceResponse);
    }
}
