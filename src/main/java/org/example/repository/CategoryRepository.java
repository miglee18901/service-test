package org.example.repository;

import org.example.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    @Query("select c from Category c where c.categoryId = :id")
    Category findOne(@Param("id") Long id);

    List<Category> getChildCategoryByCategoryParentId(Long parentId);

    boolean existsByCategoryNameAndDomainId(String name, Integer domainId);

    Category findTopByCategoryNameStartingWithOrderByCategoryIdDesc(String prefix);

    default Category findOneContainCategoryByName(String prefix) {
        return findTopByCategoryNameStartingWithOrderByCategoryIdDesc(prefix);
    }

    @Query("select coalesce(max(c.posIndex), 0) from Category c where c.categoryParentId = :parentId and c.categoryType = :type")
    Integer getMaxPosIndex(@Param("parentId") Long parentId, @Param("type") Integer type);
}
