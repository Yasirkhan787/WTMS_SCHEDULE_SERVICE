package com.yasirkhan.schedule.models.dtos;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.yasirkhan.schedule.models.enums.EventStatus;
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
public class RouteResponseEventDto {

    private EventStatus eventTypeStatus; // SUCCESS, FAILURE

    private UUID routeId;
    private String routeName;
    private String routePath;
    private UUID tehsilId;
    private String tehsilName;
    private UUID sourceYardId;
    private String sourceYardName;
    private String sourceYardType;
    private UUID destinationYardId;
    private String destinationYardType;
    private String destinationYardName;
    private String status;

    @JsonProperty("routeData")
    private void unpackNestedRouteData(Map<String, Object> routeData) {
        if (routeData != null) {
            this.routeId = UUID.fromString((String) routeData.get("routeId"));
            this.routeName = (String) routeData.get("routeName");
            this.routePath = (String) routeData.get("routePath");
            this.tehsilId = (UUID.fromString((String) routeData.get("tehsilId")));
            this.tehsilName = (String) routeData.get("tehsilName");
            this.sourceYardId = (UUID.fromString((String) routeData.get("sourceYardId")));
            this.sourceYardName = (String) routeData.get("sourceYardName");
            this.sourceYardType = (String) routeData.get("sourceYardType");
            this.destinationYardId = (UUID.fromString((String) routeData.get("destinationYardId")));
            this.destinationYardType = (String) routeData.get("destinationYardType");
            this.destinationYardName = (String) routeData.get("destinationYardName");
            this.status = (String) routeData.get("status");
        }
    }
}
