package com.ddms.service;

import com.ddms.model.Category;
import com.ddms.model.Document;
import com.ddms.model.User;
import com.ddms.repository.CategoryRepository;
import com.ddms.repository.DocumentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class DocumentService {

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private StorageService storageService;

    @Autowired
    private ActivityLogService activityLogService;

    public Document uploadDocument(MultipartFile file, String description, Long categoryId, User user) throws Exception {
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isEmpty()) {
            throw new Exception("Invalid file name.");
        }

        // 1. Check for Duplicate File (same original name and file size for this user)
        if (documentRepository.existsByUploadedByAndOriginalNameAndFileSize(user, originalFilename, file.getSize())) {
            throw new Exception("Duplicate file detected: You have already uploaded this file.");
        }

        // 2. Fetch Category if provided
        Category category = null;
        if (categoryId != null) {
            category = categoryRepository.findById(categoryId)
                    .orElseThrow(() -> new Exception("Selected category not found."));
        }

        // 3. Generate a Unique Filename for Disk Storage to prevent overwrites
        String fileExtension = getFileExtension(originalFilename);
        String uniqueFileName = UUID.randomUUID().toString() + (fileExtension.isEmpty() ? "" : "." + fileExtension);

        // 4. Create Document Record
        Document doc = new Document(
                uniqueFileName,
                originalFilename,
                file.getSize(),
                fileExtension.toUpperCase(),
                LocalDateTime.now(),
                user,
                "", // Placeholder, set after generating ID
                category,
                description
        );
        doc.setFileData(file.getBytes());

        Document savedDoc = documentRepository.save(doc);
        savedDoc.setS3Url("/api/documents/" + savedDoc.getId() + "/download");
        savedDoc = documentRepository.save(savedDoc);

        activityLogService.log(user.getUsername(), "UPLOAD", "Uploaded document: " + originalFilename + " (" + file.getSize() + " bytes)");
        return savedDoc;
    }

    public Page<Document> searchAndFilterDocuments(
            User user, String query, String categoryName, String fileType, LocalDate uploadDate, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("uploadDate").descending());
        
        Specification<Document> spec = Specification.where(null);
        
        if (user != null) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("uploadedBy").get("id"), user.getId()));
        }
        
        if (query != null && !query.trim().isEmpty()) {
            String qStr = "%" + query.trim().toLowerCase() + "%";
            spec = spec.and((root, q, cb) -> cb.like(cb.lower(root.get("originalName")), qStr));
        }
        
        if (categoryName != null && !categoryName.trim().isEmpty()) {
            spec = spec.and((root, q, cb) -> cb.equal(
                cb.lower(root.join("category", jakarta.persistence.criteria.JoinType.LEFT).get("name")),
                categoryName.trim().toLowerCase()
            ));
        }
        
        if (fileType != null && !fileType.trim().isEmpty()) {
            spec = spec.and((root, q, cb) -> cb.equal(cb.lower(root.get("fileType")), fileType.trim().toLowerCase()));
        }
        
        if (uploadDate != null) {
            java.time.LocalDateTime start = uploadDate.atStartOfDay();
            java.time.LocalDateTime end = uploadDate.atTime(23, 59, 59, 999999999);
            spec = spec.and((root, q, cb) -> cb.between(root.get("uploadDate"), start, end));
        }
        
        return documentRepository.findAll(spec, pageable);
    }

    public Document updateDocumentDetails(Long docId, String description, Long categoryId, User user) throws Exception {
        Document doc = documentRepository.findById(docId)
                .orElseThrow(() -> new Exception("Document not found."));

        // Access check
        if (!"ADMIN".equalsIgnoreCase(user.getRole()) && !doc.getUploadedBy().getId().equals(user.getId())) {
            throw new Exception("Access denied: You cannot edit this document.");
        }

        if (categoryId != null) {
            Category category = categoryRepository.findById(categoryId)
                    .orElseThrow(() -> new Exception("Selected category not found."));
            doc.setCategory(category);
        } else {
            doc.setCategory(null);
        }

        doc.setDescription(description);
        Document updated = documentRepository.save(doc);
        activityLogService.log(user.getUsername(), "UPDATE", "Updated details of document: " + doc.getOriginalName());
        return updated;
    }

    public Document renameDocument(Long docId, String newOriginalName, User user) throws Exception {
        Document doc = documentRepository.findById(docId)
                .orElseThrow(() -> new Exception("Document not found."));

        // Access check
        if (!"ADMIN".equalsIgnoreCase(user.getRole()) && !doc.getUploadedBy().getId().equals(user.getId())) {
            throw new Exception("Access denied: You cannot rename this document.");
        }

        String cleanedName = newOriginalName.trim();
        if (cleanedName.isEmpty()) {
            throw new Exception("New name cannot be empty.");
        }

        // Maintain original extension if missing in input
        String oldExt = getFileExtension(doc.getOriginalName());
        String newExt = getFileExtension(cleanedName);
        if (newExt.isEmpty() && !oldExt.isEmpty()) {
            cleanedName += "." + oldExt;
        }

        String oldOriginalName = doc.getOriginalName();
        doc.setOriginalName(cleanedName);
        Document updated = documentRepository.save(doc);
        activityLogService.log(user.getUsername(), "RENAME", "Renamed document from '" + oldOriginalName + "' to '" + cleanedName + "'");
        return updated;
    }

    public void deleteDocument(Long docId, User user) throws Exception {
        Document doc = documentRepository.findById(docId)
                .orElseThrow(() -> new Exception("Document not found."));

        // Access check
        if (!"ADMIN".equalsIgnoreCase(user.getRole()) && !doc.getUploadedBy().getId().equals(user.getId())) {
            throw new Exception("Access denied: You cannot delete this document.");
        }

        // Delete from DB
        documentRepository.delete(doc);
        activityLogService.log(user.getUsername(), "DELETE", "Deleted document: " + doc.getOriginalName());
    }

    public org.springframework.core.io.Resource downloadDocument(Long docId, User user) throws Exception {
        Document doc = documentRepository.findById(docId)
                .orElseThrow(() -> new Exception("Document not found."));

        // Access check
        if (!"ADMIN".equalsIgnoreCase(user.getRole()) && !doc.getUploadedBy().getId().equals(user.getId())) {
            throw new Exception("Access denied: You cannot access this document.");
        }

        org.springframework.core.io.Resource resource = new org.springframework.core.io.ByteArrayResource(doc.getFileData());
        activityLogService.log(user.getUsername(), "DOWNLOAD", "Downloaded document: " + doc.getOriginalName());
        return resource;
    }

    public Document getDocument(Long docId, User user) throws Exception {
        Document doc = documentRepository.findById(docId)
                .orElseThrow(() -> new Exception("Document not found."));
        if (!"ADMIN".equalsIgnoreCase(user.getRole()) && !doc.getUploadedBy().getId().equals(user.getId())) {
            throw new Exception("Access denied.");
        }
        return doc;
    }

    // Dashboard and Global Admin Statistics
    public Map<String, Object> getAdminStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalDocuments", documentRepository.countTotalDocuments());
        Long storageUsed = documentRepository.sumTotalStorageUsed();
        stats.put("storageUsed", storageUsed != null ? storageUsed : 0L);

        // Fetch category statistics
        List<Object[]> categoryResults = documentRepository.getCategoryStatistics();
        Map<String, Long> categoryStats = new HashMap<>();
        for (Object[] row : categoryResults) {
            String catName = row[0] != null ? (String) row[0] : "Uncategorized";
            categoryStats.put(catName, (Long) row[1]);
        }
        stats.put("categoryStats", categoryStats);

        // Fetch file type statistics
        List<Object[]> typeResults = documentRepository.getFileTypeStatistics();
        Map<String, Long> typeStats = new HashMap<>();
        for (Object[] row : typeResults) {
            String type = row[0] != null ? (String) row[0] : "UNKNOWN";
            typeStats.put(type, (Long) row[1]);
        }
        stats.put("fileTypeStats", typeStats);

        // Fetch recent uploads
        List<Document> recentUploads = documentRepository.findTop5ByOrderByUploadDateDesc();
        List<Map<String, Object>> uploadsList = new ArrayList<>();
        for (Document d : recentUploads) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", d.getId());
            item.put("originalName", d.getOriginalName());
            item.put("uploadedBy", d.getUploadedBy().getUsername());
            item.put("uploadDate", d.getUploadDate());
            item.put("fileSize", d.getFileSize());
            uploadsList.add(item);
        }
        stats.put("recentUploads", uploadsList);

        return stats;
    }

    private String getFileExtension(String filename) {
        int lastIndex = filename.lastIndexOf('.');
        if (lastIndex == -1 || lastIndex == filename.length() - 1) {
            return "";
        }
        return filename.substring(lastIndex + 1).toLowerCase();
    }
}
