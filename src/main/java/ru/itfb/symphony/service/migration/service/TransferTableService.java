package ru.itfb.symphony.service.migration.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.itfb.symphony.service.migration.repository.TransferTableRepository;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransferTableService {

    private final TransferTableRepository transferTableRepository;

    @Transactional
    public void deleteAll() {
        transferTableRepository.deleteAllInBatch();
    }
}
