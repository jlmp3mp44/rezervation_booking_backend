package com.horovod.hub.repository;

import com.horovod.hub.domain.LogEntry;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LogEntryRepository extends JpaRepository<LogEntry, String> {

    List<LogEntry> findTop50ByOrderByTimestampDesc();
}
