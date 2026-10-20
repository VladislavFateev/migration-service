package ru.itfb.symphony.service.migration.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.itfb.symphony.service.migration.model.domain.MigrationTask;

@Repository
public interface MigrationTaskRepository extends JpaRepository<MigrationTask, String> {
}
