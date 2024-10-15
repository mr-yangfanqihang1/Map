package com.server.server.data;

import java.time.LocalDateTime;
import java.util.List;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class Route {
    private int id; // 路由请求的唯一标识
    private int userId;
    private String startPointname;
    private String endPointname;
    private double distance;
    private double duration;
    private List<RouteData> pathData;  // 处理多个路径点
    private String timestamp;

    // 动态优先级调度相关字段
    private int priority;             // 初始优先级
    private LocalDateTime requestTime; // 请求进入系统的时间
    private int dynamicPriority;      // 动态优先级
    private int distanceWeight;    // 用户偏好权重：距离
    private int durationWeight;    // 用户偏好权重：时间
    private int priceWeight;       // 用户偏好权重：价格
}