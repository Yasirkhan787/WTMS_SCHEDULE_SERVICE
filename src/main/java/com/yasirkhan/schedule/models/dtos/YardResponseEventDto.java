package com.yasirkhan.schedule.models.dtos;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.yasirkhan.schedule.models.enums.EventStatus;
import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class YardResponseEventDto {

    private EventStatus eventTypeStatus; // SUCCESS, FAILURE

    private UUID yardId;

    private String yardName; // e.g., "I-9 Sector Transfer Station", "Losar Landfill"

    private String yardType; // Enum: COLLECTION_POINT, DUMP_SITE, PARKING_DEPOT

    // Identifies which UI component to render on the frontend map
    private String boundaryType;   // RADIUS, POLYGON

    private UUID tehsilId;

    private String tehsilName;

    // --- GEOGRAPHIC DATA (PostGIS) ---
/*
     // For RADIUS method
    private CoordinateDto centerCoords;
    private Double radiusMeters;

    // For POLYGON method
    private String polygonPath;

 */

    private String status;

    @JsonProperty("yardData")
    private void unpackNestedRouteData(Map<String, Object> yardData) {
        if (yardData != null) {
            this.yardId = UUID.fromString((String) yardData.get("yardId"));
            this.yardName = (String) yardData.get("yardName");
            this.yardType = (String) yardData.get("yardType");
            this.boundaryType = (String) yardData.get("boundaryType");
            this.tehsilId = UUID.fromString((String) yardData.get("tehsilId"));
            this.tehsilName = (String) yardData.get("tehsilName");
            this.status = (String) yardData.get("status");
        }
    }
}
