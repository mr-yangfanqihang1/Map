package com.server.server.data;
import lombok.Data;
import lombok.NoArgsConstructor;
@Data
@NoArgsConstructor
public class Route {
    private String startPoint;
    private String endPoint;
    private String pathData;
    private String timestamp;
}
