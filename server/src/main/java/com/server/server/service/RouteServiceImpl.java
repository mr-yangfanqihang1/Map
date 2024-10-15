package com.server.server.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
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
    private UserMapper userMapper;  // 用于查询用户权重

    private final ObjectMapper objectMapper = new ObjectMapper();  // 用于解析 JSON 的 ObjectMapper

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

        // 使用用户权重执行 A* 算法
        return aStarSearch(route, weights);
    }

    // 动态调整优先级，基于请求时间和初始优先级
    private void adjustDynamicPriority(Route route) {
        LocalDateTime now = LocalDateTime.now();
        // 计算请求已等待的时间（分钟）
        long waitingTime = Duration.between(route.getRequestTime(), now).toMinutes();

        // 动态增加优先级：每等待10分钟，增加1点优先级
        int additionalPriority = (int) (waitingTime / 10);

        // 动态优先级 = 初始优先级 + 额外增加的优先级
        route.setDynamicPriority(route.getPriority() + additionalPriority);
    }

    // 从 JSON 字符串解析用户的权重
    private Map<String, Double> getUserWeights(String preferencesJson) {
        try {
            // 使用 ObjectMapper 将 JSON 字符串转换为 Map<String, Double>
            return objectMapper.readValue(preferencesJson, Map.class);  // 解析成 Map<String, Double>
        } catch (Exception e) {
            throw new RuntimeException("解析用户权重失败", e);
        }
    }

    // A* 搜索算法
    private Route aStarSearch(Route route, Map<String, Double> weights) {
        List<RouteData> pathData = route.getPathData();

        // 计算路径中 distance, duration, price 的最大值和最小值，用于归一化
        double maxDistance = pathData.stream().mapToDouble(RouteData::getDistance).max().orElse(1);
        double minDistance = pathData.stream().mapToDouble(RouteData::getDistance).min().orElse(0);
        double maxDuration = pathData.stream().mapToDouble(RouteData::getDuration).max().orElse(1);
        double minDuration = pathData.stream().mapToDouble(RouteData::getDuration).min().orElse(0);
        double maxPrice = pathData.stream().mapToDouble(RouteData::getPrice).max().orElse(1);
        double minPrice = pathData.stream().mapToDouble(RouteData::getPrice).min().orElse(0);

        // 初始化开放列表和关闭列表
        PriorityQueue<Node> openList = new PriorityQueue<>(Comparator.comparingDouble(Node::getF));
        Set<Node> closedList = new HashSet<>();

        // 初始化起点节点
        Node startNode = new Node(pathData.get(0), null, 0, heuristic(pathData.get(0), route));
        openList.add(startNode);

        // A* 主循环
        while (!openList.isEmpty()) {
            Node currentNode = openList.poll();  // 获取当前代价最小的节点
            if (isGoal(currentNode, pathData)) {
                // 如果达到目标点，构造路径并返回
                return constructRoute(currentNode, route);
            }

            closedList.add(currentNode);  // 将当前节点标记为已处理

            // 遍历当前节点的邻居
            for (RouteData neighborData : getNeighbors(currentNode, pathData)) {
                // 计算邻居节点的代价
                double cost = getCost(neighborData, weights, maxDistance, minDistance, maxDuration, minDuration, maxPrice, minPrice);
                Node neighborNode = new Node(neighborData, currentNode, currentNode.getG() + cost, 0);
                neighborNode.setH(heuristic(neighborData, route));

                if (closedList.contains(neighborNode)) {
                    // 如果邻居节点已经在关闭列表中，跳过
                    continue;
                }

                if (!openList.contains(neighborNode)) {
                    // 如果邻居节点不在开放列表中，添加到开放列表
                    openList.add(neighborNode);
                }
            }
        }
        return null; // 没有找到路径，返回 null
    }

    // 计算代价函数，依据用户的权重和归一化的值
    private double getCost(RouteData data, Map<String, Double> weights, double maxDistance, double minDistance, double maxDuration, double minDuration, double maxPrice, double minPrice) {
        // 归一化的距离、时间和价格
        double normalizedDistance = (data.getDistance() - minDistance) / (maxDistance - minDistance);
        double normalizedDuration = (data.getDuration() - minDuration) / (maxDuration - minDuration);
        double normalizedPrice = (data.getPrice() - minPrice) / (maxPrice - minPrice);

        // 从用户的权重中获取 distance、duration、price 的权重
        double weightDistance = weights.getOrDefault("weightDistance", 1.0);
        double weightDuration = weights.getOrDefault("weightDuration", 1.0);
        double weightPrice = weights.getOrDefault("weightPrice", 1.0);

        // 加权计算代价
        return weightDistance * normalizedDistance +
               weightDuration * normalizedDuration +
               weightPrice * normalizedPrice;
    }

    // 辅助方法：判断是否到达目标点（简化实现）
    private boolean isGoal(Node currentNode, List<RouteData> pathData) {
        return currentNode.getRouteData().equals(pathData.get(pathData.size() - 1));
    }

    // 辅助方法：构造最终路径（简化实现）
    private Route constructRoute(Node node, Route route) {
        List<RouteData> path = new ArrayList<>();
        while (node != null) {
            path.add(0, node.getRouteData());  // 将节点数据加入路径
            node = node.getParent();
        }
        route.setPathData(path);
        return route;
    }

    // 辅助方法：获取邻居节点（简化实现）
    private List<RouteData> getNeighbors(Node currentNode, List<RouteData> pathData) {
        // 根据实际业务需求实现邻居节点查找逻辑
        return pathData; // 这里假设所有路径点都是邻居，实际实现中应根据逻辑改进
    }

    // 辅助方法：计算启发式函数（H 值），这里简化为 0
    private double heuristic(RouteData data, Route route) {
        return 0; // 简化为 0，可以根据实际需求进行调整
    }

    // 内部类 Node 用于表示 A* 算法中的节点
    private static class Node {
        private RouteData routeData;
        private Node parent;
        private double g; // 实际代价
        private double h; // 启发式代价

        public Node(RouteData routeData, Node parent, double g, double h) {
            this.routeData = routeData;
            this.parent = parent;
            this.g = g;
            this.h = h;
        }

        public double getF() {
            return g + h;  // 总代价 = 实际代价 + 启发式代价
        }

        public RouteData getRouteData() {
            return routeData;
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

