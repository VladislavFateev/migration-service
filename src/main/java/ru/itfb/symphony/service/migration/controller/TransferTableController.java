package ru.itfb.symphony.service.migration.controller;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.itfb.symphony.service.migration.service.TransferTableService;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("public/transfer")
public class TransferTableController {

    private final TransferTableService transferTableService;

    @DeleteMapping
    public void deleteAll() {
        transferTableService.deleteAll();
    }
}
