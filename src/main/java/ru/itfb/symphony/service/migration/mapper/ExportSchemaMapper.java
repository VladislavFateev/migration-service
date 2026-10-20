package ru.itfb.symphony.service.migration.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import ru.itfb.symphony.common.mapper.BaseMapper;
import ru.itfb.symphony.service.migration.model.domain.Schema;
import ru.itfb.symphony.service.migration.model.dto.ExportSchemaDto;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface ExportSchemaMapper extends BaseMapper<Schema, ExportSchemaDto> {

    @Override
    ExportSchemaDto domainToDto(Schema domain);

    @Override
    Schema dtoToDomain(ExportSchemaDto exportSchemaDto);
}
