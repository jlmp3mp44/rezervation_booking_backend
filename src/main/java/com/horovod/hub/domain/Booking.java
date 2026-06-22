package com.horovod.hub.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "bookings")
@Getter
@Setter
@NoArgsConstructor
public class Booking {

    @Id
    private String id;

    @Column(name = "userName", nullable = false)
    private String userName;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private String date;

    @Column(name = "startTime", nullable = false)
    private String startTime;

    @Column(name = "endTime", nullable = false)
    private String endTime;

    @Column(nullable = false)
    private String type;

    private String notes;

    @Column(nullable = false)
    private String status;

    @Column(name = "submittedAt")
    private String submittedAt;

    @Column(name = "cancelReason")
    private String cancelReason;

    @Column(name = "cancelledBy")
    private String cancelledBy;
}
