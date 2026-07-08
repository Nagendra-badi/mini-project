package com.ddms.controller;

import com.ddms.model.ActivityLog;
import com.ddms.model.Document;
import com.ddms.model.User;
import com.ddms.service.ActivityLogService;
import com.ddms.service.DocumentService;
import com.ddms.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    @Autowired
    private UserService userService;

    @Autowired
    private DocumentService documentService;

    @Autowired
    private ActivityLogService activityLogService;

    private boolean isAdmin(HttpSession session) {
        User user = (User) session.getAttribute("user");
        return user != null && "ADMIN".equalsIgnoreCase(user.getRole());
    }

    @GetMapping("/users")
    public ResponseEntity<?> getAllUsers(HttpSession session) {
        if (!isAdmin(session)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Access denied. Admin role required."));
        }
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id, HttpSession session) {
        if (!isAdmin(session)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Access denied. Admin role required."));
        }
        try {
            User loggedIn = (User) session.getAttribute("user");
            userService.deleteUser(id);
            activityLogService.log(loggedIn.getUsername(), "ADMIN_DELETE_USER", "Deleted user account with ID: " + id);
            return ResponseEntity.ok(Map.of("message", "User deleted successfully."));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/documents")
    public ResponseEntity<?> getAllDocuments(
            @RequestParam(value = "query", required = false) String query,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "type", required = false) String type,
            @RequestParam(value = "date", required = false) String dateStr,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            HttpSession session) {

        if (!isAdmin(session)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Access denied. Admin role required."));
        }

        LocalDate uploadDate = null;
        if (dateStr != null && !dateStr.trim().isEmpty()) {
            try {
                uploadDate = LocalDate.parse(dateStr);
            } catch (Exception e) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "Invalid date format."));
            }
        }

        // Passing null for user to search across ALL users system-wide
        Page<Document> documents = documentService.searchAndFilterDocuments(
                null, query, category, type, uploadDate, page, size
        );
        return ResponseEntity.ok(documents);
    }

    @GetMapping("/statistics")
    public ResponseEntity<?> getStatistics(HttpSession session) {
        if (!isAdmin(session)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Access denied. Admin role required."));
        }

        Map<String, Object> stats = documentService.getAdminStatistics();
        stats.put("totalUsers", userService.getAllUsers().size());
        
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/activity-logs")
    public ResponseEntity<?> getActivityLogs(HttpSession session) {
        if (!isAdmin(session)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Access denied. Admin role required."));
        }
        return ResponseEntity.ok(activityLogService.getAllLogs());
    }
}
