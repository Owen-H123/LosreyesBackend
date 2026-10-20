package com.delosreyes.inventario.modules.vehicles.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import com.delosreyes.inventario.modules.vehicles.dto.VehicleDtos;
import com.delosreyes.inventario.modules.vehicles.entity.Vehicle;

@Mapper(componentModel = "spring")
public interface VehicleMapper {

    @Mapping(target = "displayName", expression =
        "java(vehicle.getBrand() + \" \" + vehicle.getModel() + \" \" + vehicle.getYear())")
    VehicleDtos.Response toResponse(Vehicle vehicle);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "products", ignore = true)
    Vehicle toEntity(VehicleDtos.Request request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "products", ignore = true)
    void updateEntity(VehicleDtos.Request request, @MappingTarget Vehicle vehicle);
}