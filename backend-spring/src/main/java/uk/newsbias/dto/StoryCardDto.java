package uk.newsbias.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.List;

public record StoryCardDto(
        Long id,
        String title,
        @JsonProperty("first_seen_at")    LocalDateTime firstSeenAt,
        @JsonProperty("last_seen_at")     LocalDateTime lastSeenAt,
        @JsonProperty("article_count")    int articleCount,
        @JsonProperty("left_count")       int leftCount,
        @JsonProperty("centre_count")     int centreCount,
        @JsonProperty("right_count")      int rightCount,
        @JsonProperty("is_blind_spot")    boolean isBlindSpot,
        @JsonProperty("blind_spot_side")  String blindSpotSide,
        @JsonProperty("outlets_covered")  List<String> outletsCovered
) {}
