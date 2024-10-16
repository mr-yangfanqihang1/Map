package com.server.server.data;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.server.server.mapper.RoadCopyMapper;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.*;

@Component
public class OSMToRoadConverter {

    private final RoadCopyMapper roadCopyMapper;

    public OSMToRoadConverter(RoadCopyMapper roadCopyMapper) {
        this.roadCopyMapper = roadCopyMapper;
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

        // 提取所有节点
        for (JsonNode element : elements) {
            if ("node".equals(element.get("type").asText())) {
                long nodeId = element.get("id").asLong();
                double lat = element.get("lat").asDouble();
                double lon = element.get("lon").asDouble();
                nodeCoordinates.put(nodeId, new double[]{lat, lon});
            }
        }

        // 处理所有的way，构建邻接表
        for (JsonNode element : elements) {
            if ("way".equals(element.get("type").asText())) {
                JsonNode nodes = element.get("nodes");
                String name = element.get("tags").has("name") ? element.get("tags").get("name").asText() : "Unnamed Road";
                boolean isOneway = element.get("tags").has("oneway") && "yes".equals(element.get("tags").get("oneway").asText());

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

                        // 双向道路需要创建反向记录
                        if (!isOneway) {
                            createRoad(endNodeId, startNodeId, name, endCoord, startCoord);
                        }

                        // 将 startNodeId 和 endNodeId 关联起来，更新邻接关系
                        updateAdjacencyList(startNodeId, endNodeId);
                        if (!isOneway) {
                            updateAdjacencyList(endNodeId, startNodeId);
                        }
                    }
                }
            }
        }

        // 更新 nextRoadId 并插入数据库
        updateNextRoadIds();

        // 插入或更新数据库中的道路记录
        for (Road road : roads) {
            Road existingRoad = roadCopyMapper.getRoadById(road.getId()); // 查找数据库中是否存在该道路

            if (existingRoad != null) {
                // 道路已存在，更新 nextRoadId
                String updatedNextRoadId = mergeNextRoadIds(existingRoad.getNextRoadId(), road.getNextRoadId());
                existingRoad.setNextRoadId(updatedNextRoadId);

                roadCopyMapper.updateRoad(existingRoad); // 更新数据库中的道路
                System.out.println("道路ID " + road.getId() + " 已更新 nextRoadId。");
            } else {
                // 如果道路不存在，插入新的道路
                roadCopyMapper.insertRoad(road);
                System.out.println("插入新道路ID " + road.getId());
            }
        }
    }

    // 创建一条道路并加入到邻接表
    private void createRoad(long startNodeId, long endNodeId, String name, double[] startCoord, double[] endCoord) {
        Road road = new Road();
        road.setId(startNodeId);  // 使用 startNodeId 作为道路 ID
        road.setStartName("Node " + startNodeId); // 设置起点名称
        road.setName(name); // 设置道路名称
        road.setStartLat(startCoord[0]);
        road.setStartLong(startCoord[1]);
        road.setEndLat(endCoord[0]);
        road.setEndLong(endCoord[1]);
        road.setDistance(road.calculateDistance()); // 计算距离
        road.setMaxLoad(1000); // 默认载重
        road.setStatus("绿"); // 默认状态为 "绿"
        road.setPrice(road.getDistance() * 1.0); // 根据距离计算价格，假设每千米1.0单位
        road.setNextRoadId(""); // 将 nextRoadId 初始化为空字符串

        roads.add(road); // 将新建的道路加入道路列表
    }

    // 更新邻接表
    private void updateAdjacencyList(long startNodeId, long endNodeId) {
        nodeToRoadMap.putIfAbsent(startNodeId, new ArrayList<>());
        nodeToRoadMap.get(startNodeId).add(endNodeId);
    }

    // 更新每条道路的 nextRoadId
    private void updateNextRoadIds() {
        for (Road road : roads) {
            long startNodeId = road.getId();
            List<Long> nextRoads = nodeToRoadMap.get(startNodeId);

            if (nextRoads != null && !nextRoads.isEmpty()) {
                Set<Long> uniqueNextRoads = new HashSet<>(nextRoads);
                // 生成逗号分隔的 nextRoadId 列表
                String nextRoadIds = String.join(",", uniqueNextRoads.stream().map(String::valueOf).toArray(String[]::new));
                road.setNextRoadId(nextRoadIds);
            }
        }
    }

    // 合并 nextRoadId，确保没有重复的道路ID
    private String mergeNextRoadIds(String existingNextRoadId, String newNextRoadId) {
        if (existingNextRoadId == null || existingNextRoadId.isEmpty()) {
            return newNextRoadId;
        }

        Set<String> nextRoadSet = new HashSet<>(Arrays.asList(existingNextRoadId.split(",")));
        nextRoadSet.addAll(Arrays.asList(newNextRoadId.split(",")));

        return String.join(",", nextRoadSet);
    }
}
