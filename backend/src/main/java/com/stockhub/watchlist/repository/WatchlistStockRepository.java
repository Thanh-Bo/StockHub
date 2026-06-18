package com.stockhub.watchlist.repository;

import com.stockhub.watchlist.entity.WatchlistStock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link WatchlistStock} entity.
 */
@Repository
public interface WatchlistStockRepository extends JpaRepository<WatchlistStock, UUID> {

    /**
     * Find all stocks in a watchlist ordered by sort order.
     *
     * @param watchlistId the watchlist UUID
     * @return list of watchlist stocks
     */
    List<WatchlistStock> findByWatchlistIdOrderBySortOrder(UUID watchlistId);

    /**
     * Check whether a specific company already exists in a watchlist.
     *
     * @param watchlistId the watchlist UUID
     * @param companyId   the company UUID
     * @return true if the company is in the watchlist
     */
    boolean existsByWatchlistIdAndCompanyId(UUID watchlistId, UUID companyId);

    /**
     * Count the number of stocks in a watchlist.
     *
     * @param watchlistId the watchlist UUID
     * @return the number of stocks in the watchlist
     */
    long countByWatchlistId(UUID watchlistId);

    /**
     * Get the maximum sort order value in a watchlist.
     * Returns 0 if the watchlist is empty.
     *
     * @param watchlistId the watchlist UUID
     * @return the maximum sort order, or 0 if no stocks exist
     */
    @Query("SELECT COALESCE(MAX(ws.sortOrder), 0) FROM WatchlistStock ws WHERE ws.watchlistId = :watchlistId")
    int getMaxSortOrder(@Param("watchlistId") UUID watchlistId);

    /**
     * Delete a specific stock from a watchlist.
     *
     * @param watchlistId the watchlist UUID
     * @param companyId   the company UUID
     */
    @Modifying
    @Transactional
    void deleteByWatchlistIdAndCompanyId(UUID watchlistId, UUID companyId);
}
