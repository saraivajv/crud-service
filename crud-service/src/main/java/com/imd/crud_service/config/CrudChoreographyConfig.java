package com.imd.crud_service.config;

import com.imd.common.events.*;
import com.imd.crud_service.repository.EmployeeRepository; // Import necessário
import org.slf4j.Logger; // Import necessário
import org.slf4j.LoggerFactory; // Import necessário
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import reactor.core.publisher.Flux;

import java.util.function.Consumer;
import java.util.function.Supplier;

@Configuration
@Profile("choreography")
public class CrudChoreographyConfig {

    // 1. Definição do Logger (Faltava isso)
    private static final Logger log = LoggerFactory.getLogger(CrudChoreographyConfig.class);

    // 2. Injeção do Repositório (Faltava isso)
    private final EmployeeRepository repository;

    public CrudChoreographyConfig(EmployeeRepository repository) {
        this.repository = repository;
    }

    // 3. PRODUTOR: Pega do SagaConfig e manda para o Broker
    @Bean
    public Supplier<Flux<EmployeeEvent>> employeeProducer(SagaConfig sagaConfig) {
        return () -> sagaConfig.getChoreoFlux()
                .doOnSubscribe(s -> System.out.println("RABBIT [DEBUG]: O Spring Cloud Stream se inscreveu no Fluxo!"))
                .doOnNext(e -> System.out.println("RABBIT [DEBUG]: Mensagem capturada pelo Stream e indo para o Rabbit: " + e));
    }

    // 4. CONSUMIDOR: Recebe respostas do Broker
    @Bean
    public Consumer<EmployeeEvent> employeeResultConsumer() {
        return event -> {
            if (event instanceof EmployeeApproved approved) {
                log.info("✅ SUCESSO FINAL: ID {}", approved.dbId());
                updateStatus(approved.dbId(), "APPROVED", "Concluído");
            }
            else if (event instanceof SalaryRejected rejected) {
                log.warn("❌ FALHA RH: ID {}", rejected.eventId());
                updateStatus(rejected.eventId(), "REJECTED", rejected.reason());
            }
            else if (event instanceof EquipmentUnavailable unavailable) {
                log.warn("❌ FALHA ESTOQUE: ID {}", unavailable.eventId());
                updateStatus(unavailable.eventId(), "CANCELLED", unavailable.reason());
            }
            else if (event instanceof EmployeePersistenceFailed failed) {
                log.error("❌ FALHA DB: ID {}", failed.eventId());
                updateStatus(failed.eventId(), "ERROR", failed.reason());
            }
        };
    }

    private void updateStatus(java.util.UUID id, String status, String reason) {
        repository.updateStatus(id, status, reason)
                .doOnSuccess(rows -> {
                    if (rows > 0) log.info("🔄 CRUD DB: Status atualizado para {} (ID: {})", status, id);
                    else log.error("⚠️ CRUD DB: ID {} não encontrado para atualização!", id);
                })
                .subscribe(); // Dispara a ação reativa (Fire-and-forget para o consumidor)
    }
}