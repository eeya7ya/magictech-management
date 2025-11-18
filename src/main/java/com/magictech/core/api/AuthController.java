package com.magictech.core.api;

import com.magictech.core.auth.AuthenticationService;
import com.magictech.core.auth.User;
import com.magictech.core.auth.UserRole;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST API Controller for Authentication and User Management
 * Accessible remotely at: http://your-server:8080/api/auth
 */
@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*") // Allow all origins - restrict in production
public class AuthController {

    @Autowired
    private AuthenticationService authService;

    /**
     * Login endpoint
     * POST /api/auth/login
     * Body: {"username": "admin", "password": "admin123"}
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        try {
            User user = authService.authenticate(request.getUsername(), request.getPassword());

            if (user != null) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("userId", user.getId());
                response.put("username", user.getUsername());
                response.put("role", user.getRole());
                response.put("lastLogin", user.getLastLogin());
                response.put("message", "Login successful");
                return ResponseEntity.ok(response);
            }

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Invalid username or password");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "Server error: " + e.getMessage()));
        }
    }

    /**
     * Get all active users
     * GET /api/auth/users
     */
    @GetMapping("/users")
    public ResponseEntity<?> getAllUsers() {
        try {
            List<User> users = authService.getAllActiveUsers();
            // Remove passwords from response
            users.forEach(user -> user.setPassword("***"));
            return ResponseEntity.ok(users);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch users: " + e.getMessage()));
        }
    }

    /**
     * Get user by ID
     * GET /api/auth/users/{id}
     */
    @GetMapping("/users/{id}")
    public ResponseEntity<?> getUserById(@PathVariable Long id) {
        try {
            return authService.getUserById(id)
                    .map(user -> {
                        user.setPassword("***"); // Hide password
                        return ResponseEntity.ok(user);
                    })
                    .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                            .body(null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get users by role
     * GET /api/auth/users/role/{role}
     */
    @GetMapping("/users/role/{role}")
    public ResponseEntity<?> getUsersByRole(@PathVariable UserRole role) {
        try {
            List<User> users = authService.getUsersByRole(role);
            users.forEach(user -> user.setPassword("***"));
            return ResponseEntity.ok(users);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Create new user (Master only)
     * POST /api/auth/users
     * Body: {"username": "newuser", "password": "pass123", "role": "SALES"}
     */
    @PostMapping("/users")
    public ResponseEntity<?> createUser(@RequestBody CreateUserRequest request) {
        try {
            // Validate input
            if (request.getUsername() == null || request.getUsername().trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Username is required"));
            }
            if (request.getPassword() == null || request.getPassword().length() < 6) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Password must be at least 6 characters"));
            }
            if (request.getRole() == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Role is required"));
            }

            User user = authService.addUser(
                    request.getUsername(),
                    request.getPassword(),
                    request.getRole()
            );

            user.setPassword("***"); // Hide password in response
            return ResponseEntity.status(HttpStatus.CREATED).body(user);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to create user: " + e.getMessage()));
        }
    }

    /**
     * Update user password
     * PUT /api/auth/users/{id}/password
     * Body: {"newPassword": "newpass123"}
     */
    @PutMapping("/users/{id}/password")
    public ResponseEntity<?> updatePassword(
            @PathVariable Long id,
            @RequestBody PasswordUpdateRequest request) {
        try {
            if (request.getNewPassword() == null || request.getNewPassword().length() < 6) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Password must be at least 6 characters"));
            }

            authService.updatePassword(id, request.getNewPassword());
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Password updated successfully"
            ));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to update password: " + e.getMessage()));
        }
    }

    /**
     * Deactivate user (soft delete)
     * DELETE /api/auth/users/{id}
     */
    @DeleteMapping("/users/{id}")
    public ResponseEntity<?> deactivateUser(@PathVariable Long id) {
        try {
            authService.deactivateUser(id);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "User deactivated successfully"
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to deactivate user: " + e.getMessage()));
        }
    }

    /**
     * Activate user
     * PUT /api/auth/users/{id}/activate
     */
    @PutMapping("/users/{id}/activate")
    public ResponseEntity<?> activateUser(@PathVariable Long id) {
        try {
            authService.activateUser(id);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "User activated successfully"
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to activate user: " + e.getMessage()));
        }
    }

    /**
     * Get user statistics
     * GET /api/auth/stats
     */
    @GetMapping("/stats")
    public ResponseEntity<?> getUserStats() {
        try {
            Map<String, Object> stats = new HashMap<>();
            stats.put("totalUsers", authService.getTotalUsers());
            stats.put("activeUsers", authService.getActiveUserCount());
            stats.put("masterUsers", authService.getUsersByRole(UserRole.MASTER).size());
            stats.put("salesUsers", authService.getUsersByRole(UserRole.SALES).size());
            stats.put("maintenanceUsers", authService.getUsersByRole(UserRole.MAINTENANCE).size());
            stats.put("projectsUsers", authService.getUsersByRole(UserRole.PROJECTS).size());
            stats.put("pricingUsers", authService.getUsersByRole(UserRole.PRICING).size());
            stats.put("storageUsers", authService.getUsersByRole(UserRole.STORAGE).size());
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Health check endpoint
     * GET /api/auth/health
     */
    @GetMapping("/health")
    public ResponseEntity<?> healthCheck() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "MagicTech Authentication API",
                "timestamp", System.currentTimeMillis()
        ));
    }

    // ==================== Request DTOs ====================

    public static class LoginRequest {
        private String username;
        private String password;

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }

    public static class CreateUserRequest {
        private String username;
        private String password;
        private UserRole role;

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        public UserRole getRole() { return role; }
        public void setRole(UserRole role) { this.role = role; }
    }

    public static class PasswordUpdateRequest {
        private String newPassword;

        public String getNewPassword() { return newPassword; }
        public void setNewPassword(String newPassword) { this.newPassword = newPassword; }
    }
}