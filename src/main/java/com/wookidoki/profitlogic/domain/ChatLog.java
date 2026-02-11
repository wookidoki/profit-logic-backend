package com.wookidoki.profitlogic.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "chat_logs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ChatLog extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Lob
    @Column(nullable = false, columnDefinition = "TEXT")
    private String question;

    @Lob
    @Column(nullable = false, columnDefinition = "TEXT")
    private String answer;

    @Column(name = "tokens_used", nullable = false)
    @Builder.Default
    private Integer tokensUsed = 0;
}
