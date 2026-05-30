package com.yasirkhan.schedule.responses;

import lombok.Builder;
import lombok.Data;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class ResourceResponse {
    private List<VehicleOption> vehicles;
    private List<DriverOption> drivers;
    private List<RouteOption> routes;
    private List<TemplateOption> templates;

    @Data @Builder
    public static class VehicleOption {
        private String vehicleNo;
        private String status;
    }

    @Data @Builder
    public static class DriverOption {
        private UUID driverId;
        private String name;
        private String phoneNo;
        private String status;
    }

    @Data @Builder
    public static class RouteOption {
        private UUID routeId;
        private String origin;
        private String destination;
        private String status;
    }

    @Data @Builder
    public static class TemplateOption {
        private UUID templateId;
        private String shiftName;
        private LocalTime startTime;
        private LocalTime endTime;
        private String status;
    }
}