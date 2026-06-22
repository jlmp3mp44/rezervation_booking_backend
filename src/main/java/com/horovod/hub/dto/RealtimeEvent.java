package com.horovod.hub.dto;

public record RealtimeEvent(
        String table,
        String eventType,
        Object oldRecord,
        Object newRecord
) {
}
