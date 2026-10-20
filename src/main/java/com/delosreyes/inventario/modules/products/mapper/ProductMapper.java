package com.delosreyes.inventario.modules.products.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import com.delosreyes.inventario.modules.products.dto.ProductDtos;
import com.delosreyes.inventario.modules.products.entity.Product;
import com.delosreyes.inventario.modules.vehicles.mapper.VehicleMapper;

/**
 * ProductMapper — Conversión Product entity ↔ DTOs.
 *
 * NOTA:
 *   profitMargin y lowStock son campos calculados que MapStruct no puede resolver
 *   automáticamente. Se calculan en ProductService después del mapeo.
 *
 *   vehicles usa VehicleMapper para convertir el Set<Vehicle> en List<VehicleDtos.Response>.
 */
@Mapper(componentModel = "spring", uses = {VehicleMapper.class})
public interface ProductMapper {

    @Mapping(target = "categoryId", source = "category.id")
    @Mapping(target = "categoryName", source = "category.name")
    @Mapping(target = "lowStock", expression = "java(product.isLowStock())")
    @Mapping(target = "profitMargin", ignore = true)
    @Mapping(target = "vehicles", source = "vehicles")
    ProductDtos.ProductResponse toResponse(Product product);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "category", ignore = true)
    @Mapping(target = "vehicles", ignore = true)
    @Mapping(target = "status", constant = "ACTIVE")
    @Mapping(target = "deleted", constant = "false")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Product toEntity(ProductDtos.CreateRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "code", ignore = true)
    @Mapping(target = "stock", ignore = true)
    @Mapping(target = "category", ignore = true)
    @Mapping(target = "vehicles", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntity(ProductDtos.UpdateRequest request, @MappingTarget Product product);
}