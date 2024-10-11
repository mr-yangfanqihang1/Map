package com.server.server.data;

import lombok.Data;
import lombok.NoArgsConstructor;
@Data
@NoArgsConstructor
public class PathData {
    private String roadName;  // 道路名称
    private double lat;  // 纬度
    private double lon;  // 经度
}

