package com.horovod.hub.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "issues")
@Getter
@Setter
@NoArgsConstructor
public class Issue {

    @Id
    private String id;

    @Column(nullable = false)
    private String category;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, length = 2000)
    private String description;

    @Column(name = "reportedBy", nullable = false)
    private String reportedBy;

    @Column(name = "reportedAt", nullable = false)
    private String reportedAt;

    private boolean resolved;

    @Column(name = "resolvedAt")
    private String resolvedAt;

    private String resolution;
}
