package com.delosreyes.inventario.modules.customers.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.delosreyes.inventario.modules.customers.entity.Customer;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {

    Optional<Customer> findByDocumentNumber(String documentNumber);

    boolean existsByDocumentNumber(String documentNumber);

    @Query("""
        SELECT c FROM Customer c
        WHERE LOWER(c.name) LIKE LOWER(CONCAT('%', :q, '%'))
           OR c.documentNumber LIKE CONCAT('%', :q, '%')
           OR LOWER(c.email) LIKE LOWER(CONCAT('%', :q, '%'))
        ORDER BY c.name
        """)
    Page<Customer> search(@Param("q") String query, Pageable pageable);

    Page<Customer> findByActiveTrueOrderByNameAsc(Pageable pageable);
}