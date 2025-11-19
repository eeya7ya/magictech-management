package com.magictech.core.auth;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Authentication Service
 * Manages users and their authentication using PostgreSQL database
 */
@Service
public class AuthenticationService {

    @Autowired
    private UserRepository userRepository;

    /**
     * Initialize predefined users in database (runs once on startup)
     */
    @PostConstruct
    @Transactional
    public void initializeUsers() {
        // Check if users already exist
        if (userRepository.count() > 0) {
            System.out.println("Users already initialized. Skipping...");
            return;
        }

        System.out.println("Initializing default users...");

        // Master User - Full Access
        createUserIfNotExists("admin", "admin123", UserRole.MASTER);
        createUserIfNotExists("manager", "manager123", UserRole.MASTER);

        // Department Users
        createUserIfNotExists("john", "sales123", UserRole.SALES);
        createUserIfNotExists("mike", "main123", UserRole.MAINTENANCE);
        createUserIfNotExists("sara", "proj123", UserRole.PROJECTS);
        createUserIfNotExists("emma", "price123", UserRole.PRICING);
        createUserIfNotExists("david", "store123", UserRole.STORAGE);

        System.out.println("Default users initialized successfully!");
    }

    /**
     * Create user if doesn't exist
     */
    private void createUserIfNotExists(String username, String password, UserRole role) {
        if (!userRepository.existsByUsernameIgnoreCase(username)) {
            User user = new User(username, password, role);
            userRepository.save(user);
            System.out.println("Created user: " + username + " with role: " + role);
        }
    }

    /**
     * Authenticate user with username and password
     * @return User object if authentication successful, null otherwise
     */
    @Transactional
    public User authenticate(String username, String password) {
        System.out.println("🔐 Authentication attempt for user: " + username);

        if (username == null || password == null || username.trim().isEmpty()) {
            System.out.println("❌ Authentication failed: Empty username or password");
            return null;
        }

        Optional<User> userOpt = userRepository.findByUsernameIgnoreCaseAndActiveTrue(username);

        if (userOpt.isEmpty()) {
            System.out.println("❌ Authentication failed: User not found or inactive: " + username);

            // Debug: Check if user exists but is inactive
            Optional<User> anyUserOpt = userRepository.findByUsernameIgnoreCase(username);
            if (anyUserOpt.isPresent()) {
                User debugUser = anyUserOpt.get();
                System.out.println("⚠️  User exists but: Active=" + debugUser.getActive() + " | Role=" + debugUser.getRole());
            }

            return null;
        }

        User user = userOpt.get();
        System.out.println("✓ User found: " + user.getUsername() + " | Role: " + user.getRole() + " | Active: " + user.getActive());

        // In production, use BCrypt or similar for password hashing
        if (user.getPassword().equals(password)) {
            System.out.println("✓ Password match! Authentication successful");
            // Update last login time
            user.setLastLogin(LocalDateTime.now());
            userRepository.save(user);
            return user;
        } else {
            System.out.println("❌ Password mismatch for user: " + username);
        }

        return null;
    }

    /**
     * Check if username exists
     */
    public boolean userExists(String username) {
        return userRepository.existsByUsernameIgnoreCase(username);
    }

    /**
     * Get all registered usernames (for reference/debugging)
     */
    public String[] getAllUsernames() {
        return userRepository.findAll().stream()
                .map(User::getUsername)
                .toArray(String[]::new);
    }

    /**
     * Get all active users
     */
    public List<User> getAllActiveUsers() {
        return userRepository.findByActiveTrue();
    }

    /**
     * Get users by role
     */
    public List<User> getUsersByRole(UserRole role) {
        return userRepository.findByRole(role);
    }

    /**
     * Get user by ID
     */
    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }

    /**
     * Get user by username
     */
    public Optional<User> getUserByUsername(String username) {
        return userRepository.findByUsernameIgnoreCase(username);
    }

    /**
     * Add new user (Master only)
     */
    @Transactional
    public User addUser(String username, String password, UserRole role) {
        if (userRepository.existsByUsernameIgnoreCase(username)) {
            throw new IllegalArgumentException("Username already exists");
        }

        User user = new User(username, password, role);
        return userRepository.save(user);
    }

    /**
     * Update user password
     */
    @Transactional
    public User updatePassword(Long userId, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        user.setPassword(newPassword);
        return userRepository.save(user);
    }

    /**
     * Deactivate user (soft delete)
     */
    @Transactional
    public void deactivateUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        user.setActive(false);
        userRepository.save(user);
    }

    /**
     * Activate user
     */
    @Transactional
    public void activateUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        user.setActive(true);
        userRepository.save(user);
    }

    /**
     * Delete user permanently (Master only - use with caution)
     */
    @Transactional
    public void deleteUser(Long userId) {
        userRepository.deleteById(userId);
    }

    /**
     * Get user information without password
     */
    public String getUserInfo(String username) {
        Optional<User> userOpt = userRepository.findByUsernameIgnoreCase(username);

        if (userOpt.isPresent()) {
            User user = userOpt.get();
            return String.format("User: %s | Role: %s | Active: %s | Last Login: %s",
                    user.getUsername(),
                    user.getRole().getDisplayName(),
                    user.getActive() ? "Yes" : "No",
                    user.getLastLogin() != null ? user.getLastLogin().toString() : "Never");
        }
        return "User not found";
    }

    /**
     * Get all users (including inactive)
     */
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    /**
     * Create new user
     */
    @Transactional
    public User createUser(User user) {
        System.out.println("📝 Creating user: " + user.getUsername() + " | Role: " + user.getRole() + " | Active: " + user.getActive());

        if (userRepository.existsByUsernameIgnoreCase(user.getUsername())) {
            throw new IllegalArgumentException("Username already exists");
        }

        // Ensure active flag is set
        if (user.getActive() == null) {
            user.setActive(true);
        }

        User savedUser = userRepository.save(user);
        System.out.println("✓ User created successfully: ID=" + savedUser.getId() + " | Username=" + savedUser.getUsername() + " | Active=" + savedUser.getActive());

        // Verify the user can be found
        boolean canFind = userRepository.existsByUsernameIgnoreCase(savedUser.getUsername());
        System.out.println("✓ User verification: Can find " + savedUser.getUsername() + "? " + canFind);

        return savedUser;
    }

    /**
     * Update existing user
     */
    @Transactional
    public User updateUser(User user) {
        User existingUser = userRepository.findById(user.getId())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // Update fields
        existingUser.setPassword(user.getPassword());
        existingUser.setRole(user.getRole());
        existingUser.setActive(user.getActive());
        existingUser.setPhotoPath(user.getPhotoPath());

        return userRepository.save(existingUser);
    }

    /**
     * Get total user count
     */
    public long getTotalUsers() {
        return userRepository.count();
    }

    /**
     * Get active user count
     */
    public long getActiveUserCount() {
        return userRepository.findByActiveTrue().size();
    }
}