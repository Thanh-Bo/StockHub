package com.stockhub.auth.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.stockhub.auth.entity.User;

/**
 * Spring Data JPA repository for {@link User} entity.
 */
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    /**
     * Find a user by their email address.
     *
     * @param email the email to search for
     * @return an Optional containing the user if found
     */
    Optional<User> findByEmail(String email);

    /**
     * Find a user by their Google ID (OAuth2 sub claim).
     *
     * @param googleId the Google ID to search for
     * @return an Optional containing the user if found
     */
    Optional<User> findByGoogleId(String googleId);

    /**
     * Check whether a user exists with the given email.
     *
     * @param email the email to check
     * @return true if a user with this email exists
     */
    boolean existsByEmail(String email);

    /**
     * Count total number of users.
     *
     * @return total user count
     */
    long count();
}
