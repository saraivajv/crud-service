package com.imd.crud_service.config;

import com.imd.common.events.SagaCommand;
import com.imd.common.events.SagaReply;
import com.imd.common.events.SalaryValidationResult;
import com.imd.common.events.CompleteCreationCommand;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import java.util.function.Consumer;
import java.util.function.Supplier;
import reactor.core.publisher.Flux;

@Configuration
@Profile("orchestration") // Só carrega se o perfil for orchestration
public class CrudOrchestrationConfig {

    // 1. PRODUTOR: Pega o que o Controller jogou no Sink e manda pro Kafka/Rabbit
    @Bean
    public Supplier<Flux<SagaCommand>> sagaCommandProducer(SagaConfig sagaConfig) {
        return sagaConfig::getOrchFlux;
    }

    // 2. CONSUMIDOR: A lógica do Maestro (antigo SagaOrchestrator)
    @Bean
    public Consumer<SagaReply> sagaReplyConsumer(SagaConfig sagaConfig) {
        return reply -> {
            if (reply instanceof SalaryValidationResult res) {
                if (res.approved()) {
                    System.out.println("MAESTRO: Salário Aprovado. Enviando ordem de persistência.");

                    // Envia o próximo comando via SagaConfig
                    sagaConfig.sendCommand(new CompleteCreationCommand(res.sagaId(), "NOME_CACHE", "POS_CACHE", 1000.0));

                } else {
                    System.out.println("MAESTRO: Salário Rejeitado. Fim da Saga.");
                }
            }
        };
    }
}