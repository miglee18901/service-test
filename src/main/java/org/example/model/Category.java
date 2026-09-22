package org.example.model;

import lombok.Data;
import javax.persistence.*;

@Data
@Entity
@Table(name = "category")
public class Category {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "category_id")
    private Long categoryId;
    private Integer categoryType;
    private String categoryName;
    @Column(name = "treeType")
    private String treeType;
    private Integer posIndex;
    private String remark;
    private Integer domainId;
    @Column(name = "category_parent_id")
    private Long categoryParentId;
    private String description;
}
