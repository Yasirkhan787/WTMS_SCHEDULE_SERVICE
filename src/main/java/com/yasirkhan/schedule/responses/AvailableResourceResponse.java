package com.yasirkhan.schedule.responses;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class AvailableResourceResponse {
    private List<ResourceResponse.TemplateOption> activeTemplates;
    private List<ResourceResponse.RouteOption> activeRoutes;
}