package com.delosreyes.inventario.modules.categories.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import com.delosreyes.inventario.modules.categories.dto.CategoryDtos;
import com.delosreyes.inventario.modules.categories.entity.Category;

@Mapper(componentModel = "spring")
public interface CategoryMapper {

    CategoryDtos.Response toResponse(Category category);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Category toEntity(CategoryDtos.Request request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntity(CategoryDtos.Request request, @MappingTarget Category category);
}