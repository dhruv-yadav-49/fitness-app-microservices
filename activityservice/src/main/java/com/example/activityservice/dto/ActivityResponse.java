package com.example.activityservice.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

import com.example.activityservice.model.ActivityType;

@Data
public class ActivityResponse {
    private String id;
    private String userId;
    private ActivityType type;
    private Integer duration;
    private Integer caloriesBurned;
    private LocalDateTime startTime;
    private Map<String, Object> additionalMetrics;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}