package com.magictech.core.auth;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Find user by username (case-insensitive)
     */
    Optional<User> findByUsernameIgnoreCase(String username);

    /**
     * Find user by username and check if active
     */
    Optional<User> findByUsernameIgnoreCaseAndActiveTrue(String username);

    /**
     * Find all users by role
     */
    List<User> findByRole(UserRole role);

    /**
     * Find all active users
     */
    List<User> findByActiveTrue();

    /**
     * Check if username exists (case-insensitive)
     */
    boolean existsByUsernameIgnoreCase(String username);

    /**
     * Count users by role
     */
    long countByRole(UserRole role);

    /**
     * Find all users by role and active status
     */
    List<User> findByRoleAndActive(UserRole role, Boolean active);

    /**
     * Find all active users by role (convenience method)
     */
    List<User> findByRoleAndActiveTrue(UserRole role);

    /**
     * Custom query to find master users
     */
    @Query("SELECT u FROM User u WHERE u.role = 'MASTER' AND u.active = true")
    List<User> findActiveMasterUsers();
}