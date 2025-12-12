package com.imd.crud_service.config;

import com.imd.common.events.EmployeeEvent;
import com.imd.common.events.SalaryRejected;
import com.imd.common.events.EmployeeApproved;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import java.util.function.Consumer;
import java.util.function.Supplier;
import reactor.core.publisher.Flux;

@Configuration
@Profile("choreography")
public class CrudChoreographyConfig {

    // 1. PRODUTOR: Pega do SagaConfig e manda para o Broker
    @Bean
    public Supplier<Flux<EmployeeEvent>> employeeProducer(SagaConfig sagaConfig) {
        return () -> sagaConfig.getChoreoFlux()
                // LOG 1: Prova que o Spring Stream "ligou" a torneira
                .doOnSubscribe(s -> System.out.println("RABBIT [DEBUG]: O Spring Cloud Stream se inscreveu no Fluxo!"))
                // LOG 2: Prova que a mensagem está passando do Sink para o Stream
                .doOnNext(e -> System.out.println("RABBIT [DEBUG]: Mensagem capturada pelo Stream e indo para o Rabbit: " + e));
    }

    // 2. CONSUMIDOR: Recebe respostas do Broker
    @Bean
    public Consumer<EmployeeEvent> employeeResultConsumer() {
        return event -> {
            if (event instanceof EmployeeApproved) {
                System.out.println("COREOGRAFIA [FINAL]: Employee APROVADO e Salvo no DB.");
            } else if (event instanceof SalaryRejected rejected) {
                System.out.println("COREOGRAFIA [COMPENSAÇÃO]: Falha! Motivo: " + rejected.reason());
            }
        };
    }
}