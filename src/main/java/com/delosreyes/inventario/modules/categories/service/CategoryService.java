package com.delosreyes.inventario.modules.categories.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.delosreyes.inventario.modules.categories.dto.CategoryDtos;
import com.delosreyes.inventario.modules.categories.entity.Category;
import com.delosreyes.inventario.modules.categories.mapper.CategoryMapper;
import com.delosreyes.inventario.modules.categories.repository.CategoryRepository;
import com.delosreyes.inventario.shared.exceptions.DuplicateResourceException;
import com.delosreyes.inventario.shared.exceptions.ResourceNotFoundException;

/**
 * CategoryService — Gestión de categorías de productos.
 *
 * CACHÉ:
 *   Las categorías son datos estáticos que cambian poco.
 *   Se cachean en memoria para evitar consultas repetitivas a BD.
 *   El caché se invalida al crear, actualizar o eliminar.
 */
@Service
@Transactional
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;

    public CategoryService(CategoryRepository categoryRepository, CategoryMapper categoryMapper) {
        this.categoryRepository = categoryRepository;
        this.categoryMapper = categoryMapper;
    }

    @Cacheable("categories")
    @Transactional(readOnly = true)
    public List<CategoryDtos.Response> findAll() {
        return categoryRepository.findAll()
            .stream()
            .map(categoryMapper::toResponse)
            .collect(Collectors.toList());
    }

    @Cacheable(value = "categories", key = "#id")
    @Transactional(readOnly = true)
    public CategoryDtos.Response findById(Long id) {
        return categoryMapper.toResponse(findEntityById(id));
    }

    @Transactional(readOnly = true)
    public List<CategoryDtos.Response> findActive() {
        return categoryRepository.findByActiveTrueOrderByNameAsc()
            .stream()
            .map(categoryMapper::toResponse)
            .collect(Collectors.toList());
    }

    @CacheEvict(value = "categories", allEntries = true)
    public CategoryDtos.Response create(CategoryDtos.Request request) {
        if (categoryRepository.existsByNameIgnoreCase(request.getName())) {
            throw new DuplicateResourceException("Categoría", "nombre", request.getName());
        }
        Category saved = categoryRepository.save(categoryMapper.toEntity(request));
        return categoryMapper.toResponse(saved);
    }

    @CacheEvict(value = "categories", allEntries = true)
    public CategoryDtos.Response update(Long id, CategoryDtos.Request request) {
        Category category = findEntityById(id);

        categoryRepository.findByNameIgnoreCase(request.getName()).ifPresent(existing -> {
            if (!existing.getId().equals(id)) {
                throw new DuplicateResourceException("Categoría", "nombre", request.getName());
            }
        });

        categoryMapper.updateEntity(request, category);
        return categoryMapper.toResponse(categoryRepository.save(category));
    }

    @CacheEvict(value = "categories", allEntries = true)
    public void delete(Long id) {
        Category category = findEntityById(id);
        category.setActive(false);
        categoryRepository.save(category);
    }

    public Category findEntityById(Long id) {
        return categoryRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Categoría", "id", id));
    }
}