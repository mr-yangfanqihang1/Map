package com.server.server.request.traffic;
import com.server.server.data.TrafficData;
public class TrafficDataInsertRequest extends TrafficDataRequest {
    private final TrafficData trafficData;

    public TrafficDataInsertRequest(TrafficData trafficData) {
        this.trafficData = trafficData;
    }

    public TrafficData getTrafficData() {
        return trafficData;
    }
}
