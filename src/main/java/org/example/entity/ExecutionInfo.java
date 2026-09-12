package org.example.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.util.Date;
import java.util.List;

@Entity
@Table(name = "execution_info")
@Getter
@Setter
public class ExecutionInfo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private String name;
    private String description;
    private Integer state;
    @Column(name = "execution_level", nullable = false)
    private Integer executionLevel;
    @Column(nullable = false)
    private String user;
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "execution_time")
    private Date executionTime;
    @Column(name = "domain_id", nullable = false)
    private Integer domainId;
    @Column(name = "execution_name")
    private String executionName;
    @Column(name = "time_execute")
    private String timeExecute;
    @Column(name = "user_execute")
    private String userExecute;
    @Column(name = "current_state")
    private Integer currentState;
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "create_time")
    private Date createTime;
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "start_execution_time")
    private Date startExecutionTime;
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "completed_time")
    private Date completedTime;
    @Column(name = "env_info", nullable = false)
    private String envInfo;
    @Column(name = "pos_index", nullable = false)
    private Integer posIndex;
    @Column(name = "category_id")
    private Long categoryId;
    @Column(name = "is_number_test", nullable = false)
    private Boolean isNumberTest = true;

    @OneToMany(mappedBy = "executionInfo", fetch = FetchType.LAZY)
    @JsonIgnore
    private List<ExecutionElement> executionElements;
}
