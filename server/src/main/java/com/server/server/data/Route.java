package com.server.server.data;
import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class Route {
    private int userId;
    private long startId;
    private long endId;
    private String distance;
    private String duration;
    private String price;    // 新增价格字段
    private List<RouteData> routeData;  // 使用 RouteData 存储多个路径点
    private String timestamp;
    private int priority;             // 初始优先级
    private LocalDateTime requestTime; // 请求进入系统的时间
    private int distanceWeight;    // 用户偏好权重：距离
    private int durationWeight;    // 用户偏好权重：时间
    private int priceWeight;       // 用户偏好权重：价格
    public int getUserId() {
        return userId;
    }
     public String getRouteDataJson() {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            return objectMapper.writeValueAsString(this.routeData);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return null;
        }
    }
    public void setRouteDataFromJson(String routeDataJson) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            this.routeData = objectMapper.readValue(routeDataJson, objectMapper.getTypeFactory().constructCollectionType(List.class, RouteData.class));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }
    public void setUserId(int userId) {
        this.userId = userId;
    }

    public List<RouteData> getrouteData() {
        return routeData;
    }

    public void setRouteData(List<RouteData> routeData) {
        this.routeData = routeData;
    }

    public LocalDateTime getRequestTime() {
        return requestTime;
    }

    public void setRequestTime(LocalDateTime requestTime) {
        this.requestTime = requestTime;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public String getDuration() {
        return duration;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }
}
