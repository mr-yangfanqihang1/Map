package com.server.server.service;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.server.server.data.Road;
import com.server.server.data.Route;
import com.server.server.data.RouteData;
import com.server.server.mapper.RouteMapper;
import com.fasterxml.jackson.core.type.TypeReference;
@Service
public class RouteServiceImpl implements RouteService {
    @Autowired
    private RouteMapper routeMapper;
    @Autowired
    private RoadService roadService;
    @Autowired
    private UserService userService;
    @Autowired
    private SmartService smartService;
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    private static final String REDIS_ROUTE_PREFIX = "route:";
    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final int PRIORITY_LEVELS = 5; // 定义 5 个优先级队列
    private final List<Queue<Route>> priorityQueues = new ArrayList<>(PRIORITY_LEVELS);
    boolean  isStatus = false;

    public void  setIsStatus(){
        isStatus = true;
    }
    // 初始化多个优先级队列
    public RouteServiceImpl() {
        for (int i = 0; i < PRIORITY_LEVELS; i++) {
            priorityQueues.add(new LinkedList<>());
        }
    }

    @Override
    public void createRoute(Route route) {
        routeMapper.insertRoute(route);
        addToPriorityQueue(route); // 将新创建的路线添加到优先级队列
    }

    @Override
    public Route getRouteById(int id) {
        return routeMapper.getRouteById(id);
    }


    private void adjustDynamicPriority(Route route) {
        LocalDateTime now = LocalDateTime.now();
        long waitingTime = Duration.between(route.getRequestTime(), now).toMinutes();
    
        // 根据等待时间动态调整优先级，每等待30秒，优先级提升 1 级
        int additionalPriority = (int) (waitingTime / 0.5); 
        System.out.println("Adjusting priority for route. Original priority: " 
            + route.getPriority() + ", Additional priority: " + additionalPriority);
        // 确保优先级始终在合法范围内
        int newPriority = Math.min(PRIORITY_LEVELS - 1, Math.max(0, route.getPriority() - additionalPriority));
        route.setPriority(newPriority);
    }

    private void addToPriorityQueue(Route route) {
        int priority = Math.min(route.getPriority(), PRIORITY_LEVELS - 1); // 确保优先级不超过最大值
        System.out.println("Adding route to priority queue: Priority " + priority);
        priorityQueues.get(priority).offer(route); // 将请求添加到对应的优先级队列中
    }

    public void scheduleRoutes() {
        int timeSlice = 10000000; // 每个优先级的时间片，单位为毫秒
        boolean hasProcessed = false; 
        
        for (int i = 0; i < PRIORITY_LEVELS; i++) {
            Queue<Route> currentQueue = priorityQueues.get(i);
    
            if (!currentQueue.isEmpty()) {
                Route route = currentQueue.poll();  
                Route calculatedRoute = calculateRoute(route); // 计算路线并返回

                if (calculatedRoute != null) {
                    routeMapper.updateRoute(calculatedRoute);
                }

                hasProcessed = true; 
                
                try {
                    Thread.sleep(timeSlice);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    
        if (!hasProcessed) {
            System.out.println("No routes to process.");
        }
    }

    @Scheduled(fixedDelay = 10000000)  // 每 3 秒执行一次调度
    public void runScheduler() {
        System.out.println("Running route scheduling...");
        scheduleRoutes();
    }

    @Override
    public Route calculateRoute(Route route) {
        // 调整动态优先级
        adjustDynamicPriority(route);
        
         // 获取用户偏好的权重
         Map<String, Integer> weights = getUserWeights(userService.getPreferences(route.getUserId()));
         route.setDistanceWeight(weights.getOrDefault("distance", 100));
         route.setDurationWeight(weights.getOrDefault("time", 100));
         route.setPriceWeight(weights.getOrDefault("price", 100));
         
    // 查询并返回路径
    Route existingRoute = routeMapper.findPathByStartAndEnd(
        route.getStartId(),
        route.getEndId(),
        route.getDistanceWeight(),
        route.getDurationWeight(),
        route.getPriceWeight()
    );
    if (existingRoute!=null){
        System.out.println("Found existing route in database" );
        return existingRoute;
    } 

        

        // 从redis获取起始和结束 Road
        Road startRoad = new Road();
        Road endRoad = new Road();
        isStatus = smartService.getIsStatus();
        if(isStatus == false) {
            startRoad = roadService.getRoadById(route.getStartId());
            endRoad = roadService.getRoadById(route.getEndId());
        }else if(isStatus == true) {
            startRoad = roadService.getGreenRoadById(route.getStartId());
            endRoad = roadService.getGreenRoadById(route.getEndId());
        }
        // 使用用户权重执行 A* 算法
        Route calculatedRoute = aStarSearch(startRoad, endRoad, weights, route,isStatus);

        if (calculatedRoute == null) {
            System.out.println("未找到路径。");
            throw new RuntimeException("未找到路径，请检查起点和终点。");
        }

        // 将计算的路线添加到优先级队列
        addToPriorityQueue(calculatedRoute);

        return calculatedRoute;
    }
    @SuppressWarnings("unchecked")
    private Map<String, Integer> getUserWeights(String preferencesJson) {
        try {
            System.out.println("Parsing user weights from preferences: " + preferencesJson);
            Map<String, Integer> weights = objectMapper.readValue(preferencesJson, new TypeReference<Map<String, Integer>>() {});
            System.out.println("Parsed weights: " + weights);  // 输出解析后的内容
            return weights;
        } catch (Exception e) {
            System.err.println("Failed to parse user weights.");
            throw new RuntimeException("解析用户权重失败", e);
        }
    }
    
    private void saveStateToRedis(String userId, PriorityQueue<Node> openList, Set<Road> closedList, Node currentNode) {
        try {
            // 序列化并存储 openList 和 closedList
            redisTemplate.opsForHash().put(REDIS_ROUTE_PREFIX + userId, "openList", objectMapper.writeValueAsString(openList));
            redisTemplate.opsForHash().put(REDIS_ROUTE_PREFIX + userId, "closedList", objectMapper.writeValueAsString(closedList));
            redisTemplate.opsForHash().put(REDIS_ROUTE_PREFIX + userId, "currentNode", objectMapper.writeValueAsString(currentNode));
            System.out.println("Successfully saved A* state to Redis for user: " + userId);
        } catch (Exception e) {
            System.out.println("Error saving A* state to Redis: " + e.getMessage());
        }
    }
    
    // 从Redis恢复A*算法的状态
    private boolean restoreStateFromRedis(String userId, PriorityQueue<Node> openList, Set<Road> closedList) {
        try {
            // 从Redis获取存储的 openList 和 closedList
            String openListJson = (String) redisTemplate.opsForHash().get(REDIS_ROUTE_PREFIX + userId, "openList");
            String closedListJson = (String) redisTemplate.opsForHash().get(REDIS_ROUTE_PREFIX + userId, "closedList");
            String currentNodeJson = (String) redisTemplate.opsForHash().get(REDIS_ROUTE_PREFIX + userId, "currentNode");
    
            if (openListJson != null && closedListJson != null && currentNodeJson != null) {
                openList.addAll(objectMapper.readValue(openListJson, objectMapper.getTypeFactory().constructCollectionType(PriorityQueue.class, Node.class)));
                closedList.addAll(objectMapper.readValue(closedListJson, objectMapper.getTypeFactory().constructCollectionType(Set.class, Road.class)));
                Node currentNode = objectMapper.readValue(currentNodeJson, Node.class);
                System.out.println("Successfully restored A* state from Redis for user: " + userId);
                return true;
            }
        } catch (Exception e) {
            System.out.println("Error restoring A* state from Redis: " + e.getMessage());
        }
        return false;
    }

    private Route aStarSearch(Road startRoad, Road endRoad, Map<String, Integer> weights, Route route, boolean isStatus) {
        List<RouteData> RouteData = new ArrayList<>(); // 用于存储结果路径数据
        
        PriorityQueue<Node> openList = new PriorityQueue<>(Comparator.comparingDouble(Node::getF));
        Set<Road> closedList = new HashSet<>();
        
        // 从Redis恢复上次中断的状态
        if (!restoreStateFromRedis(String.valueOf(route.getUserId()), openList, closedList)) {
            // 如果没有中断点，初始化开放和关闭列表
            Node startNode = new Node(startRoad, null, 0, heuristic(startRoad, endRoad));
            openList.add(startNode);
        }
        if(isStatus ==false) {
            // A* 主循环
            while (!openList.isEmpty()) {
                Node currentNode = openList.poll();  // 取出优先队列中的节点
                //System.out.println("当前节点: " + currentNode.getRoad().getName());

                // 如果到达终点，构建路径
                if (isGoal(currentNode, endRoad)) {
                    //System.out.println("找到目标节点: " + currentNode.getRoad().getName());
                    return constructRoute(currentNode, RouteData, route);  // 构建路径
                }

                // 将当前节点加入关闭列表
                closedList.add(currentNode.getRoad());

                // 遍历邻居节点
                for (Road neighbor : getNeighbors(currentNode.getRoad())) {
                    // 如果邻居节点在关闭列表中，跳过它
                    if (closedList.contains(neighbor)) {
                        // System.out.println("邻居已在关闭列表中: " + neighbor.getName());
                        continue;
                    }

                    // 检查邻居节点的 duration 值是否正确
                    //System.out.println("邻居道路: " + neighbor.getName() + ", 距离: " + neighbor.getDistance() + ", 时长: " + neighbor.getDuration());

                    double cost = getCost(neighbor, weights);
                    Node neighborNode = new Node(neighbor, currentNode, currentNode.getG() + cost, 0);
                    neighborNode.setH(heuristic(neighbor, endRoad));

                    // 如果邻居不在开放列表中，或者发现一个更优路径，添加到开放列表
                    if (!openList.contains(neighborNode)) {
                        openList.add(neighborNode);
                        // System.out.println("将邻居添加到开放列表: " + neighbor.getName());
                    } else {
                        // 更新现有邻居节点的优先级（如果有更优的路径）
                        for (Node openNode : openList) {
                            if (openNode.getRoad().equals(neighbor) && neighborNode.getG() < openNode.getG()) {
                                openList.remove(openNode);  // 移除旧的节点
                                openList.add(neighborNode);  // 添加新的更优节点
                                //System.out.println("更新开放列表中的邻居: " + neighbor.getName());
                                break;
                            }
                        }
                    }
                }
                if (shouldPause()) {
                    saveStateToRedis(String.valueOf(route.getUserId()), openList, closedList, currentNode);
                    return null; // 暂时中断
                }
            }
        }else if(isStatus == true){// A* 主循环
            while (!openList.isEmpty()) {
                Node currentNode = openList.poll();  // 取出优先队列中的节点
                //System.out.println("当前节点: " + currentNode.getRoad().getName());

                // 如果到达终点，构建路径
                if (isGoal(currentNode, endRoad)) {
                    //System.out.println("找到目标节点: " + currentNode.getRoad().getName());
                    return constructRoute(currentNode, RouteData, route);  // 构建路径
                }

                // 将当前节点加入关闭列表
                closedList.add(currentNode.getRoad());

                // 遍历邻居节点
                for (Road neighbor : getGreenNeighbors(currentNode.getRoad())) {
                    // 如果邻居节点在关闭列表中，跳过它
                    if (closedList.contains(neighbor)) {
                        // System.out.println("邻居已在关闭列表中: " + neighbor.getName());
                        continue;
                    }

                    // 检查邻居节点的 duration 值是否正确
                    //System.out.println("邻居道路: " + neighbor.getName() + ", 距离: " + neighbor.getDistance() + ", 时长: " + neighbor.getDuration());

                    double cost = getCost(neighbor, weights);
                    Node neighborNode = new Node(neighbor, currentNode, currentNode.getG() + cost, 0);
                    neighborNode.setH(heuristic(neighbor, endRoad));

                    // 如果邻居不在开放列表中，或者发现一个更优路径，添加到开放列表
                    if (!openList.contains(neighborNode)) {
                        openList.add(neighborNode);
                        // System.out.println("将邻居添加到开放列表: " + neighbor.getName());
                    } else {
                        // 更新现有邻居节点的优先级（如果有更优的路径）
                        for (Node openNode : openList) {
                            if (openNode.getRoad().equals(neighbor) && neighborNode.getG() < openNode.getG()) {
                                openList.remove(openNode);  // 移除旧的节点
                                openList.add(neighborNode);  // 添加新的更优节点
                                //System.out.println("更新开放列表中的邻居: " + neighbor.getName());
                                break;
                            }
                        }
                    }
                }
                if (shouldPause()) {
                    saveStateToRedis(String.valueOf(route.getUserId()), openList, closedList, currentNode);
                    return null; // 暂时中断
                }
            }
        }
        System.out.println("未找到路径。");
        return null; // 如果未找到路径
    }
    private boolean shouldPause() {
        // 可以根据实际时间、轮转调度时间片等条件判断
        return System.currentTimeMillis() % 1000000 == 0;
    }

    private double getCost(Road road, Map<String, Integer> weights) {
        // 根据 Road 属性计算代价（距离、时间、价格等）
        double normalizedDistance = Double.parseDouble(String.valueOf(road.getDistance()));
        double normalizedDuration = Double.parseDouble(String.valueOf(road.getDuration()));
        double normalizedPrice = Double.parseDouble(String.valueOf(road.getPrice()));

        double weightDistance = weights.getOrDefault("distance", 1);
        double weightDuration = weights.getOrDefault("time", 1);
        double weightPrice = weights.getOrDefault("price", 1);

        double cost = weightDistance * normalizedDistance +
                weightDuration * normalizedDuration +
                weightPrice * normalizedPrice;

        //System.out.println("Cost for road " + road.getName() + ": " + cost);
        return cost;
    }

    private boolean isGoal(Node currentNode, Road endRoad) {
        boolean isGoal = currentNode.getRoad().equals(endRoad);
        if (isGoal) {
           // System.out.println("Goal node reached: " + currentNode.getRoad().getName());
        }
        return isGoal;
    }

    private Route constructRoute(Node node, List<RouteData> RouteData,Route route) {
        //System.out.println("Constructing route from goal to start...");

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
            routeData.setStatus(road.getStatus());
            routeData.setRoadId(road.getId());
            totalDistance += road.getDistance();
            totalDuration += road.getDuration();
            totalPrice += road.getPrice();

            RouteData.add(0, routeData); // 将节点数据加入路径
            node = node.getParent(); // 移动到父节点
        }

        System.out.println("Total distance: " + totalDistance);
        System.out.println("Total duration: " + totalDuration);
        System.out.println("Total price: " + totalPrice);
        

        route.setRouteData(RouteData); // 设置路径数据
        route.setDistance(String.valueOf(totalDistance));
        route.setDuration(String.valueOf(totalDuration));
        route.setPrice(String.valueOf(totalPrice));
        routeMapper.insertRoute(route);
        return route;
    }

    private List<Road> getNeighbors(Road currentRoad) {
        //System.out.println("Getting neighbors for road: " + currentRoad.getName());
        return roadService.getNeighbors(currentRoad.getId()); // 假设方法返回相邻道路的列表
    }

    private List<Road> getGreenNeighbors(Road currentRoad) {
        //System.out.println("Getting neighbors for road: " + currentRoad.getName());
        return roadService.getGreenNeighbors(currentRoad.getId()); // 假设方法返回相邻道路的列表
    }
    private double heuristic(Road currentRoad, Road endRoad) {
        double deltaX = Math.abs(currentRoad.getStartLat() - endRoad.getStartLat());
        double deltaY = Math.abs(currentRoad.getStartLong() - endRoad.getStartLong());
        double heuristicValue = deltaX + deltaY;
        //System.out.println("Heuristic value for road " + currentRoad.getName() + " to " + endRoad.getName() + ": " + heuristicValue);
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

