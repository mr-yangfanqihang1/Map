package com.server.server.request.traffic;
import com.server.server.data.TrafficData;
public class TrafficDataInsertRequest extends TrafficDataRequest {
    private final TrafficData trafficData;

    public TrafficDataInsertRequest(TrafficData trafficData) {
        this.trafficData = trafficData;
        super.setPriority(2);
    }

    public TrafficData getTrafficData() {
        return trafficData;
    }
}
