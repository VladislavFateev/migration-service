package ru.itfb.symphony.service.migration.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cglib.core.Local;
import org.springframework.stereotype.Service;
import ru.itfb.symphony.service.migration.enums.MigrationStatus;
import ru.itfb.symphony.service.migration.enums.MigrationTaskType;
import ru.itfb.symphony.service.migration.mapper.MigrationTaskMapper;
import ru.itfb.symphony.service.migration.model.domain.MigrationTask;
import ru.itfb.symphony.service.migration.model.dto.MigrationTaskDto;
import ru.itfb.symphony.service.migration.repository.MigrationTaskRepository;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class MigrationTaskService {

    private final MigrationTaskRepository repository;
    private final MigrationTaskMapper mapper;

    public MigrationTaskDto generateMigrationTask(MigrationTaskType taskType, String schemaName) {
        MigrationTask migrationTask = MigrationTask
                .builder()
                .guid(UUID.randomUUID().toString())
                .startCreatedTime(LocalDateTime.now())
                .status(MigrationStatus.NOT_READY.getStatus())
                .taskType(taskType.getTaskType())
                .schemaName(schemaName)
                .build();
        repository.save(migrationTask);
        return mapper.domainToDto(migrationTask);
    }

    public void updateStatusReady(String guid) {
        MigrationTask migrationTask = repository.findById(guid).orElseThrow();
        migrationTask.setStatus(MigrationStatus.READY.getStatus());
        migrationTask.setCreatedTime(LocalDateTime.now());
        repository.save(migrationTask);
    }

    public void updateStatusError(String guid) {
        MigrationTask migrationTask = repository.findById(guid).orElseThrow();
        migrationTask.setStatus(MigrationStatus.ERROR.getStatus());
        repository.save(migrationTask);
    }

    public Boolean isReadyTask(String guid) {
        MigrationTask migrationTask = repository.findById(guid).orElseThrow();
        return migrationTask.getStatus().equals(MigrationStatus.READY.getStatus());
    }

    public MigrationTaskDto findMigrationTask(String guid) {
        return mapper.domainToDto(repository.findById(guid).orElseThrow());
    }
}
