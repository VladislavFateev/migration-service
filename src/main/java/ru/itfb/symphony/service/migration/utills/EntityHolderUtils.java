package ru.itfb.symphony.service.migration.utills;

import lombok.extern.slf4j.Slf4j;
import ru.itfb.symphony.service.migration.pojo.EntityFieldHolder;
import ru.itfb.symphony.service.migration.pojo.EntityHolder;

import java.util.List;
import java.util.Map;

@Slf4j
public class EntityHolderUtils {

    public static Map<String, EntityFieldHolder> getLastField(List<Map<String, EntityFieldHolder>> entity) {
        return entity.get(entity.size() - 1);
    }

    public static Map<String, EntityFieldHolder> getFistField(List<Map<String, EntityFieldHolder>> entity) {
        return entity.get(0);
    }

    public static Boolean convertStringToBoolean(String value) {
        Boolean bool = null;
        if (value != null) {
            bool = value.equals("t");
        }
        return bool;
    }

    public static String getHierarchyLevel(EntityHolder entityHolder) {
        return getLastField(entityHolder.getEntity()).get("level").getValue();
    }

}
