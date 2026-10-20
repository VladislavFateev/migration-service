package ru.itfb.symphony.service.migration.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import ru.itfb.symphony.service.migration.model.domain.Schema;

import java.util.List;

@Repository
public interface SchemaRepository extends JpaRepository<Schema, String> {
    Schema findByFullPathToTable(String fullPathToTable);

    @Query(" select s.fullPathToTable from Schema as s")
    List<String> getAllFullPathToTable();

}
