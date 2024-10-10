package com.server.server.request.traffic;
import com.server.server.data.TrafficData;

public class TrafficDataUpdateRequest extends TrafficDataRequest {
    private final TrafficData trafficData;

    public TrafficDataUpdateRequest(TrafficData trafficData) {
        this.trafficData = trafficData;
        super.setPriority(2);

    }

    public TrafficData getTrafficData() {
        return trafficData;
    }
}
