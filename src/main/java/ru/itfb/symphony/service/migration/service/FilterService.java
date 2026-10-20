package ru.itfb.symphony.service.migration.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.itfb.symphony.service.migration.component.DataSourceRegistry;
import ru.itfb.symphony.service.migration.filter.Filter;
import ru.itfb.symphony.service.migration.schema.ExportEntity;
import ru.itfb.symphony.service.migration.schema.SchemaProcessorService;
import ru.itfb.symphony.service.migration.schema.TransportSchema;
import ru.itfb.symphony.service.migration.utills.QueryBuilderUtils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Service
@Slf4j
@RequiredArgsConstructor
public class FilterService {

    private final DataSourceRegistry dataSourceRegistry;

    public Map<ExportEntity, Set<String>> createMapByFilter(Filter filter, TransportSchema schema, String datasourceName) {

        Map<ExportEntity, Set<String>> filteredMap = new HashMap<>();
        ExportEntity exportEntity = schema.getExportEntity();

        String sql = QueryBuilderUtils.createQueryGetParentIdsByFilter(SchemaProcessorService.searchNameIdColumnInSchema(exportEntity),
                exportEntity.getPathToTable(),
                filter);

        Set<String> parentIds = new HashSet<>(dataSourceRegistry.getDataSource(datasourceName).queryForList(sql, String.class));

        filteredMap.put(exportEntity, parentIds);
        return filteredMap;
    }
}
