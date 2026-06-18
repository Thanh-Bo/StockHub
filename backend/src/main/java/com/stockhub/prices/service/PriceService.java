package com.stockhub.prices.service;

import com.stockhub.common.exception.CompanyNotFoundException;
import com.stockhub.company.dto.PricePoint;
import com.stockhub.company.entity.Company;
import com.stockhub.company.repository.CompanyRepository;
import com.stockhub.prices.dto.PriceSnapshotResponse;
import com.stockhub.prices.entity.StockPrice;
import com.stockhub.prices.repository.StockPriceRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class PriceService {

    private final StockPriceRepository stockPriceRepository;
    private final CompanyRepository companyRepository;

    public PriceService(StockPriceRepository stockPriceRepository,
                        CompanyRepository companyRepository) {
        this.stockPriceRepository = stockPriceRepository;
        this.companyRepository = companyRepository;
    }

    /**
     * Get price history for a ticker within a given range.
     * Supported ranges: 1M, 3M, 6M, 1Y, 5Y, MAX.
     */
    public List<PricePoint> getPriceHistory(String ticker, String range) {
        Company company = getCompany(ticker);
        LocalDate to = LocalDate.now();
        LocalDate from = resolveFromDate(range, to);

        List<StockPrice> prices;
        if (from != null) {
            prices = stockPriceRepository.findByCompanyIdAndDateBetweenOrderByDateDesc(
                    company.getId(), from, to,
                    org.springframework.data.domain.Pageable.unpaged());
        } else {
            // MAX range: get all prices with a large page
            prices = stockPriceRepository.findByCompanyIdAndDateBetweenOrderByDateDesc(
                    company.getId(), LocalDate.of(1900, 1, 1), to,
                    org.springframework.data.domain.Pageable.unpaged());
        }

        return prices.stream()
                .map(sp -> new PricePoint(sp.getDate(), sp.getClose(), sp.getAdjustedClose(), sp.getVolume()))
                .collect(Collectors.toList());
    }

    /**
     * Get the latest closing price for a ticker.
     */
    public BigDecimal getLatestPrice(String ticker) {
        Company company = getCompany(ticker);
        return stockPriceRepository.findLatestClose(company.getId())
                .orElse(BigDecimal.ZERO);
    }

    /**
     * Get the latest price snapshot including change, high, low, volume, etc.
     */
    public PriceSnapshotResponse getLatestPriceSnapshot(String ticker) {
        Company company = getCompany(ticker);
        StockPrice latest = stockPriceRepository.findLatestByCompanyId(company.getId())
                .orElse(null);

        if (latest == null) {
            return new PriceSnapshotResponse(
                    ticker, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                    BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                    BigDecimal.ZERO, 0L, null
            );
        }

        BigDecimal previousClose = getPreviousClose(company.getId(), latest);
        BigDecimal change = latest.getClose().subtract(previousClose);
        BigDecimal changePercent = previousClose.compareTo(BigDecimal.ZERO) != 0
                ? change.divide(previousClose, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                : BigDecimal.ZERO;

        return new PriceSnapshotResponse(
                ticker,
                latest.getClose(),
                change,
                changePercent,
                latest.getHigh(),
                latest.getLow(),
                previousClose,
                latest.getOpen(),
                latest.getVolume(),
                latest.getDate()
        );
    }

    // --- Private helpers ---

    private Company getCompany(String ticker) {
        return companyRepository.findByTicker(ticker.toUpperCase())
                .orElseThrow(() -> new CompanyNotFoundException(ticker));
    }

    private LocalDate resolveFromDate(String range, LocalDate to) {
        if (range == null) {
            return to.minusYears(1); // default to 1Y
        }
        return switch (range.toUpperCase()) {
            case "1M" -> to.minusMonths(1);
            case "3M" -> to.minusMonths(3);
            case "6M" -> to.minusMonths(6);
            case "1Y" -> to.minusYears(1);
            case "5Y" -> to.minusYears(5);
            case "MAX" -> null;
            default -> to.minusYears(1);
        };
    }

    private BigDecimal getPreviousClose(java.util.UUID companyId, StockPrice latestPrice) {
        List<StockPrice> prices = stockPriceRepository.findByCompanyIdAndDateBetweenOrderByDateDesc(
                companyId, latestPrice.getDate().minusDays(5), latestPrice.getDate().minusDays(1),
                PageRequest.of(0, 1)
        );
        return prices.isEmpty() ? latestPrice.getOpen() : prices.get(0).getClose();
    }
}
