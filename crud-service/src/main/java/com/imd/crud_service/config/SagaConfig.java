package com.imd.crud_service.config;

import com.imd.common.events.EmployeeEvent;
import com.imd.common.events.SagaCommand;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message; // Importante
import org.springframework.messaging.support.MessageBuilder; // Importante
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

@Configuration
public class SagaConfig {

    // 1. COREOGRAFIA
    private final Sinks.Many<EmployeeEvent> choreoSink = Sinks.many().multicast().onBackpressureBuffer();

    // 2. ORQUESTRAÇÃO
    private final Sinks.Many<Message<SagaCommand>> orchSink = Sinks.many().unicast().onBackpressureBuffer();

    // Método para enviar eventos de Coreografia
    // Aceita qualquer coisa que implemente EmployeeEvent (ex: EmployeeCreationRequested)
    public void sendEvent(EmployeeEvent event) {
        System.out.println("SagaConfig: Emitindo evento para o Sink: " + event);
        Sinks.EmitResult result = choreoSink.tryEmitNext(event);

        if (result.isFailure()) {
            System.err.println("Falha ao emitir evento: " + result);
        }
    }

    // --- MÉTODOS DE ORQUESTRAÇÃO ---

    public void sendCommand(SagaCommand command, String targetDestination) {
        System.out.println("SagaConfig: Roteando comando " + command.getClass().getSimpleName() + " para: " + targetDestination);

        Message<SagaCommand> message = MessageBuilder.withPayload(command)
                .setHeader("spring.cloud.stream.sendto.destination", targetDestination)
                .build();

        Sinks.EmitResult result = orchSink.tryEmitNext(message);
        if (result.isFailure()) System.err.println("Falha orch: " + result);
    }

    public void sendCommand(SagaCommand command) {
        sendCommand(command, "payroll-commands");
    }

    // --- GETTERS PARA OS CONFIGS ---
    public Flux<EmployeeEvent> getChoreoFlux() {
        return choreoSink.asFlux();
    }

    public Flux<Message<SagaCommand>> getOrchFlux() {
        return orchSink.asFlux();
    }
}