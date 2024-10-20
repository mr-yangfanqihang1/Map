package com.server.server.data;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RoadTrafficData {
    private long roadId;
    private int userCount;
    private int maxLoad;
    private String status;
    private double averageSpeed;
    // getters and setters
}

