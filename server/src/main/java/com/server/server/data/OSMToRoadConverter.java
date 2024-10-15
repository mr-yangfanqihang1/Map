package com.server.server.data;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.server.server.mapper.RoadMapper;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.*;

@Component
public class OSMToRoadConverter {

    private final RoadMapper roadMapper;

    public OSMToRoadConverter(RoadMapper roadMapper) {
        this.roadMapper = roadMapper;
    }

    // 保存节点的经纬度
    private Map<Long, double[]> nodeCoordinates = new HashMap<>();

    // 保存每个节点对应的道路ID（邻接表中的nextRoadId）
    private Map<Long, List<Long>> nodeToRoadMap = new HashMap<>();

    // 保存生成的道路，方便后续插入数据库
    private List<Road> roads = new ArrayList<>();

    // 解析 OSM 数据并构建邻接表
    public void parseOSMData() throws IOException {
        // 使用相对路径读取文件
        File osmFile = Paths.get("server/src/main/resources/export.json").toFile();

        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode rootNode = objectMapper.readTree(osmFile);
        JsonNode elements = rootNode.get("elements");

        // 首先提取所有节点
        for (JsonNode element : elements) {
            if ("node".equals(element.get("type").asText())) {
                long nodeId = element.get("id").asLong();
                double lat = element.get("lat").asDouble();
                double lon = element.get("lon").asDouble();
                nodeCoordinates.put(nodeId, new double[]{lat, lon});
            }
        }

        // 然后处理所有的way，构建邻接表
        for (JsonNode element : elements) {
            if ("way".equals(element.get("type").asText())) {
                JsonNode nodes = element.get("nodes");
                String name = element.get("tags").has("name") ? element.get("tags").get("name").asText() : "Unnamed Road";

                // 构建邻接表中的道路，处理从 startNodeId 到 endNodeId
                for (int i = 0; i < nodes.size() - 1; i++) {
                    long startNodeId = nodes.get(i).asLong();
                    long endNodeId = nodes.get(i + 1).asLong();

                    // 获取起点和终点的经纬度
                    double[] startCoord = nodeCoordinates.get(startNodeId);
                    double[] endCoord = nodeCoordinates.get(endNodeId);

                    if (startCoord != null && endCoord != null) {
                        // 创建一条邻接表中的道路记录
                        createRoad(startNodeId, endNodeId, name, startCoord, endCoord);

                        // 将 startNodeId 和 endNodeId 关联起来，更新邻接关系
                        if (!nodeToRoadMap.containsKey(startNodeId)) {
                            nodeToRoadMap.put(startNodeId, new ArrayList<>());
                        }
                        nodeToRoadMap.get(startNodeId).add(endNodeId);

                        if (!nodeToRoadMap.containsKey(endNodeId)) {
                            nodeToRoadMap.put(endNodeId, new ArrayList<>());
                        }
                        nodeToRoadMap.get(endNodeId).add(startNodeId);
                    }
                }
            }
        }

        // 处理所有道路的 nextRoadId
        updateNextRoadIds();

        // 插入数据库
        for (Road road : roads) {
            if (roadMapper.existsById(road.getId()) == 0) {
                roadMapper.insertRoad(road);
            } else {
                System.out.println("道路ID " + road.getId() + " 已存在，跳过插入。");
            }
            
        }
    }

    // 创建一条道路并加入到邻接表
    private void createRoad(long startNodeId, long endNodeId, String name, double[] startCoord, double[] endCoord) {
        Road road = new Road();
        road.setId(startNodeId);  // 直接使用 long 类型的 startNodeId
        road.setName(name);
        road.setStartLat(startCoord[0]);
        road.setStartLong(startCoord[1]);
        road.setEndLat(endCoord[0]);
        road.setEndLong(endCoord[1]);
        road.setDistance(road.calculateDistance());  // 计算距离
        roads.add(road);
    }
    

    // 更新每条道路的 nextRoadId
    private void updateNextRoadIds() {
        for (Road road : roads) {
            long startNodeId = road.getId();  // 使用道路的 startNodeId 作为 ID
            List<Long> nextRoads = nodeToRoadMap.get(startNodeId);  // 获取所有与该节点相连的其他节点

            if (nextRoads != null && !nextRoads.isEmpty()) {
                Set<Long> uniqueNextRoads = new HashSet<>(nextRoads);
                // 生成 nextRoadId 列表
                String nextRoadIds = String.join(",", uniqueNextRoads.stream().map(String::valueOf).toArray(String[]::new));
                road.setNextRoadId(nextRoadIds);
            }
        }
    }
}
