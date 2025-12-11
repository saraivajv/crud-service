package com.imd.crud_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.util.function.Consumer;
import java.util.function.Supplier;

@Configuration
public class SagaConfig {
    // Sinks são "pontes" para enviar mensagens manualmente
    private final Sinks.Many<Object> sink = Sinks.many().multicast().onBackpressureBuffer();

    @Bean
    @Profile("choreography")
    public Supplier<Flux<Object>> employeeProducer() { return sink::asFlux; }

    @Bean @Profile("orchestration")
    public Supplier<Flux<Object>> sagaCommandProducer() { return sink::asFlux; }

    // Método genérico para enviar
    public void send(Object msg) { sink.tryEmitNext(msg); }

    // Consumers para ouvir respostas
    @Bean @Profile("choreography")
    public Consumer<Object> employeeResultConsumer() {
        return event -> System.out.println("COREOGRAFIA EVENTO: " + event);
    }
}
