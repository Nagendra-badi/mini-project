package com.ddms.service;

import com.ddms.model.User;
import com.ddms.repository.UserRepository;
import com.ddms.repository.DocumentRepository;
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
    }

    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }
}
