package com.stockhub.company.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.stockhub.company.entity.Company;

/**
 * Spring Data JPA repository for {@link Company} entity.
 */
@Repository
public interface CompanyRepository extends JpaRepository<Company, UUID> {

    /**
     * Find a company by its ticker symbol.
     *
     * @param ticker the ticker symbol
     * @return an Optional containing the company if found
     */
    Optional<Company> findByTicker(String ticker);

    /**
     * Find all companies whose ticker is in the given list.
     *
     * @param tickers list of ticker symbols
     * @return list of matching companies
     */
    List<Company> findByTickerIn(List<String> tickers);

    /**
     * Find companies matching the given sector and industry.
     *
     * @param sector   the sector name
     * @param industry the industry name
     * @return list of matching companies
     */
    List<Company> findBySectorAndIndustry(String sector, String industry);

    /**
     * Find all active companies.
     *
     * @return list of active companies
     */
    List<Company> findByIsActiveTrue();

    /**
     * Find all active companies ordered by market cap descending.
     *
     * @return list of active companies sorted by market cap (highest first)
     */
    List<Company> findByIsActiveTrueOrderByMarketCapDesc();

    /**
     * Autocomplete search using pg_trgm similarity on ticker and name.
     * Returns companies ranked by combined similarity score.
     *
     * @param query    the search query
     * @param pageable pagination with limit
     * @return list of matching companies
     */
    @Query(value = "SELECT c.*, " +
                   "  (similarity(c.ticker, :query) * 0.6 + similarity(c.name, :query) * 0.4) AS rank " +
                   "FROM company c " +
                   "WHERE c.is_active = true " +
                   "  AND (similarity(c.ticker, :query) > 0.1 OR similarity(c.name, :query) > 0.1) " +
                   "ORDER BY rank DESC",
           nativeQuery = true)
    List<Company> autocomplete(@Param("query") String query, Pageable pageable);

    /**
     * Full-text search using tsvector and trigram similarity.
     * Returns companies ranked by combined relevance.
     *
     * @param query    the search query
     * @param pageable pagination with limit
     * @return list of matching companies
     */
    @Query(value = "SELECT c.*, " +
                   "  COALESCE(ts_rank(to_tsvector('english', c.name || ' ' || COALESCE(c.description, '')), " +
                   "    plainto_tsquery('english', :query)), 0) * 0.7 + " +
                   "  GREATEST(similarity(c.ticker, :query), similarity(c.name, :query)) * 0.3 AS rank " +
                   "FROM company c " +
                   "WHERE c.is_active = true " +
                   "  AND (to_tsvector('english', c.name || ' ' || COALESCE(c.description, '')) @@ plainto_tsquery('english', :query) " +
                   "    OR similarity(c.ticker, :query) > 0.05 " +
                   "    OR similarity(c.name, :query) > 0.05) " +
                   "ORDER BY rank DESC",
           nativeQuery = true)
    List<Company> fullSearch(@Param("query") String query, Pageable pageable);
}
