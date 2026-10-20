package ru.itfb.symphony.service.migration.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.itfb.symphony.service.migration.model.domain.TransferTable;

public interface TransferTableRepository extends JpaRepository<TransferTable, String> {
}
