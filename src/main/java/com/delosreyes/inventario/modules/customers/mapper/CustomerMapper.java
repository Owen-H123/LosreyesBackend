package com.delosreyes.inventario.modules.customers.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import com.delosreyes.inventario.modules.customers.dto.CustomerDtos;
import com.delosreyes.inventario.modules.customers.entity.Customer;

@Mapper(componentModel = "spring")
public interface CustomerMapper {

    CustomerDtos.Response toResponse(Customer customer);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "active", constant = "true")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Customer toEntity(CustomerDtos.Request request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "active", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntity(CustomerDtos.Request request, @MappingTarget Customer customer);
}