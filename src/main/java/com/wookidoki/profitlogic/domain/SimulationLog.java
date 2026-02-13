package com.wookidoki.profitlogic.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "simulation_logs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class SimulationLog extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(nullable = false, length = 200)
    private String description;

    @Column(name = "result_json", nullable = false, columnDefinition = "TEXT")
    private String resultJson;
}
