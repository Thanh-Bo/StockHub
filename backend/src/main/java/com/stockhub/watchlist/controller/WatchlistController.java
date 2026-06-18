package com.stockhub.watchlist.controller;

import com.stockhub.auth.security.UserPrincipal;
import com.stockhub.watchlist.dto.AddStockRequest;
import com.stockhub.watchlist.dto.CreateWatchlistRequest;
import com.stockhub.watchlist.dto.ReorderRequest;
import com.stockhub.watchlist.dto.UpdateWatchlistRequest;
import com.stockhub.watchlist.dto.WatchlistDetailResponse;
import com.stockhub.watchlist.dto.WatchlistResponse;
import com.stockhub.watchlist.dto.WatchlistSummaryResponse;
import com.stockhub.watchlist.service.WatchlistService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/watchlists")
public class WatchlistController {

    private final WatchlistService watchlistService;

    public WatchlistController(WatchlistService watchlistService) {
        this.watchlistService = watchlistService;
    }

    @GetMapping
    public ResponseEntity<List<WatchlistSummaryResponse>> getWatchlists(
            @AuthenticationPrincipal UserPrincipal principal) {
        List<WatchlistSummaryResponse> response = watchlistService.getWatchlists(principal.getId());
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<WatchlistResponse> createWatchlist(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateWatchlistRequest request) {
        WatchlistResponse response = watchlistService.createWatchlist(principal.getId(), request);
        return ResponseEntity.created(URI.create("/api/v1/watchlists/" + response.id()))
                .body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<WatchlistDetailResponse> getWatchlistDetail(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id) {
        WatchlistDetailResponse response = watchlistService.getWatchlistDetail(id, principal.getId());
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<WatchlistResponse> updateWatchlist(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateWatchlistRequest request) {
        WatchlistResponse response = watchlistService.updateWatchlist(id, principal.getId(), request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteWatchlist(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id) {
        watchlistService.deleteWatchlist(id, principal.getId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/stocks")
    public ResponseEntity<Void> addStock(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id,
            @Valid @RequestBody AddStockRequest request) {
        watchlistService.addStock(id, principal.getId(), request.ticker());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}/stocks/{ticker}")
    public ResponseEntity<Void> removeStock(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id,
            @PathVariable String ticker) {
        watchlistService.removeStock(id, principal.getId(), ticker);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/stocks/reorder")
    public ResponseEntity<Void> reorderStocks(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id,
            @Valid @RequestBody ReorderRequest request) {
        watchlistService.reorderStocks(id, principal.getId(), request.orderedTickers());
        return ResponseEntity.noContent().build();
    }
}
