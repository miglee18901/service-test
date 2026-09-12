package org.example.repository;

import org.example.entity.CronjobExecution;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CronjobExecutionRepository extends JpaRepository<CronjobExecution, Long> {
    boolean existsByExecutionInfoId(Long executionInfoId);

    boolean existsByExecutionInfoIdAndIdNot(Long executionInfoId, Long id);

    boolean existsByCronjobId(Long cronjobId);

    boolean existsByCronjobIdAndStatusTrue(Long cronjobId);

    List<CronjobExecution> findByCronjobId(Long cronjobId);

    List<CronjobExecution> findByCronjobIdAndStatusTrue(Long cronjobId);

    @Query("select ce from CronjobExecution ce " +
            "join ce.cronjob c join ce.executionInfo e " +
            "where (:cronjobId is null or c.id = :cronjobId) " +
            "and (:executionInfoId is null or e.id = :executionInfoId) " +
            "and (:status is null or ce.status = :status) " +
            "and (:keyword = '' or lower(c.name) like lower(concat('%',:keyword,'%')) " +
            "or lower(e.name) like lower(concat('%',:keyword,'%')))")
    Page<CronjobExecution> search(
            @Param("keyword") String keyword,
            @Param("cronjobId") Long cronjobId,
            @Param("executionInfoId") Long executionInfoId,
            @Param("status") Boolean status,
            Pageable pageable);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update CronjobExecution ce set ce.status = :newStatus " +
            "where ce.id = :id and ce.status = :expectedStatus")
    int updateStatusIfMatches(
            @Param("id") Long id,
            @Param("expectedStatus") Boolean expectedStatus,
            @Param("newStatus") Boolean newStatus);
}
