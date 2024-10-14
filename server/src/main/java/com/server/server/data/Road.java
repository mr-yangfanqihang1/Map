package com.server.server.data;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class Road {
    private String startPoint;
    private String endPoint;
    private double startLat;
    private double startLong;
    private double endLat;
    private double endLong;
    private double distance;
    private double duration;
    private double price;
    private String name;
    private String status;
    private int maxLoad;

    // 带经纬度的构造函数
    public Road(String startPoint, String endPoint, double startLat, double startLong, 
                double endLat, double endLong, double duration, double price,
                String name, String status, int maxLoad) {
        this.startPoint = startPoint;
        this.endPoint = endPoint;
        this.startLat = startLat;
        this.startLong = startLong;
        this.endLat = endLat;
        this.endLong = endLong;
        this.duration = duration;
        this.distance = calculateDistance(); // 直接调用方法计算距离

        // 如果价格不为 0，则根据距离计算价格
        if (Math.abs(price - 0.0) > 1e-10) {
            this.price = calculatePrice();
        } else {
            this.price = price;
        }

        this.name = name;
        this.status = status;
        this.maxLoad = maxLoad;
    }

    // 计算距离的方法
    private double calculateDistance() {
        double earthRadius = 6371; // 地球半径，单位千米

        double lat1 = Math.toRadians(startLat);
        double lon1 = Math.toRadians(startLong);
        double lat2 = Math.toRadians(endLat);
        double lon2 = Math.toRadians(endLong);

        double dlon = lon2 - lon1;
        double dlat = lat2 - lat1;

        double a = Math.pow(Math.sin(dlat / 2), 2) + 
                   Math.cos(lat1) * Math.cos(lat2) * Math.pow(Math.sin(dlon / 2), 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return earthRadius * c; // 返回距离，单位千米
    }

    // 计算价格的方法
    private double calculatePrice() {
        double pricePerKm = 1.0; // 每公里的价格，可以根据实际情况调整
        return pricePerKm * distance;
    }
}
