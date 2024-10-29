package com.server.server.controller;
import com.server.server.service.RoadService;
import com.server.server.data.Road;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/roads")
public class RoadController {

    private final RoadService roadService;

    // 使用构造器注入，确保依赖关系正确注入
    public RoadController(RoadService roadService) {
        this.roadService = roadService;
    }

    /**
     * 根据道路名称获取道路 ID 列表
     * @param name 道路名称
     * @return 道路 ID 列表
     */
    @GetMapping("/name")
    public List<Road> getRoadsByName(@RequestParam String name) {
        return roadService.getRoadsByName(name);
    }


    /**
     * 获取所有道路信息
     * @return 道路列表
     */
    @GetMapping("/all")
    public List<Road> getAllRoads(@RequestParam(defaultValue = "0") int offset, 
                                  @RequestParam(defaultValue = "10") int limit) {
        return roadService.getAllRoads(offset, limit);
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
