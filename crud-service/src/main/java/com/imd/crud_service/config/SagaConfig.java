package com.imd.crud_service.config;

import com.imd.common.events.EmployeeEvent;
import com.imd.common.events.SagaCommand;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

@Configuration
public class SagaConfig {

    // Sinks configurados para aceitar a Interface Pai
    private final Sinks.Many<EmployeeEvent> choreoSink = Sinks.many().multicast().onBackpressureBuffer();
    private final Sinks.Many<SagaCommand> orchSink = Sinks.many().multicast().onBackpressureBuffer();

    // Método para enviar eventos de Coreografia
    // Aceita qualquer coisa que implemente EmployeeEvent (ex: EmployeeCreationRequested)
    public void sendEvent(EmployeeEvent event) {
        System.out.println("SagaConfig: Emitindo evento para o Sink: " + event);
        Sinks.EmitResult result = choreoSink.tryEmitNext(event);

        if (result.isFailure()) {
            System.err.println("Falha ao emitir evento: " + result);
        }
    }

    // Método para enviar comandos de Orquestração
    public void sendCommand(SagaCommand command) {
        System.out.println("SagaConfig: Emitindo comando para o Sink: " + command);
        Sinks.EmitResult result = orchSink.tryEmitNext(command);

        if (result.isFailure()) {
            System.err.println("Falha ao emitir comando: " + result);
        }
    }

    // Getters para as classes de Configuração (@Profile) consumirem
    public Flux<EmployeeEvent> getChoreoFlux() {
        return choreoSink.asFlux();
    }

    public Flux<SagaCommand> getOrchFlux() {
        return orchSink.asFlux();
    }
}