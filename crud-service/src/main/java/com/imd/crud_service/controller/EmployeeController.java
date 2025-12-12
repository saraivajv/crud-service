package com.imd.crud_service.controller;

import com.imd.common.events.EmployeeCreationRequested;
import com.imd.common.events.ValidateSalaryCommand;
import com.imd.crud_service.config.SagaConfig;
import com.imd.crud_service.dto.EmployeeDTO;
import com.imd.crud_service.service.EmployeeService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RestController
@RequestMapping("/employees")
public class EmployeeController {

    private final EmployeeService employeeService;
    private final SagaConfig sagaConfig;

    // Injeta o profile ativo
    @Value("${spring.profiles.active:}")
    private String activeProfile;

    public EmployeeController(EmployeeService employeeService, SagaConfig sagaConfig) {
        this.employeeService = employeeService;
        this.sagaConfig = sagaConfig;
    }

    @PostMapping
    public Mono<ResponseEntity<EmployeeDTO>> createEmployee(@RequestBody EmployeeDTO employee) {
        UUID sagaId = UUID.randomUUID();

        // 1. VERIFICAÇÃO PARA COREOGRAFIA
        if (activeProfile.contains("choreography")) {
            System.out.println("Controller: Iniciando fluxo COREOGRAFIA");

            EmployeeCreationRequested event = new EmployeeCreationRequested(
                    sagaId,
                    employee.getName(),
                    employee.getPosition(),
                    employee.getSalary()
            );

            // CORREÇÃO: Chama o método específico 'sendEvent'
            sagaConfig.sendEvent(event);

            // 2. VERIFICAÇÃO PARA ORQUESTRAÇÃO
        } else if (activeProfile.contains("orchestration")) {
            System.out.println("Controller: Iniciando fluxo ORQUESTRAÇÃO (Maestro)");

            ValidateSalaryCommand command = new ValidateSalaryCommand(
                    sagaId,
                    employee.getName(),
                    employee.getPosition(),
                    employee.getSalary()
            );

            sagaConfig.sendCommand(command);

        }
        employee.setAiReview("PROCESSANDO_SAGA_" + sagaId);
        return Mono.just(ResponseEntity.accepted().body(employee));
    }

    @GetMapping
    public Flux<EmployeeDTO> getAllEmployees() {
        return employeeService.getAllEmployees();
    }

    @GetMapping("/{id}")
    public Mono<ResponseEntity<EmployeeDTO>> getEmployeeById(@PathVariable Long id) {
        return employeeService.getEmployeeById(id)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public Mono<ResponseEntity<EmployeeDTO>> updateEmployee(@PathVariable Long id, @RequestBody EmployeeDTO employeeDetails) {
        return employeeService.updateEmployee(id, employeeDetails)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<Void>> deleteEmployee(@PathVariable Long id) {
        return employeeService.deleteEmployee(id)
                .then(Mono.just(ResponseEntity.noContent().<Void>build()));
    }

    @PostMapping("/{id}/generate-review")
    public Mono<ResponseEntity<EmployeeDTO>> generateEmployeeReview(@PathVariable Long id) {
        return employeeService.generateAndSaveReview(id)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }
}