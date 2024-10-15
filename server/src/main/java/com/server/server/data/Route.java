package com.server.server.data;

import java.util.List;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class Route {
    private int userId;
    private String startPoint;
    private String endPoint;
    private String distance;
    private String duration;
    private String price;    // 新增价格字段
    private List<RouteData> pathData;  // 使用 RouteData 存储多个路径点
    private String timestamp;

    // 权重字段
    private double weightDistance;  // 距离权重
    private double weightDuration;  // 时间权重
    private double weightPrice;     // 价格权重
}
