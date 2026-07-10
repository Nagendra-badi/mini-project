package com.ddms.controller;

import com.ddms.model.User;
import com.ddms.service.UserService;
import com.ddms.service.ActivityLogService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UserService userService;

    @Autowired
    private ActivityLogService activityLogService;

    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> register(@RequestBody User user) {
        Map<String, String> response = new HashMap<>();
        try {
            if (user.getRole() == null || user.getRole().isEmpty()) {
                user.setRole("USER"); // Default role
            }
            User registeredUser = userService.register(user);
            activityLogService.log(registeredUser.getUsername(), "REGISTER", "Successfully registered new account");
            response.put("message", "Registration successful.");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody Map<String, String> credentials, HttpSession session) {
        Map<String, Object> response = new HashMap<>();
        try {
            String username = credentials.get("username");
            String password = credentials.get("password");
            User user = userService.login(username, password);
            
            // Set User object in HttpSession
            session.setAttribute("user", user);
            activityLogService.log(user.getUsername(), "LOGIN", "Successfully logged in to system");

            response.put("message", "Login successful.");
            response.put("username", user.getUsername());
            response.put("role", user.getRole());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(HttpSession session) {
        Map<String, String> response = new HashMap<>();
        User user = (User) session.getAttribute("user");
        if (user != null) {
            activityLogService.log(user.getUsername(), "LOGOUT", "User logged out");
        }
        session.invalidate();
        response.put("message", "Logout successful.");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/profile")
    public ResponseEntity<?> getProfile(HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Unauthorized"));
        }
        // Refetch user to get up-to-date details
        return userService.findById(user.getId())
                .map(u -> ResponseEntity.ok((Object) u))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "User not found")));
    }

    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(@RequestBody Map<String, String> profileData, HttpSession session) {
        User loggedInUser = (User) session.getAttribute("user");
        if (loggedInUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Unauthorized"));
        }
        try {
            String fullName = profileData.get("fullName");
            String phone = profileData.get("phone");
            String email = profileData.get("email");
            User updated = userService.updateProfile(loggedInUser.getId(), fullName, phone, email);
            
            // Update session user
            session.setAttribute("user", updated);
            
            activityLogService.log(updated.getUsername(), "UPDATE_PROFILE", "Updated profile details");
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(@RequestBody Map<String, String> passwordData, HttpSession session) {
        User loggedInUser = (User) session.getAttribute("user");
        if (loggedInUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Unauthorized"));
        }
        try {
            String oldPassword = passwordData.get("oldPassword");
            String newPassword = passwordData.get("newPassword");
            userService.changePassword(loggedInUser.getId(), oldPassword, newPassword);
            activityLogService.log(loggedInUser.getUsername(), "CHANGE_PASSWORD", "Changed account password");
            return ResponseEntity.ok(Map.of("message", "Password changed successfully."));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody Map<String, String> resetData) {
        try {
            String username = resetData.get("username");
            String email = resetData.get("email");
            String newPassword = resetData.get("newPassword");
            User user = userService.forgotPassword(username, email, newPassword);
            activityLogService.log(user.getUsername(), "RESET_PASSWORD", "Simulated forgot password reset");
            return ResponseEntity.ok(Map.of("message", "Password reset successfully. You can now login."));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        }
    }

    @Autowired
    private com.ddms.repository.DocumentRepository documentRepository;

    @GetMapping("/test-db")
    public ResponseEntity<?> testDb() {
        Map<String, Object> result = new HashMap<>();
        try {
            java.util.List<User> users = userService.getAllUsers();
            java.util.List<Map<String, Object>> usersList = new java.util.ArrayList<>();
            for (User u : users) {
                Map<String, Object> item = new HashMap<>();
                item.put("id", u.getId());
                item.put("username", u.getUsername());
                item.put("role", u.getRole());
                usersList.add(item);
            }
            result.put("users", usersList);

            java.util.List<com.ddms.model.Document> docs = documentRepository.findAll();
            java.util.List<Map<String, Object>> docsList = new java.util.ArrayList<>();
            for (com.ddms.model.Document d : docs) {
                Map<String, Object> item = new HashMap<>();
                item.put("id", d.getId());
                item.put("originalName", d.getOriginalName());
                item.put("uploadedBy_id", d.getUploadedBy() != null ? d.getUploadedBy().getId() : null);
                item.put("uploadedBy_username", d.getUploadedBy() != null ? d.getUploadedBy().getUsername() : null);
                docsList.add(item);
            }
            result.put("documents", docsList);

            // Run test query for userId = 1 (Bhaskar)
            try {
                org.springframework.data.domain.Page<com.ddms.model.Document> testQueryPage = documentRepository.searchDocuments(
                        1L, null, null, null, null, null, org.springframework.data.domain.PageRequest.of(0, 10)
                );
                result.put("test_query_success", true);
                result.put("test_query_count", testQueryPage.getTotalElements());
            } catch (Exception queryEx) {
                result.put("test_query_success", false);
                result.put("test_query_error", queryEx.getMessage() != null ? queryEx.getMessage() : queryEx.toString());
            }
        } catch (Exception e) {
            result.put("error", e.getMessage());
        }
        return ResponseEntity.ok(result);
    }
}
