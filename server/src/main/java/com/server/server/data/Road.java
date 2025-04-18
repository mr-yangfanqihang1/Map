package com.server.server.data;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class Road {
    private long id;           // id 主键
    private String startName;  // 起点名称
    private String name;       // 道路名称
    private String status;     // 道路状态，enum('红','橙','绿')
    public int maxLoad;       // 最大载重
    private double startLat;   // 起点纬度
    private double startLong;  // 起点经度
    private double endLat;     // 终点纬度
    private double endLong;    // 终点经度
    private double endLat2;     // 终点纬度
    private double endLong2;    // 终点经度
    private double endLat3;     // 终点纬度
    private double endLong3;    // 终点经度
    private double distance;   // 距离
    private double duration;   // 时间
    private double price;      // 价格
    private String nextRoadId; // 下一道路ID

    // 带经纬度的构造函数
    public Road(String startName, String name, double startLat, double startLong, 
                double endLat, double endLong, double pricePerKm, 
                String status, int maxLoad, String nextRoadId) {
        this.startName = startName;
        this.name = name;
        this.startLat = startLat;
        this.startLong = startLong;
        this.endLat = endLat;
        this.endLong = endLong;
        
        // 计算距离并赋值
        this.distance = calculateDistance();

        // 根据距离计算价格
        if (Math.abs(pricePerKm - 0.0) > 1e-10) {
            this.price = pricePerKm * this.distance;
        } else {
            this.price = 0;
        }

        // 计算时间 (分钟)，假设速度为 30 km/h
        this.duration = this.distance*2;

        this.status = status;
        this.maxLoad = maxLoad;
        this.nextRoadId = nextRoadId;
    }


    // 计算距离的方法
    double calculateDistance() {
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
    public long getId() {
        return id;
    }

    public double getStartLat() {
        return startLat;
    }

    public double getStartLong() {return startLong;}

    public double getEndLat() {
        return endLat;
    }

    public double getEndLong() {
        return endLong;
    }

    public double getDistance() {
        return distance;
    }

    public double getDuration() {
        return duration;
    }

    public double getPrice() {
        return price;
    }

    public String getName() {
        return name;
    }
}
