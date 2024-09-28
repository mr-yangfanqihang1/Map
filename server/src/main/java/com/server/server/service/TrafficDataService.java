package com.server.server.service;
import com.server.server.data.*;
import com.server.server.mapper.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class TrafficDataService {
    @Autowired
    private TrafficDataMapper trafficDataMapper;

    public void uploadTrafficData(TrafficData trafficData) {
        trafficDataMapper.insertTrafficData(trafficData);
    }

    public List<TrafficData> getTrafficDataByRoadId(int roadId) {
        return trafficDataMapper.getTrafficDataByRoadId(roadId);
    }
}
