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

    @Autowired
    private com.ddms.repository.DocumentRepository documentRepository;

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
            
            // Refresh the logged-in admin's session details in case their database ID changed during re-indexing!
            if (loggedIn != null) {
                User updatedAdmin = userService.findByUsername(loggedIn.getUsername());
                if (updatedAdmin != null) {
                    session.setAttribute("user", updatedAdmin);
                    loggedIn = updatedAdmin;
                }
            }
            
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

    @GetMapping("/user-file-reports")
    public ResponseEntity<?> getUserFileReports(HttpSession session) {
        if (!isAdmin(session)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Access denied. Admin role required."));
        }
        try {
            List<User> users = userService.getAllUsers();
            List<Map<String, Object>> reports = new java.util.ArrayList<>();
            for (User u : users) {
                if ("USER".equalsIgnoreCase(u.getRole())) {
                    Map<String, Object> userReport = new HashMap<>();
                    userReport.put("userId", u.getId());
                    userReport.put("username", u.getUsername());
                    userReport.put("fullName", u.getFullName() != null ? u.getFullName() : u.getUsername());
                    userReport.put("email", u.getEmail());
                    userReport.put("phone", u.getPhone());

                    List<Document> docs = documentRepository.findByUploadedBy(u);
                    List<Map<String, Object>> docsList = new java.util.ArrayList<>();
                    for (Document d : docs) {
                        Map<String, Object> docMap = new HashMap<>();
                        docMap.put("id", d.getId());
                        docMap.put("originalName", d.getOriginalName());
                        docMap.put("fileSize", d.getFileSize());
                        docMap.put("fileType", d.getFileType());
                        docMap.put("uploadDate", d.getUploadDate());
                        docMap.put("description", d.getDescription());
                        docMap.put("s3Url", d.getS3Url());
                        docsList.add(docMap);
                    }
                    userReport.put("documents", docsList);
                    userReport.put("totalFiles", docsList.size());
                    reports.add(userReport);
                }
            }
            return ResponseEntity.ok(reports);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        }
    }
}
