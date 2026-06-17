package com.dataquadinc.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "attendance_week_off_config")
@Data
public class WeekOffConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "config_id")
    private Long configId;

    @Column(name = "day_of_week", nullable = false)
    private Integer dayOfWeek;

    @Column(name = "is_week_off")
    private Boolean isWeekOff = true;

    @Column(length = 10)
    private String entity = "IN";

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
}
