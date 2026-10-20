package ru.itfb.symphony.service.migration.mapper;

import org.springframework.jdbc.core.RowMapper;
import ru.itfb.symphony.service.migration.pojo.EntityFieldHolder;
import ru.itfb.symphony.service.migration.pojo.EntityHolder;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RowEntityHolderMapper implements RowMapper<EntityHolder> {
    @Override
    public EntityHolder mapRow(ResultSet rs, int rowNum) throws SQLException {
        EntityHolder holder = new EntityHolder();
        List<Map<String, EntityFieldHolder>> list = new ArrayList<>();
        ResultSetMetaData rsmd = rs.getMetaData();
        int count = rsmd.getColumnCount();

        for (int i = 1; i <= count; i++) {
            Map<String, EntityFieldHolder> map = new HashMap<>();
            String columnName = rsmd.getColumnName(i);
            String columnValue = rs.getString(i);
            EntityFieldHolder fieldHolder = new EntityFieldHolder();
            fieldHolder.setValue(columnValue);
            map.put(columnName, fieldHolder);
            list.add(map);
        }
        list.add(createZeroLevelHierarchyField());
        holder.setEntity(list);
        return holder;
    }

    public Map<String, EntityFieldHolder> createZeroLevelHierarchyField() {
        Map<String, EntityFieldHolder> map = new HashMap<>();
        EntityFieldHolder fieldHolder = new EntityFieldHolder();
        fieldHolder.setValue("0");
        map.put("level", fieldHolder);
        return map;
    }
}
