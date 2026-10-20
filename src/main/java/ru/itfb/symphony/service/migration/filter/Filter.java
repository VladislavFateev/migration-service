package ru.itfb.symphony.service.migration.filter;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ru.itfb.symphony.service.migration.enums.operators.LogicalOperator;
import ru.itfb.symphony.service.migration.enums.operators.Operator;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Slf4j
@Data
public class Filter {

    private List<String> columnName;
    private List<String> values;
    private List<Operator> operator;
    private List<LogicalOperator> logicalOperator;
}
