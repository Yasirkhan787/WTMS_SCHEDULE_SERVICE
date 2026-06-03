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
public class UserResponseEventDto {

    private EventStatus eventTypeStatus; // SUCCESS, FAILURE

    private UUID userId;
    private String name;
    private String phoneNo;
    private String role;
    private String status;

    @JsonProperty("userData")
    private void unpackNestedRouteData(Map<String, Object> routeData) {
        if (routeData != null) {
            this.userId = UUID.fromString((String) routeData.get("userId"));
            this.name = (String) routeData.get("name");
            this.phoneNo = (String) routeData.get("phoneNo");
            this.role = (String) routeData.get("role");
            this.status = (String) routeData.get("status");
        }
    }
}
