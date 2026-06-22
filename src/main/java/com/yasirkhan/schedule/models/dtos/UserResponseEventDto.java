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

    private EventStatus eventTypeStatus;

    private UUID userId;
    private UUID tehsilId;
    private UUID yardId;
    private String name;
    private String phoneNo;
    private String role;
    private String status;

    @JsonProperty("userData")
    private void unpackNestedUserData(Map<String, Object> userData) {
        if (userData != null) {

            if (userData.get("userId") != null) {
                this.userId = UUID.fromString((String) userData.get("userId"));
            }

            if (userData.get("tehsilId") != null) {
                this.tehsilId = UUID.fromString((String) userData.get("tehsilId"));
            } else {
                this.tehsilId = null;
            }

            if (userData.get("yardId") != null) {
                this.yardId = UUID.fromString((String) userData.get("yardId"));
            } else {
                this.yardId = null;
            }

            this.name = (String) userData.get("name");
            this.phoneNo = (String) userData.get("phoneNo");
            this.role = (String) userData.get("role");
            this.status = (String) userData.get("status");
        }
    }
}