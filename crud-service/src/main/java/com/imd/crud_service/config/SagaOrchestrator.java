package com.imd.crud_service.config;

import com.imd.common.events.CompleteCreationCommand;
import com.imd.common.events.SagaReply;
import com.imd.common.events.SalaryValidationResult;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.function.Consumer;

@Service
@Profile("orchestration")
public class SagaOrchestrator {
    private final SagaConfig sagaConfig; // Usa o config acima para enviar comandos

    public SagaOrchestrator(SagaConfig config) { this.sagaConfig = config; }

    @Bean
    public Consumer<SagaReply> sagaReplyConsumer() {
        return reply -> {
            if (reply instanceof SalaryValidationResult res) {
                if (res.approved()) {
                    System.out.println("Maestro: Salário OK via Rabbit/Kafka. Salvando no DB...");
                    // AQUI VOCÊ DEVERIA RECUPERAR OS DADOS DO CACHE/DB TEMPORÁRIO
                    // Para simplificar, vou mandar um dado Hardcoded, mas na real vc usa o sagaId para buscar o DTO original
                    sagaConfig.send(new CompleteCreationCommand(res.sagaId(), "John Doe", "Dev", 5000.0));
                } else {
                    System.out.println("Maestro: REJEITADO.");
                }
            }
        };
    }
}