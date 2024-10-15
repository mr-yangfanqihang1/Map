package com.server.server.data;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.server.server.mapper.RoadMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Component
public class RoadDataParser implements CommandLineRunner {

    @Value("${road.geojson.path}")
    private String geoJsonFilePath;

    private final RoadMapper roadMapper;

    // 构造函数注入 RoadMapper
    public RoadDataParser(RoadMapper roadMapper) {
        this.roadMapper = roadMapper;
    }

    @Override
    public void run(String... args) throws Exception {
        // 使用 UTF-8 编码读取文件内容，防止中文乱码
        try (BufferedReader reader = Files.newBufferedReader(Paths.get(geoJsonFilePath), StandardCharsets.UTF_8)) {
            StringBuilder jsonData = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                jsonData.append(line);
            }
            List<Road> roads = parseRoadData(jsonData.toString());
            for (Road road : roads) {
                roadMapper.insertRoad(road);
            }
            System.out.println("Road data has been inserted into the database.");
        }
    }

    public List<Road> parseRoadData(String jsonData) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode rootNode = objectMapper.readTree(jsonData);
        JsonNode features = rootNode.get("features");

        List<Road> roadList = new ArrayList<>();

        if (features.isArray()) {
            for (JsonNode feature : features) {
                JsonNode geometry = feature.get("geometry");
                JsonNode coordinates = geometry.get("coordinates");

                // 获取起点和终点的经纬度
                double startLat = coordinates.get(0).get(1).asDouble();
                double startLong = coordinates.get(0).get(0).asDouble();
                double endLat = coordinates.get(1).get(1).asDouble();
                double endLong = coordinates.get(1).get(0).asDouble();

                // 提取道路名称
                String name = feature.get("properties").has("name")
                        ? feature.get("properties").get("name").asText()
                        : "Unknown Road";

                // 提取起点名称
                String startName = feature.get("properties").has("start_name")
                        ? feature.get("properties").get("start_name").asText()
                        : "Unknown Start";

                // 提取道路的 ID 作为 roadId
                String wayId = feature.has("@id") ? feature.get("@id").asText() : null;
                int roadId = 0;
                if (wayId != null && wayId.startsWith("way/")) {
                    roadId = Integer.parseInt(wayId.split("/")[1]);
                }

                // 提取下一道路的 ID 作为 nextRoadId
                String nextRoadId = feature.get("properties").has("next_roadid")
                        ? feature.get("properties").get("next_roadid").asText()
                        : null;

                if (nextRoadId != null && nextRoadId.startsWith("way/")) {
                    nextRoadId = nextRoadId.split("/")[1]; // 提取 "way/{id}" 中的 {id}
                }

                // 道路状态，只接受 '红', '橙', '绿'
                String status = feature.get("properties").has("status")
                        ? feature.get("properties").get("status").asText()
                        : "绿";  // 默认值为 '绿'

                // 检查 status 是否为有效值
                if (!status.equals("红") && !status.equals("橙") && !status.equals("绿")) {
                    status = "绿";  // 默认设置为 '绿'
                }

                // 其他默认值
                double pricePerKm = 5.0;  // 假设每公里的价格
                int maxLoad = 100;        // 假设最大载重

                // 创建 Road 对象并计算距离
                Road road = new Road(startName, name, startLat, startLong,
                        endLat, endLong, pricePerKm, status, maxLoad, nextRoadId);

                road.setId(roadId);  // 设置 Road 的唯一 ID
                
                roadList.add(road);
            }
        }

        return roadList;
    }
}
