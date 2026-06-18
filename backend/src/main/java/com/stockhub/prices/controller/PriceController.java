package com.stockhub.prices.controller;

import com.stockhub.company.dto.PricePoint;
import com.stockhub.prices.dto.PriceHistoryResponse;
import com.stockhub.prices.dto.PriceSnapshotResponse;
import com.stockhub.prices.service.PriceService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/companies/{ticker}/prices")
public class PriceController {

    private final PriceService priceService;

    public PriceController(PriceService priceService) {
        this.priceService = priceService;
    }

    @GetMapping
    public ResponseEntity<PriceHistoryResponse> getPriceHistory(
            @PathVariable String ticker,
            @RequestParam(defaultValue = "1Y") String range,
            @RequestParam(defaultValue = "1d") String interval) {
        List<PricePoint> data = priceService.getPriceHistory(ticker, range);
        PriceHistoryResponse response = new PriceHistoryResponse(ticker, range, data);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/latest")
    public ResponseEntity<PriceSnapshotResponse> getLatestPrice(
            @PathVariable String ticker) {
        PriceSnapshotResponse response = priceService.getLatestPriceSnapshot(ticker);
        return ResponseEntity.ok(response);
    }
}
