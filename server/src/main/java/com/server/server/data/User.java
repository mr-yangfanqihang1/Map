package com.server.server.data;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class User {
    private int id=0;
    private String username;
    private String preferences;
}
