package ru.itfb.symphony.service.migration.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import ru.itfb.symphony.common.mapper.BaseMapper;
import ru.itfb.symphony.service.migration.model.domain.MigrationTask;
import ru.itfb.symphony.service.migration.model.dto.MigrationTaskDto;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface MigrationTaskMapper extends BaseMapper<MigrationTask, MigrationTaskDto> {

    @Override
    MigrationTaskDto domainToDto(MigrationTask domain);

    @Override
    MigrationTask dtoToDomain(MigrationTaskDto dto);
}
