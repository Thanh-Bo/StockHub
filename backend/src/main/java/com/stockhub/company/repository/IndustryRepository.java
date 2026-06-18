package com.stockhub.company.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.stockhub.company.entity.Industry;

/**
 * Spring Data JPA repository for {@link Industry} entity.
 */
@Repository
public interface IndustryRepository extends JpaRepository<Industry, UUID> {

    /**
     * Find an industry by its sector and industry name.
     *
     * @param sector   the sector name
     * @param industry the industry name
     * @return an Optional containing the industry if found
     */
    Optional<Industry> findBySectorAndIndustry(String sector, String industry);

    /**
     * Find all industries ordered by sector then industry alphabetically.
     *
     * @return all industries sorted by sector and industry
     */
    List<Industry> findAllByOrderBySectorAscIndustryAsc();
}
