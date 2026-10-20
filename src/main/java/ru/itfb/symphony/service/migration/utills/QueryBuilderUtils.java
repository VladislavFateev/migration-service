package ru.itfb.symphony.service.migration.utills;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.itfb.symphony.service.migration.enums.FieldType;
import ru.itfb.symphony.service.migration.enums.operators.Operator;
import ru.itfb.symphony.service.migration.exception.ExportException;
import ru.itfb.symphony.service.migration.filter.Filter;
import ru.itfb.symphony.service.migration.schema.ExportEntity;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class QueryBuilderUtils {

    public static String createQueryFindAll(ExportEntity exportEntity, String pathDataBaseTable) {

        return createQuerySelectAllColumnsBySchema(exportEntity) +
                " from " + pathDataBaseTable + " as t ";
    }

    public static String createQueryGetTransferMapByNameColumn(String entityColumnPath) {
        return " select name_column, old_id, new_id " +
                " from migration_schema.transfer_table " +
                " where name_column = '" + entityColumnPath + "' ";
    }

    public static String createQuerySelectAllIds(String columnIdName, String pathDataBaseTable) {
        return "select " + columnIdName + " from " + pathDataBaseTable;
    }

    public static String createQuerySelectAllByUniqueColumn(String columnName, String pathDataBaseTable) {
        return "select distinct " + columnName + " from " + pathDataBaseTable;
    }

    public static String createQuerySelectPositionColumn(String tableName, String columnName) {
        return "select ordinal_position - 1 from information_schema.columns where  table_name = '" + tableName + "' and column_name = '" + columnName + "'";
    }

    public static String createQueryRecursiveHierarchyEntityHolder(String pathDataBaseTable, String id, String parentColumn, Integer limit, Integer offset) {
        String sql =
                "with recursive t as ( " +
                        "   select * " +
                        "    from " + pathDataBaseTable + " as org " +
                        "), cte as " +
                        "                   ( " +
                        "                       select " + id + ", " + parentColumn + " as pId, 0 as level " +
                        "                       from t " +
                        "                       union all " +
                        "                       select t." + id + ", t." + parentColumn + ", level + 1 " +
                        "                       from cte " +
                        "                                join t on t." + parentColumn +" = cte." + id + " and (t." + id + " <> t." + parentColumn + " or t." + parentColumn + " is null) " +
                        "                   ) " +
                        "select " + id + ", max(level) as lvl " +
                        "from cte " +
                        "group by " + id + " " +
                        "order by lvl " +
                        "limit " + limit +
                        "    offset " + offset;
        return sql;
    }

    public static String createQueryFindIdById(String id, String pathDataBaseTable) {
        return "select t." + id + " from " + pathDataBaseTable + " as t " + " where t." + id + " = " + id;
    }

    public static String createQueryInsertInto(LinkedHashMap<String, FieldType> columnsMap,
                                               String pathDataBaseTable,
                                               List<String> jsonbColumnNames,
                                               List<String> uuidColumnNames,
                                               List<String> byteaColumnNames
                                               ) {
        int size = columnsMap.size();
        int lastColumnIndex = size - 1;
        StringBuilder columns = new StringBuilder();
        StringBuilder values = new StringBuilder();
        List<String> columnsName = new ArrayList<>(columnsMap.keySet());
        
        for (int columnIndex = 0; columnIndex < size; columnIndex++) {
            if (jsonbColumnNames.contains(columnsName.get(columnIndex))) {
                values.append("to_jsonb(?::jsonb)");
            }
            else if (uuidColumnNames.contains(columnsName.get(columnIndex))) {
                values.append("?::uuid");
            }
            else if (byteaColumnNames.contains(columnsName.get(columnIndex))) {
                values.append("?::bytea");
            }
            else {
                values.append("?");
            }
            columns.append(columnsName.get(columnIndex));
            if (columnIndex != lastColumnIndex) {
                values.append(",");
                columns.append(",");
            }
        }
        return "insert into " + pathDataBaseTable + " (" + columns + ") values (" + values + ")";
    }

    public static String createQueryUpdate(LinkedHashMap<String, FieldType> columnsMap, String pathDataBaseTable, String nameIdColumn, List<String> jsonbColumnNames, List<String> uuidColumnNames) {
        int size = columnsMap.size();
        int lastColumnIndex = size - 1;
        StringBuilder columns = new StringBuilder();
        StringBuilder uuidType = new StringBuilder();
        List<String> columnsName = new ArrayList<>(columnsMap.keySet());

        if (uuidColumnNames.contains(nameIdColumn)) {
            uuidType.append("::uuid");
        }
        
        for (int columnIndex = 1; columnIndex < size; columnIndex++) {
            columns.append(columnsName.get(columnIndex));
            if (jsonbColumnNames.contains(columnsName.get(columnIndex))) {
                columns.append(" = to_jsonb(?::jsonb)");
            }
            else if (uuidColumnNames.contains(columnsName.get(columnIndex))) {
                columns.append(" = ?::uuid");
            } else {
                columns.append(" = ?");
            }
            if (columnIndex != lastColumnIndex) {
                columns.append(",");
            }
        }
        return "update " + pathDataBaseTable + " set " + columns + " where " + nameIdColumn + " = ?" + uuidType;
    }

    public static String createQuerySelectNextSequence(ExportEntity exportEntity) {
        String schemaName = exportEntity.getPathToTable().split("\\.")[0];
        return "select nextval('" + schemaName + "." + exportEntity.getSequence() + "')";
    }

    public static String createQueryUpdateSequence(ExportEntity exportEntity, Integer sequenceIndex) {
        String schemaName = exportEntity.getPathToTable().split("\\.")[0];
        return "select setval('" + schemaName + "." + exportEntity.getSequence() + "', " + sequenceIndex + ", true)";
    }

    public static String createQuerySelectLastId(String pathDataBase, String columnIdName, FieldType type) {
        if (type.equals(FieldType.STRING_TYPE)) {
            return "select max(" + columnIdName + "::integer) from " + pathDataBase;
        }
        return "select max(" + columnIdName + ") from " + pathDataBase;
    }

    public static String createQueryCreateTransferTable() {
        return " create table if not exists transfer_table " +
                "(" +
                "    name_column varchar, " +
                "    old_id varchar, " +
                "    new_id varchar)";
    }

    public static String createQueryDropTransferTable() {
        return "drop table transfer_table";
    }

    public static String createQuerySetTransferMap(String nameColumn, String oldIdColumn, String newIdColumn) {
        return " insert into migration_schema.transfer_table(name_column, old_id, new_id) " +
                " values ('" + nameColumn + "', '" + oldIdColumn + "', '" + newIdColumn + "') ";
    }

    private static String createQuerySelectAllColumnsBySchema(ExportEntity exportEntity) {
        List<String> columnsName = new ArrayList<>(findNameColumnsByEntity(exportEntity));
        StringBuilder select = new StringBuilder();

        for (int columnIndex = 0; columnIndex < columnsName.size(); columnIndex++) {
            select.append(columnsName.get(columnIndex));
            if (columnIndex != columnsName.size() - 1) {
                select.append(", ");
            }
        }
        return "select " + select;
    }

    private static List<String> findNameColumnsByEntity(ExportEntity exportEntity) {
        return exportEntity.getAttributes().keySet().stream().toList();
    }

    public static String createQueryWhereEnumerationIds(String columnIdName, List<String> idList, FieldType type) {
        int lastIdIndex = idList.size() - 1;
        StringBuilder ids = new StringBuilder();
        if (lastIdIndex < 0) {
            throw new ExportException();
        }

        for (int idIndex = 0; idIndex < idList.size(); idIndex++) {
            if (type.equals(FieldType.STRING_TYPE)) {
                ids.append("'").append(idList.get(idIndex)).append("'");
            } else {
                ids.append(idList.get(idIndex));
            }
            if (idIndex != lastIdIndex) {
                ids.append(",");
            }
        }
        return " where " + columnIdName + " in (" + ids + ") ";
    }

    public static String createQueryGetParentIdsByColumnName(String parentColumnName,
                                                             String pathTable,
                                                             String columnIdName,
                                                             List<String> idList,
                                                             FieldType type) {

        return " select " + parentColumnName + " from " + pathTable + createQueryWhereEnumerationIds(columnIdName, idList, type);
    }

    public static String createQueryGetParentIdsByFilter(String parentColumnName,
                                                         String pathTable,
                                                         Filter filter) {
        String where = " where " + createQueryFilter(filter);
        String sql = " select " + parentColumnName + " from " + pathTable;
        return sql + where;
    }

    private static String createQueryFilter(Filter filter) {
        List<Operator> operators = filter.getOperator();
        StringBuilder result = new StringBuilder();
        Integer lastIndex = operators.size() - 1;
        for (int operatorIndex = 0; operatorIndex < operators.size(); operatorIndex++) {

            Operator operator = operators.get(operatorIndex);
            String columnName = filter.getColumnName().get(operatorIndex);
            String value = filter.getValues().get(operatorIndex);
            switch (operator) {
                case IN:
                    List<String> idList = filter.getValues();
                    int lastIdIndex = idList.size() - 1;
                    StringBuilder values = new StringBuilder();
                    for (int idIndex = 0; idIndex < idList.size(); idIndex++) {
                        values.append(idList.get(idIndex));
                        if (idIndex != lastIdIndex) {
                            values.append(",");
                        }
                    }
                    result = new StringBuilder(columnName + " in (" + values + ") ");
                    break;
                case LIKE:
                    result.append(columnName).append(" ilike ").append(value);
                    break;
                case GREATER_THAN:
                    result.append(columnName).append(" > ").append(value);
                    break;
                case LESS_THAN:
                    result.append(columnName).append(" < ").append(value);
                    break;
                case GREATER_THAN_OR_EQUAL:
                    result.append(columnName).append(" >= ").append(value);
                    break;
                case LESS_THAN_OR_EQUAL:
                    result.append(columnName).append(" <= ").append(value);
                    break;
                case EQUALITY:
                    result.append(columnName).append(" = ").append(value);
                    break;
                case BETWEEN:
                    result.append(columnName).append(" between ").append(filter.getValues().get(operatorIndex)).append(" and ").append(filter.getValues().get(operatorIndex + 1));
            }
            if (filter.getLogicalOperator() != null && operatorIndex < lastIndex) {
                result.append(" ").append(filter.getLogicalOperator().get(operatorIndex)).append(" ");
            }
        }
        return result.toString();
    }
}
