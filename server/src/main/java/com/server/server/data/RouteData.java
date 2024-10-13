package com.server.server.data;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class RouteData {
    private double distance;  // 距离
    private double duration;  // 时间

    public RouteData(double distance, double duration) {
        this.distance = distance;
        this.duration = duration;
    }
}