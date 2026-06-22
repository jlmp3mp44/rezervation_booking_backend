package com.horovod.hub.controller;

import com.horovod.hub.domain.LogEntry;
import com.horovod.hub.service.LogService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/logs")
public class LogController {

    private final LogService logService;

    public LogController(LogService logService) {
        this.logService = logService;
    }

    @GetMapping
    public List<LogEntry> findAll(
            @RequestParam(required = false) String order,
            @RequestParam(required = false, defaultValue = "false") boolean ascending,
            @RequestParam(required = false) Integer limit
    ) {
        List<LogEntry> logs = logService.findRecent();
        if (limit != null && limit > 0 && logs.size() > limit) {
            return logs.subList(0, limit);
        }
        return logs;
    }

    @PostMapping
    public LogEntry create(@RequestBody LogEntry logEntry) {
        return logService.create(logEntry);
    }
}
