package com.server.server.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;

import com.server.server.data.Road;
import com.server.server.mapper.RoadMapper;

@Service
public class RoadService {


    private final RoadMapper roadMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    private final ValueOperations<String, Object> valueOps;
    // 使用构造器注入 RedisTemplate 和 RoadMapper
    public RoadService(RoadMapper roadMapper, RedisTemplate<String, Object> redisTemplate) {
        this.roadMapper = roadMapper;
        this.redisTemplate = redisTemplate;
        this.valueOps = redisTemplate.opsForValue();
    }
    // 根据道路名称获取道路 ID


    //用名称模糊搜索
    public List<Road> getRoadsByName(String name) {
        List<Road> matchingRoads = new ArrayList<>();
        try {
            // 使用 keyCommands() 来代替已弃用的 scan() 方法
            Cursor<byte[]> cursor = redisTemplate.execute((RedisCallback<Cursor<byte[]>>) connection -> {
                ScanOptions options = ScanOptions.scanOptions().match("roadData:roadId:*").count(1000).build();
                return connection.keyCommands().scan(options);  // 使用 keyCommands() 进行键扫描
            });
    
            // 遍历 SCAN 返回的结果
            while (cursor != null && cursor.hasNext()) {
                String key = new String(cursor.next());
                Road road = (Road) valueOps.get(key);
                // 这里改成检查 road.getName() 是否包含传入的 name
                if (road != null && road.getName().toLowerCase().contains(name.toLowerCase())) {
                    matchingRoads.add(road);
                    // 找到前 10 个匹配项后停止
                    if (matchingRoads.size() >= 10) {
                        break;
                    }
                }
            }
            System.out.println("Fetched " + matchingRoads.size() + " matching roads from Redis.");
            return matchingRoads;
    
        } catch (Exception e) {
            System.out.println("Error while getting roads by name from Redis: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();  // 查询失败时返回空列表
        }
    }

    // 从redis获取所有道路数据
    public List<Road> getAllRoads() {
    List<Road> roads = new ArrayList<>();
    String pattern = "roadData:roadId:*";
    
    try {
        // 获取所有匹配的键
        Set<String> keys = redisTemplate.keys(pattern);
        
        if (keys != null && !keys.isEmpty()) {
            // 根据键逐个获取值
            for (String key : keys) {
                Road road = (Road) valueOps.get(key);
                if (road != null) {
                    roads.add(road);
                }
            }
            
            if (!roads.isEmpty()) {
                System.out.println("Get all roads from Redis");
                return roads;
            }
        }
        
        System.out.println("Road data not found in Redis");
    } catch (Exception e) {
        System.out.println("Error while getting all roads from Redis: " + e.getMessage());
        e.printStackTrace();
    }
    
    return roads;
}

    

    // 根据 ID 从 Redis 或数据库获取 Road
    public Road getRoadById(long id) {
        String key = "roadData:roadId:" + id;
        Road road = (Road) valueOps.get(key);
        if (road != null) {
           // System.out.println("Get road data from Redis: roadId: " + id);
            return road;
        } else {
            //System.out.println("Road data not found in Redis for roadId: " + id);
            // 如果 Redis 中没有，则从数据库查询并缓存
            road = roadMapper.getRoadById(id);
            if (road != null) {
                valueOps.set(key, road);  // 缓存到 Redis
            }
            return road;
        }
    }

    // 获取相邻道路信息
    public List<Road> getNeighbors(long id) {
        String key = "roadData:roadId:" + id;
        Road road = (Road) valueOps.get(key);  // 从 Redis 获取 road 对象
        try {
            if (road != null) {
                //System.out.println("Get road data from Redis: roadId: " + id);
                return getNeighborsByRoad(road);
            } else {
                //System.out.println("Road data not found in Redis for roadId: " + id);
                // 如果 Redis 中没有数据，从数据库中查询 Road 对象
                Road dbRoad = getRoadById(id);
                if (dbRoad != null) {
                    return getNeighborsByRoad(dbRoad);
                }
                return new ArrayList<>();  // 如果没有相邻道路，返回空列表
            }
        } catch (Exception e) {
            System.out.println("Error getting neighbor road objects for roadId: " + id);
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    // 根据给定 Road 获取其相邻的道路
    private List<Road> getNeighborsByRoad(Road road) {
        List<Road> neighbors = new ArrayList<>();
        String nextRoadIds = road.getNextRoadId();
        if (nextRoadIds != null && !nextRoadIds.isEmpty()) {
            for (String nextRoadId : nextRoadIds.split(",")) {
                long neighborId = Long.parseLong(nextRoadId.trim());
                Road neighborRoad = getRoadById(neighborId);
                if (neighborRoad != null) {
                    neighbors.add(neighborRoad);
                }
            }
        }
        return neighbors;
    }
}
