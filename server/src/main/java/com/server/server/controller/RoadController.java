package com.server.server.controller;
import com.server.server.service.RoadService;
import com.server.server.service.RoadStatusService;
import com.server.server.data.Road;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/roads")
public class RoadController {

    private final RoadService roadService;
    private final RoadStatusService roadStatusService;

    // 使用构造器注入，确保依赖关系正确注入
    public RoadController(RoadService roadService, RoadStatusService roadStatusService) {
        this.roadService = roadService;
        this.roadStatusService = roadStatusService;
    }

    /**
     * 获取某条道路的状态
     * @param roadId 道路的唯一标识符
     * @return 道路的状态信息
     */
    @GetMapping("/status/{roadId}")
    public String getRoadStatus(@PathVariable int roadId) {
        if (roadStatusService.isRoadStatusCached(roadId)) {
            return "Road ID " + roadId + " status: " + roadStatusService.getRoadStatus(roadId);
        } else {
            return "Road ID " + roadId + " status not cached in Redis";
        }
    }

    /**
     * 创建新道路
     * @param road 道路实体对象
     */
    @PostMapping("/create")
    public void createRoad(@RequestBody Road road) {
        roadService.createRoad(road);
    }

    /**
     * 获取所有道路信息
     * @return 道路列表
     */
    @GetMapping("/all")
    public List<Road> getAllRoads() {
        return roadService.getAllRoads();
    }

    /**
     * 根据 ID 获取道路信息
     * @param id 道路的唯一标识符
     * @return 道路实体对象
     */
    @GetMapping("/{id}")
    public Road getRoadById(@PathVariable int id) {
        return roadService.getRoadById(id);
    }
}
