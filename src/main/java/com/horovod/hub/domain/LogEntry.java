package com.horovod.hub.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "logs")
@Getter
@Setter
@NoArgsConstructor
public class LogEntry {

    @Id
    private String id;

    @Column(name = "time", nullable = false)
    private String time;

    @Column(nullable = false)
    private String type;

    @Column(nullable = false, length = 4000)
    private String message;

    @Column(nullable = false)
    private String status;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, length = 4000)
    private String body;

    private Instant timestamp;

    @Column(name = "bookingId")
    private String bookingId;
}
