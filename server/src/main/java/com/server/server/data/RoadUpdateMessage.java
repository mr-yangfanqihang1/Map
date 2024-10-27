package com.server.server.data;

public class RoadUpdateMessage {
    private long roadId;
    private double durationAdjustment;

    public RoadUpdateMessage(long roadId, double durationAdjustment) {
        this.roadId = roadId;
        this.durationAdjustment = durationAdjustment;
    }

    public long getRoadId() {
        return roadId;
    }

    public void setRoadId(long roadId) {
        this.roadId = roadId;
    }

    public double getDurationAdjustment() {
        return durationAdjustment;
    }

    public void setDurationAdjustment(double durationAdjustment) {
        this.durationAdjustment = durationAdjustment;
    }
}
