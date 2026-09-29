package uk.newsbias.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

public record ArticleOutDto(
        Long id,
        @JsonProperty("outlet_slug")  String outletSlug,
        @JsonProperty("outlet_name")  String outletName,
        @JsonProperty("outlet_bias")  String outletBias,
        String title,
        String url,
        @JsonProperty("published_at") LocalDateTime publishedAt
) {}
