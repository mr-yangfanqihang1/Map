package com.server.server.data;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class RouteData {
    private long roadId;
    private double startLat;
    private double startLong;
    private double endLat;
    private double endLong;
    private double distance;  // 距离
    private double duration;  // 时间
    private double price;  
    private String status;    // 价格
    public RouteData(double startLat, double startLong, double endLat, double endLong, double distance, double duration, double price,String status) {
        this.startLat = startLat;
        this.startLong = startLong;
        this.endLat = endLat;
        this.endLong = endLong;
        this.distance = distance;
        this.duration = duration;
        this.price = price;
        this.status=status;

    }
}