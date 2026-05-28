package com.fitness.aiservice.service;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fitness.aiservice.model.Activity;
import com.fitness.aiservice.model.Recommendation;
import lombok.extern.slf4j.Slf4j;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class GeminiService {

    @Value("${gemini.api.key:}")
    private String apiKey;

    @Value("${gemini.api.url}")
    private String apiUrl;

    public Recommendation generateRecommendation(Activity activity) {
        if (apiKey == null || apiKey.trim().isEmpty() || apiKey.startsWith("${")) {
            log.warn("Gemini API key is not configured. Falling back to simulated recommendation.");
            return generateMockRecommendation(activity);
        }

        try {
            log.info("Generating AI recommendation using Gemini API for activity: {}", activity.getId());
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            String prompt = String.format(
                "Analyze the following fitness activity and provide personalized professional recommendations. " +
                "You must return the response as a JSON object matching this schema exactly:\n" +
                "{\n" +
                "  \"recommendation\": \"A concise, supportive overview recommendation message\",\n" +
                "  \"improvements\": [\"Concrete way to improve form, technique, or pacing\", \"Another improvement...\"],\n" +
                "  \"suggestions\": [\"Practical training suggestions or next steps\", \"Another suggestion...\"],\n" +
                "  \"safety\": [\"Safety tip or caution specific to this activity\", \"Another safety tip...\"]\n" +
                "}\n\n" +
                "Activity Details:\n" +
                "- Type: %s\n" +
                "- Duration: %d minutes\n" +
                "- Calories Burned: %d kcal\n" +
                "- Start Time: %s\n" +
                "- Additional Metrics: %s\n",
                activity.getType(),
                activity.getDuration() != null ? activity.getDuration() : 0,
                activity.getCaloriesBurned() != null ? activity.getCaloriesBurned() : 0,
                activity.getStartTime() != null ? activity.getStartTime().toString() : "N/A",
                activity.getAdditionalMetrics() != null ? activity.getAdditionalMetrics().toString() : "None"
            );

            GeminiRequest request = new GeminiRequest(prompt);
            HttpEntity<GeminiRequest> entity = new HttpEntity<>(request, headers);

            String requestUrl = apiUrl + "?key=" + apiKey;
            GeminiResponse response = restTemplate.postForObject(requestUrl, entity, GeminiResponse.class);

            if (response != null && response.getCandidates() != null && !response.getCandidates().isEmpty()) {
                String jsonText = response.getCandidates().get(0).getContent().getParts().get(0).getText();
                log.debug("Gemini raw JSON response: {}", jsonText);

                ObjectMapper objectMapper = new ObjectMapper();
                // Configure standard mapping
                Recommendation aiRec = objectMapper.readValue(jsonText, Recommendation.class);

                aiRec.setActivityId(activity.getId());
                aiRec.setUserId(activity.getUserId());
                if (aiRec.getCreatedAt() == null) {
                    aiRec.setCreatedAt(LocalDateTime.now());
                }
                log.info("Successfully generated recommendation using Gemini API.");
                return aiRec;
            } else {
                throw new RuntimeException("Empty response received from Gemini API");
            }
        } catch (Exception e) {
            log.error("Failed to generate recommendation via Gemini API. Falling back to simulated recommendation. Error: {}", e.getMessage());
            return generateMockRecommendation(activity);
        }
    }

    private Recommendation generateMockRecommendation(Activity activity) {
        String type = activity.getType() != null ? activity.getType().toLowerCase() : "general";
        
        List<String> improvements = new ArrayList<>();
        List<String> suggestions = new ArrayList<>();
        List<String> safety = new ArrayList<>();
        String recText;

        if (type.contains("run") || type.contains("jog") || type.contains("cardio")) {
            recText = "Great job on your cardio session! Consistent aerobic exercise builds heart strength and boosts endurance.";
            improvements.add("Maintain a consistent pace rather than frequent sprinting and resting.");
            improvements.add("Keep your chest upright and your shoulders relaxed to maximize lung expansion.");
            suggestions.add("Incorporate interval training: alternate between high and moderate intensity.");
            suggestions.add("Add active recovery or low-impact stretching post-run.");
            safety.add("Ensure proper footwear to avoid stress fractures or shin splints.");
            safety.add("Stay hydrated by drinking at least 250ml of water every 20 minutes.");
        } else if (type.contains("weight") || type.contains("strength") || type.contains("lift") || type.contains("gym")) {
            recText = "Excellent strength training session! Building muscle mass is crucial for metabolism and bone health.";
            improvements.add("Focus on controlled eccentric (lowering) phases rather than dropping the weight.");
            improvements.add("Ensure full range of motion on each repetition.");
            suggestions.add("Keep track of your reps and weight to apply progressive overload.");
            suggestions.add("Fuel up with high-quality protein within 2 hours after your session.");
            safety.add("Warm up properly and check your form before lifting heavy weights.");
            safety.add("Use a spotter or safety bars when performing exercises like bench press or squats.");
        } else if (type.contains("yoga") || type.contains("stretch") || type.contains("pilates")) {
            recText = "Wonderful job focusing on flexibility and core stabilization! This improves mobility and reduces injury risk.";
            improvements.add("Focus on steady, deep diaphragmatic breathing throughout the poses.");
            improvements.add("Align your joints carefully and don't force a stretch beyond comfort.");
            suggestions.add("Try practicing first thing in the morning to reduce stiffness.");
            suggestions.add("Combine yoga with meditation or mindfulness practice.");
            safety.add("Never bounce during a static stretch, as this can cause micro-tears in muscles.");
            safety.add("Modify positions or use blocks if your joints feel excessive pressure.");
        } else {
            recText = "Fantastic effort on staying active! Any movement is a win for your health and well-being.";
            improvements.add("Try to gradually increase either duration or intensity over time.");
            improvements.add("Pay attention to posture and alignment during activity.");
            suggestions.add("Aim for at least 150 minutes of moderate activity per week.");
            suggestions.add("Mix different kinds of activities to keep it exciting.");
            safety.add("Listen to your body; rest when you feel excessive fatigue or discomfort.");
            safety.add("Warm up before and cool down after any moderate-to-high intensity activity.");
        }

        return Recommendation.builder()
                .activityId(activity.getId())
                .userId(activity.getUserId())
                .recommendation(recText)
                .improvements(improvements)
                .suggestions(suggestions)
                .safety(safety)
                .createdAt(LocalDateTime.now())
                .build();
    }

    // --- Gemini DTOs ---

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GeminiRequest {
        private List<Content> contents;
        private GenerationConfig generationConfig;

        public GeminiRequest(String prompt) {
            this.contents = List.of(new Content(List.of(new Part(prompt))));
            this.generationConfig = new GenerationConfig("application/json");
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Content {
        private List<Part> parts;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Part {
        private String text;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GenerationConfig {
        private String responseMimeType;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GeminiResponse {
        private List<Candidate> candidates;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Candidate {
        private Content content;
    }
}
