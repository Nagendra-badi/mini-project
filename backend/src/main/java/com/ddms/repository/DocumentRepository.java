package com.ddms.repository;

import com.ddms.model.Document;
import com.ddms.model.User;
import com.ddms.model.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface DocumentRepository extends JpaRepository<Document, Long>, JpaSpecificationExecutor<Document> {

    Page<Document> findByUploadedBy(User uploadedBy, Pageable pageable);

    List<Document> findByUploadedBy(User uploadedBy);

    @Query("SELECT d FROM Document d LEFT JOIN d.category c WHERE " +
           "(:userId IS NULL OR d.uploadedBy.id = :userId) AND " +
           "(:fileName IS NULL OR LOWER(d.originalName) LIKE LOWER(CONCAT('%', :fileName, '%'))) AND " +
           "(:categoryName IS NULL OR LOWER(c.name) = LOWER(:categoryName)) AND " +
           "(:fileType IS NULL OR LOWER(d.fileType) = LOWER(:fileType)) AND " +
           "(:startOfDay IS NULL OR (d.uploadDate >= :startOfDay AND d.uploadDate <= :endOfDay))")
    Page<Document> searchDocuments(
            @Param("userId") Long userId,
            @Param("fileName") String fileName,
            @Param("categoryName") String categoryName,
            @Param("fileType") String fileType,
            @Param("startOfDay") java.time.LocalDateTime startOfDay,
            @Param("endOfDay") java.time.LocalDateTime endOfDay,
            Pageable pageable
    );

    // Checks for duplicate files per user (same original name and file size)
    boolean existsByUploadedByAndOriginalNameAndFileSize(User user, String originalName, Long fileSize);

    // Summary statistics queries
    @Query("SELECT COUNT(d) FROM Document d")
    long countTotalDocuments();

    @Query("SELECT SUM(d.fileSize) FROM Document d")
    Long sumTotalStorageUsed();

    @Query("SELECT d.fileType, COUNT(d) FROM Document d GROUP BY d.fileType")
    List<Object[]> getFileTypeStatistics();

    @Query("SELECT d.category.name, COUNT(d) FROM Document d GROUP BY d.category.name")
    List<Object[]> getCategoryStatistics();

    List<Document> findTop5ByOrderByUploadDateDesc();
}
