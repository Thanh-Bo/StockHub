package com.stockhub.prices.repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.stockhub.prices.entity.StockPrice;

/**
 * Spring Data JPA repository for {@link StockPrice} entity.
 */
@Repository
public interface StockPriceRepository extends JpaRepository<StockPrice, UUID> {

    /**
     * Find the most recent stock price for a given company using DISTINCT ON.
     *
     * @param companyId the company UUID
     * @return an Optional containing the latest stock price if found
     */
    @Query(value = "SELECT DISTINCT ON (sp.company_id) sp.* FROM stock_prices sp " +
                   "WHERE sp.company_id = :companyId " +
                   "ORDER BY sp.company_id, sp.date DESC",
           nativeQuery = true)
    Optional<StockPrice> findLatestByCompanyId(@Param("companyId") UUID companyId);

    /**
     * Find the latest stock prices for multiple companies using DISTINCT ON
     * with a LATERAL JOIN to fetch the previous close for each company.
     *
     * @param companyIds list of company UUIDs
     * @return list of the latest StockPrice for each company
     */
    @Query(value = "SELECT sp.* FROM stock_prices sp " +
                   "INNER JOIN (SELECT DISTINCT ON (company_id) id, company_id " +
                   "            FROM stock_prices " +
                   "            WHERE company_id IN (:companyIds) " +
                   "            ORDER BY company_id, date DESC) latest " +
                   "ON sp.id = latest.id",
           nativeQuery = true)
    List<StockPrice> findLatestPrices(@Param("companyIds") List<UUID> companyIds);

    /**
     * Find stock prices for a company within a date range, ordered by date descending.
     *
     * @param companyId the company UUID
     * @param from      the start date (inclusive)
     * @param to        the end date (inclusive)
     * @param pageable  pagination information
     * @return paginated list of stock prices
     */
    List<StockPrice> findByCompanyIdAndDateBetweenOrderByDateDesc(
            UUID companyId, LocalDate from, LocalDate to, Pageable pageable);

    /**
     * Find the latest closing price for a company.
     *
     * @param companyId the company UUID
     * @return an Optional containing the latest closing price
     */
    @Query(value = "SELECT sp.close FROM stock_prices sp " +
                   "WHERE sp.company_id = :companyId " +
                   "ORDER BY sp.date DESC LIMIT 1",
           nativeQuery = true)
    Optional<BigDecimal> findLatestClose(@Param("companyId") UUID companyId);
}
