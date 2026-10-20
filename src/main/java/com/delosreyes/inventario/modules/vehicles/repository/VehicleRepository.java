package com.delosreyes.inventario.modules.vehicles.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.delosreyes.inventario.modules.vehicles.entity.Vehicle;

@Repository
public interface VehicleRepository extends JpaRepository<Vehicle, Long> {

    List<Vehicle> findByBrandOrderByModelAscYearAsc(String brand);

    List<Vehicle> findByBrandAndModelOrderByYearAsc(String brand, String model);

    Optional<Vehicle> findByBrandAndModelAndYearAndEngine(
        String brand, String model, Integer year, String engine);

    @Query("SELECT DISTINCT v.brand FROM Vehicle v ORDER BY v.brand")
    List<String> findDistinctBrands();

    @Query("SELECT DISTINCT v.model FROM Vehicle v WHERE v.brand = :brand ORDER BY v.model")
    List<String> findModelsByBrand(@Param("brand") String brand);

    @Query("""
        SELECT v FROM Vehicle v
        WHERE LOWER(v.brand) LIKE LOWER(CONCAT('%', :query, '%'))
           OR LOWER(v.model) LIKE LOWER(CONCAT('%', :query, '%'))
        ORDER BY v.brand, v.model, v.year
        """)
    List<Vehicle> search(@Param("query") String query);

    @Query("""
        SELECT v FROM Vehicle v
        JOIN v.products p
        WHERE p.id = :productId
        """)
    List<Vehicle> findByProductId(@Param("productId") Long productId);
}