package com.server.server.service;
import com.server.server.data.*;
import com.server.server.mapper.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class RoadService {
    @Autowired
    private RoadMapper roadMapper;

    public void createRoad(Road road) {
        roadMapper.insertRoad(road);
    }

    public List<Road> getAllRoads() {
        return roadMapper.getAllRoads();
    }

    public Road getRoadById(int id) {
        return roadMapper.getRoadById(id);
    }
}
