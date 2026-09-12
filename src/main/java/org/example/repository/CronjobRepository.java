package org.example.repository;

import org.example.entity.Cronjob;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CronjobRepository extends JpaRepository<Cronjob, Long> {
    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

    @Query("select c from Cronjob c where :keyword = '' " +
            "or lower(c.name) like lower(concat('%', :keyword, '%')) " +
            "or lower(c.cronValue) like lower(concat('%', :keyword, '%'))")
    Page<Cronjob> search(@Param("keyword") String keyword, Pageable pageable);

    @Query("select distinct c from Cronjob c join CronjobExecution ce " +
            "on ce.cronjob.id = c.id where ce.status = true")
    List<Cronjob> findAllHavingEnabledExecutions();
}
