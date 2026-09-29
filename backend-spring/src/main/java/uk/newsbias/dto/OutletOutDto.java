package uk.newsbias.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record OutletOutDto(
        String slug,
        String name,
        String bias,
        @JsonProperty("logo_filename") String logoFilename
) {}
