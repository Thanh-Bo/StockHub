package com.stockhub.watchlist.repository;

import com.stockhub.watchlist.entity.Watchlist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link Watchlist} entity.
 */
@Repository
public interface WatchlistRepository extends JpaRepository<Watchlist, UUID> {

    /**
     * Find all watchlists for a user ordered by sort order.
     *
     * @param userId the user UUID
     * @return list of watchlists belonging to the user
     */
    List<Watchlist> findByUserIdOrderBySortOrder(UUID userId);

    /**
     * Find a specific watchlist by its ID and the owning user's ID.
     *
     * @param id     the watchlist UUID
     * @param userId the user UUID
     * @return an Optional containing the watchlist if found and owned by the user
     */
    Optional<Watchlist> findByIdAndUserId(UUID id, UUID userId);

    /**
     * Count the number of watchlists owned by a user.
     *
     * @param userId the user UUID
     * @return the number of watchlists
     */
    long countByUserId(UUID userId);
}
