package uk.newsbias.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;
import java.util.Map;

@Data
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private String modelPath = "src/main/resources/model";
    private int fetchIntervalMinutes = 30;
    private int rollingWindowHours = 48;
    private double similarityThreshold = 0.75;
    private List<OutletDef> outlets = List.of();

    @Data
    public static class OutletDef {
        private String name;
        private String slug;
        private String bias;
        private String rssUrl;
        private String logoFilename;
    }

    /** Map five bias values to three display buckets. */
    public static String biasBucket(String bias) {
        return switch (bias) {
            case "left", "centre_left"  -> "left";
            case "centre_right", "centre" -> bias.equals("centre") ? "centre" : "centre";
            case "right"                -> "right";
            default                     -> "centre";
        };
    }

    /** Collapsed to left / centre / right. */
    public static String bucket(String bias) {
        return switch (bias) {
            case "left", "centre_left"   -> "left";
            case "centre", "centre_right" -> "centre";
            case "right"                 -> "right";
            default                      -> "centre";
        };
    }
}
