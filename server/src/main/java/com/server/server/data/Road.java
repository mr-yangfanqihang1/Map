package com.server.server.data;
import lombok.Data;
import lombok.NoArgsConstructor;
@Data
@NoArgsConstructor
public class Road {
    private int id;
    private String name;
    private String status; // 可以是 ENUM 类型
    private float load;
}
