package com.server.server.data;
import lombok.Data;
import lombok.NoArgsConstructor;
@Data
@NoArgsConstructor
public class TrafficData {
    private int id;
    private int roadId; // 外键
    private int userId; // 外键
    private float speed;
    private String timestamp; // 可以用 java.util.Date 或 Timestamp 代替
}