package com.server.server.data;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class RoadTrafficData {
    private long roadId;
    private int userCount;
    private int maxLoad;
    private String status;
    // getters and setters
}

