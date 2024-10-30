package com.server.server.data;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
@NoArgsConstructor
@AllArgsConstructor
@Data
public class RoadUpdateMessage {
    private int userId;
    private long roadId;
    private double durationAdjustment;

}
