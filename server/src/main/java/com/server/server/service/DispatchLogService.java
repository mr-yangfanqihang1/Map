package com.server.server.service;
import com.server.server.data.*;
import com.server.server.mapper.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
@Service
public class DispatchLogService {
    @Autowired
    private DispatchLogMapper dispatchLogMapper;

    public void createDispatchLog(DispatchLog log) {
        dispatchLogMapper.insertDispatchLog(log);
    }

    public List<DispatchLog> getAllLogs() {
        return dispatchLogMapper.getAllLogs();
    }
}

