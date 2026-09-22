package org.example.model;

import lombok.Data;
import javax.persistence.*;
import java.util.Date;

@Data
@Entity
@Table(name = "test_case")
public class TestCase {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private String name;
    private String description;
    private Integer state;
    private String version;
    private String versionDescription;
    private String offerVersionId;
    private Integer timeout;
    @Column(name = "category_id", nullable = false)
    private Long categoryId;
    @Column(name = "test_case_definition_id", nullable = false)
    private Long testCaseDefinitionId;
    @Column(nullable = false)
    private Integer posIndex;
    @Column(nullable = false)
    private Integer domainId;
    @Column(name = "create_at")
    private Date createAt;
    private Date updateDate;
}
