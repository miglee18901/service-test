package org.example.entity;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;

@Entity
@Table(name = "cronjob_execution", uniqueConstraints = {
        @UniqueConstraint(
                name = "uk_ce_execution_info",
                columnNames = "execution_info_id")
}, indexes = {
        @Index(
                name = "idx_ce_cronjob_status",
                columnList = "cronjob_id,status")
})
@Getter
@Setter
public class CronjobExecution {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cronjob_id", nullable = false)
    private Cronjob cronjob;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "execution_info_id", nullable = false)
    private ExecutionInfo executionInfo;

    @Column(nullable = false)
    private Boolean status = true;
}
