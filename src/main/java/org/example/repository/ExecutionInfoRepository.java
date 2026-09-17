package org.example.repository;

import org.example.model.ExecutionInfo;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExecutionInfoRepository extends JpaRepository<ExecutionInfo, Long> {
    default ExecutionInfo findOne(Long id) {
        return findById(id).orElse(null);
    }
}
