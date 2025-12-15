package com.imd.crud_service.config;

import com.imd.common.events.*;
import com.imd.crud_service.dto.EmployeeDTO;
import com.imd.crud_service.repository.EmployeeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Supplier;

@Configuration
@Profile("orchestration")
public class CrudOrchestrationConfig {

    private static final Logger log = LoggerFactory.getLogger(CrudOrchestrationConfig.class);
    private final SagaConfig sagaConfig;
    private final EmployeeRepository employeeRepository;

    // Memória do Maestro (Guarda os dados originais enquanto a saga viaja)
    private final Map<UUID, SagaState> sagaStateMap = new ConcurrentHashMap<>();

    public record SagaState(String name, String position, Double salary) {}

    public CrudOrchestrationConfig(SagaConfig sagaConfig, EmployeeRepository employeeRepository) {
        this.sagaConfig = sagaConfig;
        this.employeeRepository = employeeRepository;
    }

    // --- PONTO DE PARTIDA (Chamado pelo Controller) ---
    public void startSaga(UUID sagaId, EmployeeDTO dto) {
        // 1. Salva estado Sincronamente antes de qualquer coisa
        SagaState state = new SagaState(dto.name(), dto.position(), dto.salary());
        sagaStateMap.put(sagaId, state);

        log.info("MAESTRO: [START] Saga {} iniciada. Estado salvo. Map Size: {}", sagaId, sagaStateMap.size());

        // 2. Envia o primeiro comando (Payroll)
        ValidateSalaryCommand cmd = new ValidateSalaryCommand(sagaId, dto.name(), dto.position(), dto.salary());
        sagaConfig.sendCommand(cmd, "payroll-commands");
    }

    // --- BEAN 1: TRIGGER (Supplier) ---
    @Bean
    public Supplier<Flux<Message<SagaCommand>>> orchestratorTrigger() {
        return sagaConfig::getOrchFlux;
    }

    // --- BEAN 2: PROCESSADOR (Function) ---
    @Bean
    public Function<Flux<SagaReply>, Flux<Message<SagaCommand>>> orchestratorProcess() {
        return flux -> flux.flatMap(reply -> {
            // Executa a lógica de decisão
            Message<SagaCommand> nextStep = decideNextStep(reply);

            // Se nextStep for null (fim da saga ou erro), o Mono.justOrEmpty encerra o fluxo reativo graciosamente
            return Mono.justOrEmpty(nextStep);
        });
    }

    // --- MÁQUINA DE ESTADOS DO MAESTRO ---
    private Message<SagaCommand> decideNextStep(SagaReply reply) {
        UUID sagaId = reply.sagaId();
        SagaState state = sagaStateMap.get(sagaId);

        if (state == null) {
            log.error("MAESTRO CRÍTICO: Estado perdido para Saga {}. Ignorando resposta.", sagaId);
            return null;
        }

        // ---------------------------------------------------------------------
        // 1. Resposta do PAYROLL (Validar Salário)
        // ---------------------------------------------------------------------
        if (reply instanceof SalaryValidationResult res) {
            if (res.approved()) {
                log.info("MAESTRO: Salário Aprovado. Próximo passo: Inventory (Reservar).");

                ReserveEquipmentCommand cmd = new ReserveEquipmentCommand(sagaId, state.name(), state.salary());
                return createCommand(cmd, "inventory-commands");
            } else {
                log.warn("MAESTRO: Salário Rejeitado. Motivo: {}. Encerrando Saga.", res.reason());

                updateStatus(sagaId, "REJECTED", res.reason());
                sagaStateMap.remove(sagaId);
                return null;
            }
        }

        // ---------------------------------------------------------------------
        // 2. Resposta do INVENTORY (Reservar Equipamento)
        // ---------------------------------------------------------------------
        else if (reply instanceof EquipmentReservationResult res) {
            if (res.success()) {
                log.info("MAESTRO: Estoque Reservado. Próximo passo: DB Service (Salvar Final).");

                CompleteCreationCommand cmd = new CompleteCreationCommand(
                        sagaId, state.name(), state.position(), state.salary()
                );
                return createCommand(cmd, "db-commands");
            } else {
                log.error("MAESTRO: Sem Estoque. Motivo: {}. Cancelando.", res.message());

                updateStatus(sagaId, "CANCELLED", res.message());
                sagaStateMap.remove(sagaId);
                return null;
            }
        }

        // ---------------------------------------------------------------------
        // 3. Resposta do DB SERVICE (Persistência Final)
        // ---------------------------------------------------------------------
        else if (reply instanceof DbCompletionResult res) {
            if (res.success()) {
                // --- SUCESSO ABSOLUTO ---
                log.info("✅ MAESTRO: Saga Finalizada com SUCESSO! ID DB: {}", res.dbId());

                updateStatus(sagaId, "APPROVED", "Concluído");
                sagaStateMap.remove(sagaId);
                return null;
            } else {
                // --- FALHA NO FIM -> INICIAR ROLLBACK ---
                log.error("MAESTRO: ❌ Falha no DB ({}). Iniciando COMPENSAÇÃO do Inventory.", res.message());

                // 1. Atualiza status para indicar que deu erro
                updateStatus(sagaId, "ERROR", "Falha DB: " + res.message());

                // 2. Cria o comando de compensação para devolver o Laptop
                CompensateStockCommand rollbackCmd = new CompensateStockCommand(
                        sagaId,
                        "Falha na persistência: " + res.message()
                );

                // 3. Limpa a memória
                sagaStateMap.remove(sagaId);

                // 4. Envia para o Inventory desfazer a reserva
                return createCommand(rollbackCmd, "inventory-commands");
            }
        }

        return null;
    }

    // --- MÉTODOS AUXILIARES ---

    // Atualiza o status no Banco de Dados de forma reativa e segura
    private void updateStatus(UUID sagaId, String status, String reason) {
        employeeRepository.findById(sagaId)
                .flatMap(emp -> {
                    emp.setStatus(status);
                    emp.setReasonMsg(reason);
                    return employeeRepository.save(emp);
                })
                .doOnSuccess(saved -> log.info("🔄 DB Atualizado: {} | Motivo: {}", status, reason))
                .doOnError(e -> log.error("❌ Falha ao atualizar status no banco", e))
                .subscribe(); // Dispara em background (Fire-and-forget)
    }

    private Message<SagaCommand> createCommand(SagaCommand cmd, String dest) {
        return MessageBuilder.withPayload(cmd)
                .setHeader("spring.cloud.stream.sendto.destination", dest)
                .build();
    }
}