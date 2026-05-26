package com.fitness.aiservice.service;

import org.springframework.stereotype.Service;

import com.fitness.aiservice.model.Activity;
import com.fitness.aiservice.model.Recommendation;
import com.fitness.aiservice.repository.RecommendationRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class RecommendationService {

    private final RecommendationRepository recommendationRepository;
    private final GeminiService geminiService;

    public void processActivityAndCreateRecommendation(Activity activity) {
        log.info("Processing activity for user: {}, type: {}", activity.getUserId(), activity.getType());
        try {
            Recommendation recommendation = geminiService.generateRecommendation(activity);
            recommendationRepository.save(recommendation);
            log.info("Saved AI recommendation successfully for activity: {}", activity.getId());
        } catch (Exception e) {
            log.error("Failed to generate or save AI recommendation for activity: " + activity.getId(), e);
        }
    }
}
