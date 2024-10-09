package com.server.server.data;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class User {
    private String username;
    private String location;
    private String preferences; // 可以选择将其定义为 JSON 类型或 String
}
