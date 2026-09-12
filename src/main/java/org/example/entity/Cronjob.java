package org.example.entity;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;

@Entity
@Table(name = "cronjob", uniqueConstraints = {
        @UniqueConstraint(name = "uk_cronjob_name", columnNames = "name")
})
@Getter
@Setter
public class Cronjob {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(name = "cron_value", nullable = false, length = 100)
    private String cronValue;
}
