package com.server.server.data;

import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
public class Route {
    private int userId;
    private String startPoint;
    private String endPoint;
    private String distance;
    private String duration;
    private List<PathData> pathData;  // 处理多个路径点
    private String timestamp;
}
