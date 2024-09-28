package com.server.server.data;
import lombok.Data;
import lombok.NoArgsConstructor;
@Data
@NoArgsConstructor
public class Route {
    private int id;
    private String startPoint;
    private String endPoint;
    private String pathData; // 可以选择将其定义为 JSON 类型或 String
    private String timestamp; // 可以用 java.util.Date 或 Timestamp 代替
}
