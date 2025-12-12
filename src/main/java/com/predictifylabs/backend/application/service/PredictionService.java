package com.predictifylabs.backend.application.service;

import com.predictifylabs.backend.domain.model.PredictionLevel;
import com.predictifylabs.backend.domain.model.PredictionTrend;
import com.predictifylabs.backend.infrastructure.adapters.input.rest.dto.prediction.PredictionDTO;
import com.predictifylabs.backend.infrastructure.adapters.input.rest.dto.prediction.PredictionFactorDTO;
import com.predictifylabs.backend.infrastructure.adapters.output.persistence.entity.EventEntity;
import com.predictifylabs.backend.infrastructure.adapters.output.persistence.entity.EventPredictionEntity;
import com.predictifylabs.backend.infrastructure.adapters.output.persistence.repository.EventPredictionRepository;
import com.predictifylabs.backend.infrastructure.adapters.output.persistence.repository.EventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Service for event attendance prediction operations
 * Integrates with AI service for enhanced predictions
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class PredictionService {

    private final EventPredictionRepository predictionRepository;
    private final EventRepository eventRepository;
    private final AiService aiService;

    /**
     * Get the latest prediction for an event
     */
    public PredictionDTO getEventPrediction(UUID eventId) {
        return predictionRepository.findLatestByEventId(eventId)
                .map(this::toDTO)
                .orElse(null);
    }

    /**
     * Generate a new prediction for an event
     */
    @Transactional
    public PredictionDTO generatePrediction(UUID eventId) {
        log.info("Generating prediction for event {}", eventId);

        var event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found with id: " + eventId));

        // Calculate prediction factors
        var factors = calculatePredictionFactors(event);

        // Calculate overall probability based on factors
        int baseProbability = calculateBaseProbability(event, factors);
        
        // Determine prediction level
        PredictionLevel level = determinePredictionLevel(baseProbability);

        // Calculate attendance estimates
        int capacity = event.getCapacity();
        int currentRegistrations = event.getRegisteredCount();
        int estimatedExpected = calculateExpectedAttendance(capacity, baseProbability, currentRegistrations);
        int estimatedMin = Math.max(currentRegistrations, (int) (estimatedExpected * 0.7));
        int estimatedMax = Math.min(capacity, (int) (estimatedExpected * 1.3));

        // Determine trend
        PredictionTrend trend = determineTrend(event);

        // Create prediction entity
        var prediction = EventPredictionEntity.builder()
                .event(event)
                .probability((short) baseProbability)
                .level(level)
                .confidence(calculateConfidence(event))
                .estimatedMin(estimatedMin)
                .estimatedMax(estimatedMax)
                .estimatedExpected(estimatedExpected)
                .trend(trend)
                .trendChange(BigDecimal.valueOf(calculateTrendChange(event)))
                .calculatedAt(OffsetDateTime.now())
                .build();

        var saved = predictionRepository.save(prediction);
        log.info("Prediction generated for event {}: probability={}, level={}", eventId, baseProbability, level);

        return toDTO(saved, factors);
    }

    /**
     * Generate AI-enhanced description for prediction context
     */
    public String generatePredictionInsight(UUID eventId) {
        var event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found with id: " + eventId));

        var prediction = predictionRepository.findLatestByEventId(eventId)
                .orElse(null);

        String context = buildPredictionContext(event, prediction);

        try {
            return aiService.generateText(
                    "Based on the following event data, provide a brief insight (2-3 sentences) about the expected attendance:\n" + context);
        } catch (Exception e) {
            log.error("Failed to generate AI insight for event {}", eventId, e);
            return "Prediction analysis is currently unavailable.";
        }
    }

    // Helper methods
    private List<PredictionFactorDTO> calculatePredictionFactors(EventEntity event) {
        List<PredictionFactorDTO> factors = new ArrayList<>();

        // Time until event factor
        long daysUntil = ChronoUnit.DAYS.between(java.time.LocalDate.now(), event.getStartDate());
        factors.add(PredictionFactorDTO.builder()
                .name("Time Until Event")
                .type(com.predictifylabs.backend.domain.model.FactorType.POSITIVE)
                .impact(daysUntil < 7 ? com.predictifylabs.backend.domain.model.FactorImpact.HIGH :
                        daysUntil < 30 ? com.predictifylabs.backend.domain.model.FactorImpact.MEDIUM :
                                com.predictifylabs.backend.domain.model.FactorImpact.LOW)
                .weight(BigDecimal.valueOf(0.2))
                .score(BigDecimal.valueOf(Math.max(0, 100 - daysUntil)))
                .description(daysUntil + " days until event")
                .build());

        // Registration rate factor
        double registrationRate = event.getCapacity() > 0 ?
                (double) event.getRegisteredCount() / event.getCapacity() * 100 : 0;
        factors.add(PredictionFactorDTO.builder()
                .name("Registration Rate")
                .type(com.predictifylabs.backend.domain.model.FactorType.POSITIVE)
                .impact(registrationRate > 70 ? com.predictifylabs.backend.domain.model.FactorImpact.HIGH :
                        registrationRate > 40 ? com.predictifylabs.backend.domain.model.FactorImpact.MEDIUM :
                                com.predictifylabs.backend.domain.model.FactorImpact.LOW)
                .weight(BigDecimal.valueOf(0.3))
                .score(BigDecimal.valueOf(registrationRate))
                .description(String.format("%.1f%% of capacity registered", registrationRate))
                .build());

        // Interest factor
        int interestScore = event.getInterestedCount() > 0 ?
                Math.min(100, event.getInterestedCount() * 2) : 0;
        factors.add(PredictionFactorDTO.builder()
                .name("Interest Level")
                .type(com.predictifylabs.backend.domain.model.FactorType.POSITIVE)
                .impact(interestScore > 50 ? com.predictifylabs.backend.domain.model.FactorImpact.HIGH :
                        interestScore > 20 ? com.predictifylabs.backend.domain.model.FactorImpact.MEDIUM :
                                com.predictifylabs.backend.domain.model.FactorImpact.LOW)
                .weight(BigDecimal.valueOf(0.15))
                .score(BigDecimal.valueOf(interestScore))
                .description(event.getInterestedCount() + " people interested")
                .build());

        // Views factor
        int viewsScore = event.getViewsCount() > 0 ?
                Math.min(100, event.getViewsCount() / 10) : 0;
        factors.add(PredictionFactorDTO.builder()
                .name("Visibility")
                .type(com.predictifylabs.backend.domain.model.FactorType.POSITIVE)
                .impact(viewsScore > 50 ? com.predictifylabs.backend.domain.model.FactorImpact.HIGH :
                        viewsScore > 20 ? com.predictifylabs.backend.domain.model.FactorImpact.MEDIUM :
                                com.predictifylabs.backend.domain.model.FactorImpact.LOW)
                .weight(BigDecimal.valueOf(0.1))
                .score(BigDecimal.valueOf(viewsScore))
                .description(event.getViewsCount() + " views")
                .build());

        // Price factor
        boolean isFree = event.getIsFree() != null && event.getIsFree();
        int priceScore = isFree ? 80 : 50;
        factors.add(PredictionFactorDTO.builder()
                .name("Price Accessibility")
                .type(isFree ? com.predictifylabs.backend.domain.model.FactorType.POSITIVE :
                        com.predictifylabs.backend.domain.model.FactorType.NEUTRAL)
                .impact(isFree ? com.predictifylabs.backend.domain.model.FactorImpact.HIGH :
                        com.predictifylabs.backend.domain.model.FactorImpact.MEDIUM)
                .weight(BigDecimal.valueOf(0.15))
                .score(BigDecimal.valueOf(priceScore))
                .description(isFree ? "Free event" : "Paid event")
                .build());

        // Featured/Trending factor
        boolean isFeatured = event.getIsFeatured() != null && event.getIsFeatured();
        boolean isTrending = event.getIsTrending() != null && event.getIsTrending();
        int promotionScore = (isFeatured ? 50 : 0) + (isTrending ? 50 : 0);
        factors.add(PredictionFactorDTO.builder()
                .name("Promotion Status")
                .type(promotionScore > 0 ? com.predictifylabs.backend.domain.model.FactorType.POSITIVE :
                        com.predictifylabs.backend.domain.model.FactorType.NEUTRAL)
                .impact(promotionScore > 50 ? com.predictifylabs.backend.domain.model.FactorImpact.HIGH :
                        promotionScore > 0 ? com.predictifylabs.backend.domain.model.FactorImpact.MEDIUM :
                                com.predictifylabs.backend.domain.model.FactorImpact.LOW)
                .weight(BigDecimal.valueOf(0.1))
                .score(BigDecimal.valueOf(promotionScore))
                .description(isFeatured ? "Featured event" : (isTrending ? "Trending event" : "Standard listing"))
                .build());

        return factors;
    }

    private int calculateBaseProbability(EventEntity event, List<PredictionFactorDTO> factors) {
        double weightedSum = 0;
        double totalWeight = 0;

        for (var factor : factors) {
            weightedSum += factor.score().doubleValue() * factor.weight().doubleValue();
            totalWeight += factor.weight().doubleValue();
        }

        return totalWeight > 0 ? (int) (weightedSum / totalWeight) : 50;
    }

    private PredictionLevel determinePredictionLevel(int probability) {
        if (probability >= 65) return PredictionLevel.HIGH;
        if (probability >= 35) return PredictionLevel.MEDIUM;
        return PredictionLevel.LOW;
    }

    private int calculateExpectedAttendance(int capacity, int probability, int currentRegistrations) {
        int baseExpected = (capacity * probability) / 100;
        // Weight current registrations more heavily
        return (baseExpected + currentRegistrations * 2) / 3;
    }

    private short calculateConfidence(EventEntity event) {
        // Higher confidence with more data (registrations, views, time)
        int registrationData = Math.min(30, event.getRegisteredCount() / 2);
        int viewData = Math.min(20, event.getViewsCount() / 50);
        int interestData = Math.min(20, event.getInterestedCount());
        int baseConfidence = 30;

        return (short) Math.min(100, baseConfidence + registrationData + viewData + interestData);
    }

    private PredictionTrend determineTrend(EventEntity event) {
        // Simple trend based on recent activity
        if (event.getRegisteredCount() > event.getCapacity() * 0.5) {
            return PredictionTrend.UP;
        } else if (event.getRegisteredCount() < event.getCapacity() * 0.2) {
            return PredictionTrend.DOWN;
        }
        return PredictionTrend.STABLE;
    }

    private double calculateTrendChange(EventEntity event) {
        // Simplified trend change calculation
        double registrationRate = event.getCapacity() > 0 ?
                (double) event.getRegisteredCount() / event.getCapacity() * 100 : 0;
        return registrationRate > 50 ? 5.0 : (registrationRate > 25 ? 0.0 : -3.0);
    }

    private String buildPredictionContext(EventEntity event, EventPredictionEntity prediction) {
        StringBuilder context = new StringBuilder();
        context.append("Event: ").append(event.getTitle()).append("\n");
        context.append("Category: ").append(event.getCategory()).append("\n");
        context.append("Type: ").append(event.getType()).append("\n");
        context.append("Capacity: ").append(event.getCapacity()).append("\n");
        context.append("Registered: ").append(event.getRegisteredCount()).append("\n");
        context.append("Interested: ").append(event.getInterestedCount()).append("\n");
        context.append("Views: ").append(event.getViewsCount()).append("\n");
        context.append("Is Free: ").append(event.getIsFree()).append("\n");
        context.append("Start Date: ").append(event.getStartDate()).append("\n");

        if (prediction != null) {
            context.append("Predicted Probability: ").append(prediction.getProbability()).append("%\n");
            context.append("Prediction Level: ").append(prediction.getLevel()).append("\n");
        }

        return context.toString();
    }

    private PredictionDTO toDTO(EventPredictionEntity prediction) {
        return toDTO(prediction, new ArrayList<>());
    }

    private PredictionDTO toDTO(EventPredictionEntity prediction, List<PredictionFactorDTO> factors) {
        return PredictionDTO.builder()
                .id(prediction.getId())
                .eventId(prediction.getEvent().getId())
                .probability(prediction.getProbability())
                .level(prediction.getLevel())
                .confidence(prediction.getConfidence())
                .estimatedMin(prediction.getEstimatedMin())
                .estimatedMax(prediction.getEstimatedMax())
                .estimatedExpected(prediction.getEstimatedExpected())
                .trend(prediction.getTrend())
                .trendChange(prediction.getTrendChange())
                .calculatedAt(prediction.getCalculatedAt())
                .factors(factors)
                .build();
    }
}
