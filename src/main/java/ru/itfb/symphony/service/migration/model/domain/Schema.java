package ru.itfb.symphony.service.migration.model.domain;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "schema")
public class Schema {

    @Id
    @Column(name = "full_path_to_table")
    private String fullPathToTable;

    @Column(name = "export_schema", nullable = false)
    private String exportSchema;

    @Column(name = "version", nullable = false)
    private String version;
}
