package com.ddms.controller;

import com.ddms.model.Document;
import com.ddms.model.User;
import com.ddms.service.DocumentService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    @Autowired
    private DocumentService documentService;

    @PostMapping("/upload")
    public ResponseEntity<?> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "categoryId", required = false) Long categoryId,
            HttpSession session) {
        
        User loggedInUser = (User) session.getAttribute("user");
        if (loggedInUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Unauthorized"));
        }

        if (file.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "Please select a file to upload."));
        }

        // File size validation (limit to 50MB, spring handles this automatically but check anyway)
        if (file.getSize() > 50 * 1024 * 1024) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "File exceeds maximum upload size (50MB)."));
        }

        try {
            Document doc = documentService.uploadDocument(file, description, categoryId, loggedInUser);
            return ResponseEntity.ok(doc);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<?> getUserDocuments(
            @RequestParam(value = "query", required = false) String query,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "type", required = false) String type,
            @RequestParam(value = "date", required = false) String dateStr,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            HttpSession session) {

        User loggedInUser = (User) session.getAttribute("user");
        if (loggedInUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Unauthorized"));
        }

        LocalDate uploadDate = null;
        if (dateStr != null && !dateStr.trim().isEmpty()) {
            try {
                uploadDate = LocalDate.parse(dateStr);
            } catch (Exception e) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "Invalid date format. Use YYYY-MM-DD"));
            }
        }

        // Search is strictly scoped to the logged-in user, even for admins (who can manage other files via the Reports page)
        Page<Document> documents = documentService.searchAndFilterDocuments(
                loggedInUser, query, category, type, uploadDate, page, size
        );
        return ResponseEntity.ok(documents);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getDocumentDetails(@PathVariable Long id, HttpSession session) {
        User loggedInUser = (User) session.getAttribute("user");
        if (loggedInUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Unauthorized"));
        }

        try {
            Document doc = documentService.getDocument(id, loggedInUser);
            return ResponseEntity.ok(doc);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateDocument(
            @PathVariable Long id,
            @RequestBody Map<String, Object> payload,
            HttpSession session) {

        User loggedInUser = (User) session.getAttribute("user");
        if (loggedInUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Unauthorized"));
        }

        try {
            String description = (String) payload.get("description");
            Long categoryId = null;
            if (payload.get("categoryId") != null) {
                categoryId = Long.valueOf(payload.get("categoryId").toString());
            }

            Document updated = documentService.updateDocumentDetails(id, description, categoryId, loggedInUser);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{id}/rename")
    public ResponseEntity<?> renameDocument(
            @PathVariable Long id,
            @RequestBody Map<String, String> payload,
            HttpSession session) {

        User loggedInUser = (User) session.getAttribute("user");
        if (loggedInUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Unauthorized"));
        }

        try {
            String newName = payload.get("newName");
            Document doc = documentService.renameDocument(id, newName, loggedInUser);
            return ResponseEntity.ok(doc);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteDocument(@PathVariable Long id, HttpSession session) {
        User loggedInUser = (User) session.getAttribute("user");
        if (loggedInUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Unauthorized"));
        }

        try {
            documentService.deleteDocument(id, loggedInUser);
            return ResponseEntity.ok(Map.of("message", "Document deleted successfully."));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<?> downloadDocument(
            @PathVariable Long id,
            @RequestParam(value = "inline", defaultValue = "false") boolean inline,
            HttpServletRequest request,
            HttpSession session) {
        User loggedInUser = (User) session.getAttribute("user");
        if (loggedInUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Unauthorized"));
        }

        try {
            Document doc = documentService.getDocument(id, loggedInUser);
            Resource resource = documentService.downloadDocument(id, loggedInUser);

            // Determine content type using original name since resource is a ByteArrayResource
            String contentType = request.getServletContext().getMimeType(doc.getOriginalName());

            if (contentType == null) {
                contentType = "application/octet-stream";
            }

            String dispositionType = inline ? "inline" : "attachment";
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, dispositionType + "; filename=\"" + doc.getOriginalName() + "\"")
                    .body(resource);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        }
    }
}
