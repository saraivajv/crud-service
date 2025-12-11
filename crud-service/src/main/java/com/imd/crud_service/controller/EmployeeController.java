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
    private final SagaConfig sagaConfig; // Injeção para enviar eventos

    // Injeta o profile ativo para decidir a estratégia
    @Value("${spring.profiles.active:}")
    private String activeProfile;

    public EmployeeController(EmployeeService employeeService, SagaConfig sagaConfig) {
        this.employeeService = employeeService;
        this.sagaConfig = sagaConfig;
    }

    // --- MUDANÇA PRINCIPAL AQUI ---
    @PostMapping
    public Mono<ResponseEntity<EmployeeDTO>> createEmployee(@RequestBody EmployeeDTO employee) {
        // Geramos um ID de evento para rastreio
        UUID sagaId = UUID.randomUUID();

        // Verificamos qual estratégia usar baseada no profile
        if (activeProfile.contains("choreography")) {
            // Estratégia 1: Emitir Evento (Coreografia)
            EmployeeCreationRequested event = new EmployeeCreationRequested(
                    sagaId,
                    employee.getName(),
                    employee.getPosition(),
                    employee.getSalary()
            );
            sagaConfig.send(event);

        } else if (activeProfile.contains("orchestration")) {
            // Estratégia 2: Emitir Comando (Orquestração)
            ValidateSalaryCommand command = new ValidateSalaryCommand(
                    sagaId,
                    employee.getName(),
                    employee.getPosition(),
                    employee.getSalary()
            );
            sagaConfig.send(command);
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