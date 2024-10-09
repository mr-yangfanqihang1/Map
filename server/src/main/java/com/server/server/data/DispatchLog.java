package com.server.server.data;
import lombok.Data;
import lombok.NoArgsConstructor;
@Data
@NoArgsConstructor
public class DispatchLog {
    private String timestamp;
    private String action;
}