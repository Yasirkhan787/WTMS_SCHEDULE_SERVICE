package com.yasirkhan.schedule.services;

import com.yasirkhan.schedule.requests.AvailableAssetRequest;
import com.yasirkhan.schedule.responses.AvailableAssetResponse;
import com.yasirkhan.schedule.responses.AvailableResourceResponse;
import com.yasirkhan.schedule.responses.ResourceResponse;

import java.util.List;

public interface ResourceService {

    AvailableAssetResponse getAvailableAssets(AvailableAssetRequest request);

    AvailableResourceResponse getAvailableResources();
}
