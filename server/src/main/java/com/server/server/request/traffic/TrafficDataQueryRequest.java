package com.server.server.request.traffic;

public class TrafficDataQueryRequest extends TrafficDataRequest {
    private final int roadId;

    public TrafficDataQueryRequest(int roadId) {
        this.roadId = roadId;
    }

    public int getRoadId() {
        return roadId;
    }
}