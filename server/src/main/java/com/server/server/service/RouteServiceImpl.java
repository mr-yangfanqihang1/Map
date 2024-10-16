package com.server.server.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.server.server.data.Road;
import com.server.server.data.Route;
import com.server.server.data.RouteData;
import com.server.server.data.User;
import com.server.server.mapper.RouteMapper;
import com.server.server.mapper.UserMapper;

@Service
public class RouteServiceImpl implements RouteService {

    @Autowired
    private RouteMapper routeMapper;

    @Autowired
    private UserMapper userMapper;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void createRoute(Route route) {
        routeMapper.insertRoute(route);
    }

    @Override
    public Route getRouteById(int id) {
        return routeMapper.getRouteById(id);
    }

    @Override
    public Route calculateRoute(Route route) {
        // 调整动态优先级
        adjustDynamicPriority(route);

        // 从数据库获取用户信息及其权重
        User user = userMapper.getUserById(route.getUserId());
        if (user == null || user.getPreferences() == null) {
            throw new RuntimeException("用户的权重信息不存在");
        }

        // 解析 preferences JSON 字段，获取权重
        Map<String, Double> weights = getUserWeights(user.getPreferences());

        // 从数据库获取起始和结束 Road
        Road startRoad = routeMapper.getRoadById(route.getStartId());
        Road endRoad = routeMapper.getRoadById(route.getEndId());

        // 使用用户权重执行 A* 算法
        return aStarSearch(startRoad, endRoad, weights);
    }

    private void adjustDynamicPriority(Route route) {
        LocalDateTime now = LocalDateTime.now();
        long waitingTime = Duration.between(route.getRequestTime(), now).toMinutes();
        int additionalPriority = (int) (waitingTime / 0.5);
        route.setPriority(route.getPriority() + additionalPriority);
    }

    private Map<String, Double> getUserWeights(String preferencesJson) {
        try {
            return objectMapper.readValue(preferencesJson, Map.class);
        } catch (Exception e) {
            throw new RuntimeException("解析用户权重失败", e);
        }
    }

    private Route aStarSearch(Road startRoad, Road endRoad, Map<String, Double> weights) {
        List<RouteData> pathData = new ArrayList<>(); // 用于存储结果路径数据

        // 初始化开放列表和关闭列表
        PriorityQueue<Node> openList = new PriorityQueue<>(Comparator.comparingDouble(Node::getF));
        Set<Node> closedList = new HashSet<>();

        // 初始化起点节点
        Node startNode = new Node(startRoad, null, 0, heuristic(startRoad, endRoad));
        openList.add(startNode);

        // A* 主循环
        while (!openList.isEmpty()) {
            Node currentNode = openList.poll();
            if (isGoal(currentNode, endRoad)) {
                return constructRoute(currentNode, pathData); // 构建路径
            }

            closedList.add(currentNode);

            // 遍历邻居节点
            for (Road neighbor : getNeighbors(currentNode.getRoad())) {
                double cost = getCost(neighbor, weights);
                Node neighborNode = new Node(neighbor, currentNode, currentNode.getG() + cost, 0);
                neighborNode.setH(heuristic(neighbor, endRoad));

                if (closedList.contains(neighborNode)) {
                    continue;
                }

                if (!openList.contains(neighborNode)) {
                    openList.add(neighborNode);
                }
            }
        }
        return null;
    }

    private double getCost(Road road, Map<String, Double> weights) {
        // 根据 Road 属性计算代价（距离、时间、价格等）
        double normalizedDistance = Double.parseDouble(String.valueOf( road.getDistance()));
        double normalizedDuration = Double.parseDouble(String.valueOf(road.getDuration()));
        double normalizedPrice = Double.parseDouble( String.valueOf(road.getPrice()));

        double weightDistance = weights.getOrDefault("weightDistance", 1.0);
        double weightDuration = weights.getOrDefault("weightDuration", 1.0);
        double weightPrice = weights.getOrDefault("weightPrice", 1.0);

        return weightDistance * normalizedDistance +
               weightDuration * normalizedDuration +
               weightPrice * normalizedPrice;
    }

    private boolean isGoal(Node currentNode, Road endRoad) {
        return currentNode.getRoad().equals(endRoad);
    }

    private Route constructRoute(Node node, List<RouteData> pathData) {
        while (node != null) {
            Road road = node.getRoad();
    
            RouteData routeData = new RouteData();
            routeData.setStartLat(road.getStartLat());
            routeData.setStartLong(road.getStartLong());
            routeData.setEndLat(road.getEndLat());
            routeData.setEndLong(road.getEndLong());
            routeData.setDistance(road.getDistance());
            routeData.setPrice(road.getPrice());
    
            pathData.add(0, routeData); // 将节点数据加入路径
            node = node.getParent(); // 移动到父节点
        }
    
        Route resultRoute = new Route();
        resultRoute.setPathData(pathData); // 设置路径数据
        return resultRoute;
    }

    private List<Road> getNeighbors(Road currentRoad) {
        // 根据实际需求获取邻居
        return routeMapper.getNeighbors(currentRoad.getId()); // 假设方法返回相邻道路的列表
    }

    private double heuristic(Road currentRoad, Road endRoad) {
        // 这里可以实现更复杂的启发式函数，例如基于距离的估算
        return 0; // 你可以根据具体情况修改
    }

    private static class Node {
        private Road road;
        private Node parent;
        private double g;
        private double h;

        public Node(Road road, Node parent, double g, double h) {
            this.road = road;
            this.parent = parent;
            this.g = g;
            this.h = h;
        }

        public double getF() {
            return g + h;
        }

        public Road getRoad() {
            return road;
        }

        public Node getParent() {
            return parent;
        }

        public double getG() {
            return g;
        }

        public void setH(double h) {
            this.h = h;
        }
    }
}
