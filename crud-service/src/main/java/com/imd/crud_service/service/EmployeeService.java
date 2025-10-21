package com.imd.crud_service.service;

import com.imd.crud_service.dto.EmployeeDTO;
import com.imd.crud_service.dto.ReviewDTO;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class EmployeeService {

    private final WebClient.Builder webClientBuilder;
    private final String DB_SERVICE_URL = "http://db-service/db/employees";
    private final String AI_SERVICE_URL = "http://ai-service/ai/reviews";

    public EmployeeService(WebClient.Builder webClientBuilder) {
        this.webClientBuilder = webClientBuilder;
    }

    public Mono<EmployeeDTO> createEmployee(EmployeeDTO employee) {
        return webClientBuilder.build().post()
                .uri(DB_SERVICE_URL)
                .bodyValue(employee)
                .retrieve()
                .bodyToMono(EmployeeDTO.class);
    }

    public Flux<EmployeeDTO> getAllEmployees() {
        return webClientBuilder.build().get()
                .uri(DB_SERVICE_URL)
                .retrieve()
                .bodyToFlux(EmployeeDTO.class);
    }

    public Mono<EmployeeDTO> getEmployeeById(Long id) {
        return webClientBuilder.build().get()
                .uri(DB_SERVICE_URL + "/{id}", id)
                .retrieve()
                .bodyToMono(EmployeeDTO.class);
    }

    public Mono<EmployeeDTO> updateEmployee(Long id, EmployeeDTO employeeDetails) {
        return webClientBuilder.build().put()
                .uri(DB_SERVICE_URL + "/{id}", id)
                .bodyValue(employeeDetails)
                .retrieve()
                .bodyToMono(EmployeeDTO.class);
    }

    public Mono<Void> deleteEmployee(Long id) {
        return webClientBuilder.build().delete()
                .uri(DB_SERVICE_URL + "/{id}", id)
                .retrieve()
                .bodyToMono(Void.class);
    }

    public Mono<EmployeeDTO> generateAndSaveReview(Long employeeId) {
        // 1. Chamar ai-service para gerar a avaliação de forma não-blocante
        Mono<ReviewDTO> reviewMono = webClientBuilder.build().post()
                .uri(AI_SERVICE_URL + "/generate/{employeeId}", employeeId)
                .retrieve()
                .bodyToMono(ReviewDTO.class);

        // 2. Usar flatMap para encadear a próxima chamada assíncrona: salvar no db-service
        return reviewMono.flatMap(reviewDTO ->
                webClientBuilder.build().post()
                        .uri(DB_SERVICE_URL + "/{id}/review", employeeId)
                        .bodyValue(reviewDTO)
                        .retrieve()
                        .bodyToMono(EmployeeDTO.class)
        );
    }
}

