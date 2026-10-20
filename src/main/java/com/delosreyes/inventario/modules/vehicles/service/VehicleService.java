package com.delosreyes.inventario.modules.vehicles.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.delosreyes.inventario.modules.vehicles.dto.VehicleDtos;
import com.delosreyes.inventario.modules.vehicles.entity.Vehicle;
import com.delosreyes.inventario.modules.vehicles.mapper.VehicleMapper;
import com.delosreyes.inventario.modules.vehicles.repository.VehicleRepository;
import com.delosreyes.inventario.shared.exceptions.DuplicateResourceException;
import com.delosreyes.inventario.shared.exceptions.ResourceNotFoundException;

/**
 * VehicleService — Gestión de vehículos compatibles.
 *
 * QUÉ HACE:
 *   CRUD de vehículos. Búsquedas por marca, modelo y año.
 *   Autocomplete para el frontend al registrar compatibilidad de productos.
 */
@Service
@Transactional
public class VehicleService {

    private final VehicleRepository vehicleRepository;
    private final VehicleMapper vehicleMapper;

    public VehicleService(VehicleRepository vehicleRepository, VehicleMapper vehicleMapper) {
        this.vehicleRepository = vehicleRepository;
        this.vehicleMapper = vehicleMapper;
    }

    @Cacheable("vehicles")
    @Transactional(readOnly = true)
    public List<VehicleDtos.Response> findAll() {
        return vehicleRepository.findAll()
            .stream().map(vehicleMapper::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<VehicleDtos.Response> search(String query) {
        return vehicleRepository.search(query)
            .stream().map(vehicleMapper::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<String> findBrands() {
        return vehicleRepository.findDistinctBrands();
    }

    @Transactional(readOnly = true)
    public List<String> findModelsByBrand(String brand) {
        return vehicleRepository.findModelsByBrand(brand);
    }

    @Transactional(readOnly = true)
    public List<VehicleDtos.Response> findByBrand(String brand) {
        return vehicleRepository.findByBrandOrderByModelAscYearAsc(brand)
            .stream().map(vehicleMapper::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<VehicleDtos.Response> findByProduct(Long productId) {
        return vehicleRepository.findByProductId(productId)
            .stream().map(vehicleMapper::toResponse).collect(Collectors.toList());
    }

    @CacheEvict(value = "vehicles", allEntries = true)
    public VehicleDtos.Response create(VehicleDtos.Request request) {
        vehicleRepository.findByBrandAndModelAndYearAndEngine(
            request.getBrand(), request.getModel(), request.getYear(), request.getEngine()
        ).ifPresent(v -> {
            throw new DuplicateResourceException(
                "Vehículo", "brand/model/year/engine",
                request.getBrand() + " " + request.getModel() + " " + request.getYear());
        });

        Vehicle saved = vehicleRepository.save(vehicleMapper.toEntity(request));
        return vehicleMapper.toResponse(saved);
    }

    @CacheEvict(value = "vehicles", allEntries = true)
    public VehicleDtos.Response update(Long id, VehicleDtos.Request request) {
        Vehicle vehicle = findEntityById(id);
        vehicleMapper.updateEntity(request, vehicle);
        return vehicleMapper.toResponse(vehicleRepository.save(vehicle));
    }

    @CacheEvict(value = "vehicles", allEntries = true)
    public void delete(Long id) {
        vehicleRepository.delete(findEntityById(id));
    }

    public Vehicle findEntityById(Long id) {
        return vehicleRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Vehículo", "id", id));
    }
}