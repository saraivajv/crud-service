package com.imd.crud_service.client;

import com.imd.crud_service.dto.ReviewDTO;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

@FeignClient(name = "ai-service")
public interface AiServiceClient {

    @CircuitBreaker(name = "aiServiceCircuitBreaker", fallbackMethod = "fallbackGenerateReview")
    @Retry(name = "aiServiceRetry")
    @PostMapping("/ai/reviews/generate/{employee_id}")
    ReviewDTO generateReview(@PathVariable("employee_id") Long employeeId);


    // MÉTODO DE FALLBACK
    default ReviewDTO fallbackGenerateReview(Long employeeId, Throwable t) {
        System.err.println("Fallback para generateReview ativado para o ID: " + employeeId + ". Causa: " + t.toString());
        ReviewDTO fallbackReview = new ReviewDTO();
        fallbackReview.setReviewText("Não foi possível gerar a avaliação de IA no momento. Tente novamente mais tarde.");
        return fallbackReview;
    }
}