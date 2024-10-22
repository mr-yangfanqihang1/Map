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
import com.server.server.mapper.RouteMapper;

@Service
public class RouteServiceImpl implements RouteService {

    @Autowired
    private RouteMapper routeMapper;
    @Autowired
    private RoadService roadService;
    @Autowired
    private UserService userService;

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
        // 1. 先查询数据库中的路径
        Route existingRoute = routeMapper.findPathByStartAndEnd(route.getStartId(), route.getEndId());
        if (existingRoute != null) {
            System.out.println("Found existing route in database.");
            return existingRoute; // 如果数据库中有，直接返回
        }

        // 2. 调整动态优先级
        adjustDynamicPriority(route);

        // 打印 route 数据，调试用
        System.out.println("Calculating new route for: " + route.toString());
        Map<String, Double> weights = getUserWeights(userService.getPreferences(route.getUserId()));

        // 从 redis 获取起始和结束 Road
        Road startRoad = roadService.getRoadById(route.getStartId());
        Road endRoad = roadService.getRoadById(route.getEndId());
        System.out.println("Start Road: " + startRoad.toString());
        System.out.println("End Road: " + endRoad.toString());

        // 3. 使用用户权重执行 A* 算法计算新路径
        Route newRoute = aStarSearch(startRoad, endRoad, weights);

        // 4. 将新路径存入数据库
        if (newRoute != null) {
            newRoute.setStartId(route.getStartId());
            newRoute.setEndId(route.getEndId());
            routeMapper.insertRoute(newRoute); // 存入数据库
            System.out.println("New route inserted into database.");
        }

        return newRoute;
    }


    private void adjustDynamicPriority(Route route) {
        LocalDateTime now = LocalDateTime.now();
        long waitingTime = Duration.between(route.getRequestTime(), now).toMinutes();
        int additionalPriority = (int) (waitingTime / 0.5);

        System.out.println("Adjusting priority for route. Original priority: " 
        + route.getPriority() + ", Additional priority: " + additionalPriority);

        route.setPriority(route.getPriority() + additionalPriority);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Double> getUserWeights(String preferencesJson) {
        try {
            System.out.println("Parsing user weights from preferences: " + preferencesJson);
            return objectMapper.readValue(preferencesJson, Map.class);
        } catch (Exception e) {
            System.err.println("Failed to parse user weights.");
            throw new RuntimeException("解析用户权重失败", e);
        }
    }

    private Route aStarSearch(Road startRoad, Road endRoad, Map<String, Double> weights) {
        List<RouteData> pathData = new ArrayList<>(); // 用于存储结果路径数据
    
        // 初始化开放列表和关闭列表
        PriorityQueue<Node> openList = new PriorityQueue<>(Comparator.comparingDouble(Node::getF));
        Set<Road> closedList = new HashSet<>();  // 仅存储 Road 而不是 Node
    
        // 初始化起点节点
        Node startNode = new Node(startRoad, null, 0, heuristic(startRoad, endRoad));
        openList.add(startNode);
    
        System.out.println("开始 A* 搜索。起点节点: " + startNode.getRoad().getName());
    
        // A* 主循环
        while (!openList.isEmpty()) {
            Node currentNode = openList.poll();  // 取出优先队列中的节点
            System.out.println("当前节点: " + currentNode.getRoad().getName());
    
            // 如果到达终点，构建路径
            if (isGoal(currentNode, endRoad)) {
                System.out.println("找到目标节点: " + currentNode.getRoad().getName());
                return constructRoute(currentNode, pathData);  // 构建路径
            }
    
            // 将当前节点加入关闭列表
            closedList.add(currentNode.getRoad());
    
            // 遍历邻居节点
            for (Road neighbor : getNeighbors(currentNode.getRoad())) {
                // 如果邻居节点在关闭列表中，跳过它
                if (closedList.contains(neighbor)) {
                    System.out.println("邻居已在关闭列表中: " + neighbor.getName());
                    continue;
                }
    
                // 检查邻居节点的 duration 值是否正确
                System.out.println("邻居道路: " + neighbor.getName() + ", 距离: " + neighbor.getDistance() + ", 时长: " + neighbor.getDuration());
    
                double cost = getCost(neighbor, weights);
                Node neighborNode = new Node(neighbor, currentNode, currentNode.getG() + cost, 0);
                neighborNode.setH(heuristic(neighbor, endRoad));
    
                // 如果邻居不在开放列表中，或者发现一个更优路径，添加到开放列表
                if (!openList.contains(neighborNode)) {
                    openList.add(neighborNode);
                    System.out.println("将邻居添加到开放列表: " + neighbor.getName());
                } else {
                    // 更新现有邻居节点的优先级（如果有更优的路径）
                    for (Node openNode : openList) {
                        if (openNode.getRoad().equals(neighbor) && neighborNode.getG() < openNode.getG()) {
                            openList.remove(openNode);  // 移除旧的节点
                            openList.add(neighborNode);  // 添加新的更优节点
                            System.out.println("更新开放列表中的邻居: " + neighbor.getName());
                            break;
                        }
                    }
                }
            }
        }
    
        System.out.println("未找到路径。");
        return null; // 如果未找到路径
    }
    

    private double getCost(Road road, Map<String, Double> weights) {
        // 根据 Road 属性计算代价（距离、时间、价格等）
        double normalizedDistance = Double.parseDouble(String.valueOf(road.getDistance()));
        double normalizedDuration = Double.parseDouble(String.valueOf(road.getDuration()));
        double normalizedPrice = Double.parseDouble(String.valueOf(road.getPrice()));

        double weightDistance = weights.getOrDefault("weightDistance", 1.0);
        double weightDuration = weights.getOrDefault("weightDuration", 1.0);
        double weightPrice = weights.getOrDefault("weightPrice", 1.0);

        double cost = weightDistance * normalizedDistance +
                      weightDuration * normalizedDuration +
                      weightPrice * normalizedPrice;

        System.out.println("Cost for road " + road.getName() + ": " + cost);
        return cost;
    }

    private boolean isGoal(Node currentNode, Road endRoad) {
        boolean isGoal = currentNode.getRoad().equals(endRoad);
        if (isGoal) {
            System.out.println("Goal node reached: " + currentNode.getRoad().getName());
        }
        return isGoal;
    }

    private Route constructRoute(Node node, List<RouteData> pathData) {
        System.out.println("Constructing route from goal to start...");
    
        double totalDistance = 0;
        double totalDuration = 0;
        double totalPrice = 0;

        while (node != null) {
            Road road = node.getRoad();
    
            RouteData routeData = new RouteData();
            routeData.setStartLat(road.getStartLat());
            routeData.setStartLong(road.getStartLong());
            routeData.setEndLat(road.getEndLat());
            routeData.setEndLong(road.getEndLong());
            routeData.setDistance(road.getDistance());
            routeData.setDuration(road.getDuration());
            routeData.setPrice(road.getPrice());
    
            totalDistance += road.getDistance();
            totalDuration += road.getDuration();
            totalPrice += road.getPrice();
    
            pathData.add(0, routeData); // 将节点数据加入路径
            node = node.getParent(); // 移动到父节点
        }

        System.out.println("Total distance: " + totalDistance);
        System.out.println("Total duration: " + totalDuration);
        System.out.println("Total price: " + totalPrice);

        Route resultRoute = new Route();
        resultRoute.setPathData(pathData); // 设置路径数据
        resultRoute.setDistance(String.valueOf(totalDistance));
        resultRoute.setDuration(String.valueOf(totalDuration));
        resultRoute.setPrice(String.valueOf(totalPrice));
        
        return resultRoute;
    }

    private List<Road> getNeighbors(Road currentRoad) {
        System.out.println("Getting neighbors for road: " + currentRoad.getName());
        return roadService.getNeighbors(currentRoad.getId()); // 假设方法返回相邻道路的列表
    }

    private double heuristic(Road currentRoad, Road endRoad) {
        // 可以实现基于距离的启发式估算
        double heuristicValue = 0; // 简单的启发式函数可以改进
        System.out.println("Heuristic value for road " + currentRoad.getName() + " to " + endRoad.getName() + ": " + heuristicValue);
        return heuristicValue;
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


