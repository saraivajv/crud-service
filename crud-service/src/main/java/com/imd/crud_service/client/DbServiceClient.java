package com.imd.crud_service.client;

import com.imd.crud_service.dto.EmployeeDTO;
import com.imd.crud_service.dto.ReviewDTO;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;

@FeignClient(name = "db-service")
public interface DbServiceClient {

    // Circuit Breaker e Retry aplicados na criação de empregado
    @CircuitBreaker(name = "dbServiceCircuitBreaker", fallbackMethod = "fallbackCreateEmployee")
    @Retry(name = "dbServiceRetry", fallbackMethod = "fallbackCreateEmployee")
    @PostMapping("/db/employees")
    EmployeeDTO createEmployee(@RequestBody EmployeeDTO employee);

    // Circuit Breaker e Retry aplicados na busca de todos os empregados
    @CircuitBreaker(name = "dbServiceCircuitBreaker", fallbackMethod = "fallbackGetAllEmployees")
    @Retry(name = "dbServiceRetry", fallbackMethod = "fallbackGetAllEmployees")
    @GetMapping("/db/employees")
    List<EmployeeDTO> getAllEmployees();

    // Circuit Breaker e Retry aplicados na busca de empregado por id
    @CircuitBreaker(name = "dbServiceCircuitBreaker", fallbackMethod = "fallbackGetEmployeeById")
    @Retry(name = "dbServiceRetry", fallbackMethod = "fallbackGetEmployeeById")
    @GetMapping("/db/employees/{id}")
    EmployeeDTO getEmployeeById(@PathVariable("id") Long id);

    // Circuit Breaker e Retry aplicados na atualização de empregado
    @CircuitBreaker(name = "dbServiceCircuitBreaker", fallbackMethod = "fallbackUpdateEmployee")
    @Retry(name = "dbServiceRetry", fallbackMethod = "fallbackUpdateEmployee")
    @PutMapping("/db/employees/{id}")
    EmployeeDTO updateEmployee(@PathVariable("id") Long id, @RequestBody EmployeeDTO employee);

    // Circuit Breaker e Retry aplicados na exclusão de empregado
    @CircuitBreaker(name = "dbServiceCircuitBreaker", fallbackMethod = "fallbackDeleteEmployee")
    @Retry(name = "dbServiceRetry", fallbackMethod = "fallbackDeleteEmployee")
    @DeleteMapping("/db/employees/{id}")
    void deleteEmployee(@PathVariable("id") Long id);

    @CircuitBreaker(name = "dbServiceCircuitBreaker", fallbackMethod = "fallbackSaveReview")
    @Retry(name = "dbServiceRetry")
    @PostMapping("/db/employees/{id}/review")
    EmployeeDTO saveReview(@PathVariable("id") Long employeeId, @RequestBody ReviewDTO reviewDTO);

    // Métodos de fallback
    default EmployeeDTO fallbackCreateEmployee(EmployeeDTO employee, Throwable t) {
        System.err.println("Fallback para createEmployee ativado. Causa: " + t.toString());
        return new EmployeeDTO(null, "Falha", "Falha", 0.0, null);
    }

    default List<EmployeeDTO> fallbackGetAllEmployees(Throwable t) {
        System.err.println("Fallback para getAllEmployees ativado. Causa: " + t.toString());
        return Collections.emptyList();
    }

    default EmployeeDTO fallbackGetEmployeeById(Long id, Throwable t) {
        System.err.println("Fallback para getEmployeeById ativado para o ID: " + id + ". Causa: " + t.toString());
        return new EmployeeDTO(id, "Não encontrado", "Falha", 0.0, null);
    }

    default EmployeeDTO fallbackUpdateEmployee(Long id, EmployeeDTO employee, Throwable t) {
        System.err.println("Fallback para updateEmployee ativado para o ID: " + id + ". Causa: " + t.toString());
        return employee; // Retorna o objeto original para não perder os dados da tentativa
    }

    default void fallbackDeleteEmployee(Long id, Throwable t) {
        System.err.println("Fallback para deleteEmployee ativado para o ID: " + id + ". Causa: " + t.toString());
    }

    default EmployeeDTO fallbackSaveReview(Long employeeId, ReviewDTO reviewDTO, Throwable t) {
        System.err.println("Fallback para saveReview ativado para o ID: " + employeeId + ". Causa: " + t.toString());
        // Retorna um DTO indicando falha, mas sem perder o ID
        EmployeeDTO fallbackDto = new EmployeeDTO();
        fallbackDto.setId(employeeId);
        return fallbackDto;
    }
}
