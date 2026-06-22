package com.horovod.hub.service;

import com.horovod.hub.config.HorovodProperties;
import com.horovod.hub.domain.LogEntry;
import com.horovod.hub.dto.RealtimeEvent;
import com.horovod.hub.repository.LogEntryRepository;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LogService {

    private final LogEntryRepository logEntryRepository;
    private final HorovodProperties properties;
    private final EventBroadcastService eventBroadcastService;

    public LogService(
            LogEntryRepository logEntryRepository,
            HorovodProperties properties,
            EventBroadcastService eventBroadcastService
    ) {
        this.logEntryRepository = logEntryRepository;
        this.properties = properties;
        this.eventBroadcastService = eventBroadcastService;
    }

    public List<LogEntry> findRecent() {
        return logEntryRepository.findTop50ByOrderByTimestampDesc();
    }

    @Transactional
    public LogEntry create(LogEntry logEntry) {
        if (logEntry.getTimestamp() == null) {
            logEntry.setTimestamp(Instant.now());
        }
        LogEntry saved = logEntryRepository.save(logEntry);
        trimOldLogs();
        eventBroadcastService.broadcast(new RealtimeEvent("logs", "INSERT", null, saved));
        return saved;
    }

    private void trimOldLogs() {
        List<LogEntry> all = logEntryRepository.findAll();
        if (all.size() <= properties.getMaxLogEntries()) {
            return;
        }
        all.sort((a, b) -> {
            Instant ta = a.getTimestamp() != null ? a.getTimestamp() : Instant.EPOCH;
            Instant tb = b.getTimestamp() != null ? b.getTimestamp() : Instant.EPOCH;
            return tb.compareTo(ta);
        });
        for (int i = properties.getMaxLogEntries(); i < all.size(); i++) {
            logEntryRepository.delete(all.get(i));
        }
    }
}
