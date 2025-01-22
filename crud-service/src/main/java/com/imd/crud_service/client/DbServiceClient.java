package com.imd.crud_service.client;

import com.imd.crud_service.dto.EmployeeDTO;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

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

    // Métodos de fallback
    default EmployeeDTO fallbackCreateEmployee(EmployeeDTO employee, Throwable t) {
        // Retorno padrão em caso de falha
        return new EmployeeDTO();  // Pode retornar um valor default ou uma mensagem de erro
    }

    default List<EmployeeDTO> fallbackGetAllEmployees(Throwable t) {
        return List.of();  // Retorna uma lista vazia em caso de falha
    }

    default EmployeeDTO fallbackGetEmployeeById(Long id, Throwable t) {
        return new EmployeeDTO();  // Retorna um valor default para o caso de falha
    }

    default EmployeeDTO fallbackUpdateEmployee(Long id, EmployeeDTO employee, Throwable t) {
        return new EmployeeDTO();  // Retorna um valor default para o caso de falha
    }

    default void fallbackDeleteEmployee(Long id, Throwable t) {
        // Pode registrar o erro ou retornar um valor adequado para falhas
    }
}
