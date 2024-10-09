package com.server.server.request.traffic;
import com.server.server.data.TrafficData;

public class TrafficDataUpdateRequest extends TrafficDataRequest {
    private final TrafficData trafficData;

    public TrafficDataUpdateRequest(TrafficData trafficData) {
        this.trafficData = trafficData;
    }

    public TrafficData getTrafficData() {
        return trafficData;
    }
}
