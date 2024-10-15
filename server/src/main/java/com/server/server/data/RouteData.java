package com.server.server.data;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class RouteData {
    private double distance;  // 距离
    private double duration;  // 时间
    private double price;     // 价格

    public RouteData(double distance, double duration, double price) {
        this.distance = distance;
        this.duration = duration;
        this.price = price;
    }
}