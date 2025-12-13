package com.imd.crud_service.repository;

import com.imd.crud_service.model.Employee;
import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface EmployeeRepository extends R2dbcRepository<Employee, UUID> {
    @Modifying
    @Query("UPDATE employee SET status = :status, reason_msg = :reason WHERE id = :id")
    Mono<Integer> updateStatus(UUID id, String status, String reason);
}
