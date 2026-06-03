package com.yasirkhan.schedule.responses;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class AvailableAssetResponse {
    private List<ResourceResponse.VehicleOption> availableVehicles;
    private List<ResourceResponse.DriverOption> availableDrivers;
}