package ru.itfb.symphony.service.migration.model.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "transfer_table")
public class TransferTable {

    @Id
    @Column(name = "name_column", nullable = false)
    private String nameColumn;

    @Column(name = "old_id", nullable = false)
    private String oldId;

    @Column(name = "new_id", nullable = false)
    private String newId;
}
