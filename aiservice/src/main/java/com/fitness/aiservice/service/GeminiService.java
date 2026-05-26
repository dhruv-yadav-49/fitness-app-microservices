package com.fitness.aiservice.service;

import org.springframework.stereotype.Service;
import com.fitness.aiservice.model.Activity;
import com.fitness.aiservice.model.Recommendation;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class GeminiService {

    public Recommendation generateRecommendation(Activity activity) {
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
}
