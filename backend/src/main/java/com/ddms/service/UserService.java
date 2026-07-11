package com.ddms.service;

import com.ddms.model.User;
import com.ddms.repository.UserRepository;
import com.ddms.repository.DocumentRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DocumentRepository documentRepository;

    @PersistenceContext
    private EntityManager entityManager;

    public User register(User user) throws Exception {
        if (userRepository.existsByUsername(user.getUsername())) {
            throw new Exception("Username already exists.");
        }
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new Exception("Email already exists.");
        }
        
        // In a real application, hash the password. 
        // For simplicity, we store it as plain text or simple hash.
        return userRepository.save(user);
    }

    public User login(String username, String password) throws Exception {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new Exception("User not found."));
        if (!user.getPassword().equals(password)) {
            throw new Exception("Invalid password.");
        }
        return user;
    }

    public User changePassword(Long userId, String oldPassword, String newPassword) throws Exception {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new Exception("User not found."));
        if (!user.getPassword().equals(oldPassword)) {
            throw new Exception("Current password does not match.");
        }
        user.setPassword(newPassword);
        return userRepository.save(user);
    }

    public User updateProfile(Long userId, String fullName, String phone, String email) throws Exception {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new Exception("User not found."));
        
        // Email unique check if updated
        if (!user.getEmail().equalsIgnoreCase(email) && userRepository.existsByEmail(email)) {
            throw new Exception("Email already in use.");
        }
        
        user.setFullName(fullName);
        user.setPhone(phone);
        user.setEmail(email);
        return userRepository.save(user);
    }

    public User forgotPassword(String username, String email, String newPassword) throws Exception {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new Exception("Username not found."));
        
        if (!user.getEmail().equalsIgnoreCase(email)) {
            throw new Exception("Email does not match this username.");
        }
        
        user.setPassword(newPassword);
        return userRepository.save(user);
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @Transactional
    public void deleteUser(Long id) throws Exception {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new Exception("User not found."));
        if ("ADMIN".equalsIgnoreCase(user.getRole())) {
            throw new Exception("Cannot delete an Admin account.");
        }
        // Cascade delete all documents uploaded by this user first to avoid Foreign Key Violations
        List<com.ddms.model.Document> documents = documentRepository.findByUploadedBy(user);
        documentRepository.deleteAll(documents);
        
        userRepository.delete(user);
        userRepository.flush(); // Synchronize deletion immediately
        
        // Re-index remaining users sequentially (option 2)
        reindexUserIds();
    }

    @Transactional
    public void reindexUserIds() {
        // Fetch all remaining users sorted by their current ID ascending
        List<User> users = userRepository.findAll(org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.ASC, "id"));
        
        // Detach all entities from the persistence context/Hibernate session.
        // This is critical because native SQL updates modifying primary keys (id)
        // will throw AssertionFailure if the entities remain attached during transaction flush/commit.
        entityManager.clear();
        
        long newId = 1;
        for (User u : users) {
            long oldId = u.getId();
            if (oldId != newId) {
                // Fetch document IDs belonging to this user
                List<?> docIdsRaw = entityManager.createNativeQuery("SELECT id FROM documents WHERE user_id = :oldId")
                        .setParameter("oldId", oldId)
                        .getResultList();
                
                List<Long> docIds = new java.util.ArrayList<>();
                for (Object obj : docIdsRaw) {
                    if (obj instanceof Number) {
                        docIds.add(((Number) obj).longValue());
                    }
                }

                if (!docIds.isEmpty()) {
                    // 1. Temporarily unlink documents from old user ID to prevent FK violation during ID migration
                    entityManager.createNativeQuery("UPDATE documents SET user_id = NULL WHERE user_id = :oldId")
                            .setParameter("oldId", oldId)
                            .executeUpdate();
                }

                // 2. Update user ID in the users table
                entityManager.createNativeQuery("UPDATE users SET id = :newId WHERE id = :oldId")
                        .setParameter("newId", newId)
                        .setParameter("oldId", oldId)
                        .executeUpdate();

                if (!docIds.isEmpty()) {
                    // 3. Re-link documents to the new user ID
                    entityManager.createNativeQuery("UPDATE documents SET user_id = :newId WHERE id IN (:docIds)")
                            .setParameter("newId", newId)
                            .setParameter("docIds", docIds)
                            .executeUpdate();
                }
            }
            newId++;
        }
        
        // 3. Reset the primary key auto-increment sequence
        try {
            entityManager.createNativeQuery("SELECT setval(pg_get_serial_sequence('users', 'id'), :nextId, false)")
                    .setParameter("nextId", newId)
                    .getResultList();
        } catch (Exception e) {
            // Fallback for in-memory databases like H2
            try {
                entityManager.createNativeQuery("ALTER TABLE users ALTER COLUMN id RESTART WITH " + newId)
                        .executeUpdate();
            } catch (Exception ex) {
                System.err.println("Could not reset user ID sequence: " + ex.getMessage());
            }
        }
    }

    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    public User findByUsername(String username) {
        return userRepository.findByUsername(username).orElse(null);
    }
}
