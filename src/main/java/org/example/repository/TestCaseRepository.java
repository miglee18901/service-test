package org.example.repository;

import org.example.model.TestCase;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TestCaseRepository extends JpaRepository<TestCase, Long> {
    @Query("select t from TestCase t where t.id = :id")
    TestCase findOne(@Param("id") Long id);

    List<TestCase> findByCategoryIdAndDomainId(Long categoryId, Integer domainId);

    boolean existsByNameAndDomainId(String name, Integer domainId);

    @Query("select t from TestCase t where t.name like :name escape '\\\\'")
    TestCase findName(@Param("name") String name);

    TestCase findTopByNameStartingWithOrderByIdDesc(String prefix);

    default TestCase findContainNameTestCase(String prefix) {
        return findTopByNameStartingWithOrderByIdDesc(prefix);
    }

    @Query("select coalesce(max(t.posIndex), 0) from TestCase t where t.domainId = :domainId and t.categoryId = :categoryId")
    Integer getMaxPosIndex(@Param("domainId") Integer domainId, @Param("categoryId") Long categoryId);
}
