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
    private List<PathData> pathData;  // 处理多个路径点
    private List<RouteData> RouteData;
    private String timestamp;
    private double weightDistance;  // 前端传来的距离权重
    private double weightDuration;  // 前端传来的时间权重
    
}
