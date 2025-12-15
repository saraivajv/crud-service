package com.imd.crud_service.controller;

import com.imd.common.events.EmployeeCreationRequested;
import com.imd.crud_service.config.CrudOrchestrationConfig;
import com.imd.crud_service.config.SagaConfig;
import com.imd.crud_service.dto.EmployeeDTO;
import com.imd.crud_service.model.Employee; // Importe a Entidade
import com.imd.crud_service.repository.EmployeeRepository; // Importe o Repo
import com.imd.crud_service.service.EmployeeService;
import org.springframework.beans.factory.annotation.Autowired;
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
    private final EmployeeRepository employeeRepository; // Injetamos o Repo direto para a Saga
    private final SagaConfig sagaConfig;

    @Autowired(required = false)
    private CrudOrchestrationConfig orchestrator;

    @Value("${spring.profiles.active:}")
    private String activeProfile;

    public EmployeeController(EmployeeService employeeService, EmployeeRepository employeeRepository, SagaConfig sagaConfig) {
        this.employeeService = employeeService;
        this.employeeRepository = employeeRepository;
        this.sagaConfig = sagaConfig;
    }

    // Mudei o retorno para <Employee> (Entidade) para o usuário ver o ID e o Status
    @PostMapping
    public Mono<ResponseEntity<Employee>> createEmployee(@RequestBody EmployeeDTO dto) {
        // 1. Gera UUID da Saga
        UUID sagaId = UUID.randomUUID();

        // 2. Cria Entidade com Status PENDING (Usando os acessors do Record: dto.name())
        Employee entity = new Employee(
                sagaId,
                dto.name(),      // Record usa .name(), não .getName()
                dto.position(),  // Record usa .position()
                dto.salary(),    // Record usa .salary()
                "PENDING"
        );

        // 3. Salva no Banco -> DEPOIS envia evento
        return employeeRepository.save(entity)
                .flatMap(saved -> {
                    // Lógica de envio (Coreografia vs Orquestração)
                    if (activeProfile.contains("choreography")) {
                        System.out.println("Controller: Iniciando fluxo COREOGRAFIA para ID: " + sagaId);

                        EmployeeCreationRequested event = new EmployeeCreationRequested(
                                sagaId,
                                dto.name(),
                                dto.position(),
                                dto.salary()
                        );
                        sagaConfig.sendEvent(event);

                    } else if (activeProfile.contains("orchestration")) {
                        System.out.println("Controller: Iniciando fluxo ORQUESTRAÇÃO para ID: " + sagaId);

                        if (orchestrator != null) {
                            orchestrator.startSaga(sagaId, dto);
                        } else {
                            System.err.println("ERRO CRÍTICO: Bean do Orquestrador não carregado!");
                        }
                    }
                    return Mono.just(ResponseEntity.accepted().body(saved));
                });
    }

    @GetMapping
    public Flux<Employee> getAllEmployees() {
        // Ajuste seu service para retornar Flux<Employee> se necessário, ou chame o repo direto
        return employeeRepository.findAll();
    }

    // Mudei Long para UUID
    @GetMapping("/{id}")
    public Mono<ResponseEntity<Employee>> getEmployeeById(@PathVariable UUID id) {
        return employeeRepository.findById(id)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    // Mudei Long para UUID e removi a lógica antiga
    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<Void>> deleteEmployee(@PathVariable UUID id) {
        return employeeRepository.deleteById(id)
                .then(Mono.just(ResponseEntity.noContent().<Void>build()));
    }
}