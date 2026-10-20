package com.delosreyes.inventario.modules.categories.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.delosreyes.inventario.modules.categories.entity.Category;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    Optional<Category> findByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCase(String name);

    List<Category> findByActiveTrueOrderByNameAsc();
}