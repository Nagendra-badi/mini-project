package com.ddms.service;

import com.ddms.model.Category;
import com.ddms.repository.CategoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class CategoryService {

    @Autowired
    private CategoryRepository categoryRepository;

    public List<Category> getAllCategories() {
        return categoryRepository.findAll();
    }

    public Category createCategory(String name) throws Exception {
        String cleanName = name.trim();
        if (cleanName.isEmpty()) {
            throw new Exception("Category name cannot be empty.");
        }
        if (categoryRepository.existsByName(cleanName)) {
            throw new Exception("Category already exists.");
        }
        return categoryRepository.save(new Category(cleanName));
    }

    public void deleteCategory(Long id) throws Exception {
        if (!categoryRepository.existsById(id)) {
            throw new Exception("Category not found.");
        }
        categoryRepository.deleteById(id);
    }
}
