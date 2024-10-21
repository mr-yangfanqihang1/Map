package com.server.server.service;

import com.server.server.data.*;
import com.server.server.mapper.RoadMapper;
import com.server.server.mapper.RouteMapper;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.*;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Service
public class IntersectionService extends ResourceManager {
    private final ResourceManager resourceManager;
    private final List<Vehicle> vehicles = new ArrayList<>();
    private final List<Philosopher> philosophers = new ArrayList<>();

    public IntersectionService(ResourceManager resourceManager) {
        super();
        this.resourceManager = resourceManager;
    }

    public void startVehicleSimulation(int numberOfVehicles) {
        for (int i = 0; i < numberOfVehicles; i++) {
            int maxDemand = new Random().nextInt(2) + 1;
            Vehicle vehicle = new Vehicle(this, maxDemand);
            vehicles.add(vehicle);
            vehicle.start();
        }
        for (Vehicle v : vehicles) {
            try {
                v.join();
            } catch (InterruptedException e) {
                // Log exception
            }
        }
    }

    public void startPhilosopherSimulation(int numberOfPhilosophers) {
        for (int i = 0; i < numberOfPhilosophers; i++) {
            int leftResource = i;
            int rightResource = (i + 1) % numberOfPhilosophers;
            Philosopher philosopher = new Philosopher(this, leftResource, rightResource);
            philosophers.add(philosopher);
            philosopher.start();
        }
        for (Philosopher p : philosophers) {
            try {
                p.join();
            } catch (InterruptedException e) {
                // Log exception
            }
        }
    }

    public void printFinalState() {
        resourceManager.printState();
    }
}

class ResourceManager {
    public RoadMapper data;
    public RouteMapper mapper;
    private int maxLoad = data.getMaxload();
    private int available;
    private final Lock lock = new ReentrantLock();
    private final Condition condition = lock.newCondition();
    private final Map<Object, RouteData> routeDataMap = new HashMap<Object, RouteData>();
    ArrayList<com.server.server.data.PathData> pathdata = mapper.getPathData();
    //double routeData = data.getrouteData();
    private final Map<Object, Integer> allocation = new HashMap<Object, Integer>();


    public ResourceManager(int maxLoad) {
        this.maxLoad = maxLoad;
        this.available = maxLoad;
    }

    public ResourceManager() {
        this.maxLoad = maxLoad;
        this.available = maxLoad;
    }

    public void registerEntity(Object entity, RouteData routeData) {
        lock.lock();
        try {
            this.routeDataMap.put(entity, routeData);
            this.allocation.put(entity, 0);
        } finally {
            lock.unlock();
        }
    }

    public void requestResources(Object entity, List<RouteData> routeData) throws InterruptedException {
        lock.lock();
        try {
            RouteData currentRouteData = routeDataMap.get(entity);
            if (currentRouteData == null) {
                throw new IllegalStateException("Entity not registered.");
            }
            while (routeData.size() > available) {
                condition.await();
            }
            int totalNeeded = routeData.size();
            int allocated = Math.min(totalNeeded, available);
            this.allocation.put(entity, this.allocation.get(entity) + allocated);
            this.available -= allocated;
            if (allocated < totalNeeded) {
                condition.await();
            }
        } finally {
            lock.unlock();
        }
    }

    public void releaseResources(Object entity, List<RouteData> routeData) {
        lock.lock();
        try {
            int totalReleased = routeData.size();
            int currentAllocation = this.allocation.get(entity);
            int newAllocation = currentAllocation - totalReleased;
            this.allocation.put(entity, newAllocation);
            this.available += totalReleased;
            if (newAllocation > 0) {
                condition.signalAll();
            }
        } finally {
            lock.unlock();
        }
    }


    protected void printState() {
        System.out.println("ResourceManager state:");
        System.out.println("Available: " + available);
        System.out.println("RouteDataMap: " + routeDataMap);
        System.out.println("Allocation: " + allocation);
    }


    abstract class SimulationThread extends Thread {
        protected ResourceManager resourceManager;
        protected int id;

        public SimulationThread(ResourceManager resourceManager, int id) {
            this.resourceManager = resourceManager;
            this.id = id;
        }
    }

    class Vehicle extends SimulationThread {
        private int maxDemand;

        public Vehicle(ResourceManager resourceManager, int maxDemand) {
            super(resourceManager, maxDemand);
            this.maxDemand = maxDemand;
        }

        @Override
        public void run() {
            // Business logic for vehicle simulation
            // 1. 从roadMapper data中读取道路资源
            List<Road> roads = resourceManager.data.getAllRoads();

            // 2. 模拟车辆消耗资源
            List<Road> validRoads = new ArrayList<>(); // 用于存储符合if条件的道路
            for (Road road : roads) {
                long id = road.getId();
                if (resourceManager.data.getRoadById(id).maxLoad >= maxDemand) {
                    // 模拟车辆使用该道路资源
                    try {
                        resourceManager.requestResources(this, Collections.singletonList
                                (new RouteData(road.getStartLat(), road.getStartLong(), road.getEndLat(), road.getEndLong(), road.getDistance(), road.getDuration(), road.getPrice())));
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    // 模拟资源使用过程
                    try {
                        Thread.sleep((long) road.getDuration()); // 资源使用时间
                    } catch (InterruptedException e) {
                        // Log exception
                    }
                    resourceManager.releaseResources(this, Collections.singletonList
                            (new RouteData(road.getStartLat(), road.getStartLong(), road.getEndLat(), road.getEndLong(), road.getDistance(), road.getDuration(), road.getPrice())));
                    validRoads.add(road); // 将符合条件的道路添加到列表中
                    break; // 如果只使用一条道路，可以使用break跳出循环
                }
            }

            // 重新遍历符合条件的道路
            for (Road road : validRoads) {
                long id = road.getId();
                if (resourceManager.data.getRoadById(id).maxLoad >= maxDemand) {
                    // 模拟车辆使用该道路资源
                    try {
                        resourceManager.requestResources(this, Collections.singletonList
                                (new RouteData(road.getStartLat(), road.getStartLong(), road.getEndLat(), road.getEndLong(), road.getDistance(), road.getDuration(), road.getPrice())));
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    // 模拟资源使用过程
                    try {
                        Thread.sleep((long) road.getDuration()); // 资源使用时间
                    } catch (InterruptedException e) {
                        // Log exception
                    }
                    resourceManager.releaseResources(this, Collections.singletonList
                            (new RouteData(road.getStartLat(), road.getStartLong(), road.getEndLat(), road.getEndLong(), road.getDistance(), road.getDuration(), road.getPrice())));
                }
            }

            resourceManager.printState();
        }

    }

    class Philosopher extends SimulationThread {
        private int leftResource;
        private int rightResource;

        public Philosopher(ResourceManager resourceManager, int leftResource, int rightResource) {
            super(resourceManager, leftResource);
            this.leftResource = leftResource;
            this.rightResource = rightResource;
        }

        @Override
        public void run() {
            // Business logic for philosopher simulation
            // 1. 从roadMapper data中读取道路资源
            List<Road> roads = resourceManager.data.getAllRoads();

            // 2. 模拟哲学家使用资源
            List<Road> relevantRoads = new ArrayList<>();
            for (Road road : roads) {
                if (road.getId() == leftResource || road.getId() == rightResource) {
                    // 将符合条件的道路资源添加到列表中
                    relevantRoads.add(road);
                }
            }

            for (Road road : relevantRoads) {
                if (road.getId() == leftResource || road.getId() == rightResource) {
                    // 模拟哲学家使用该道路资源
                    try {
                        resourceManager.requestResources(this, Collections.singletonList(
                                new RouteData(road.getStartLat(), road.getStartLong(), road.getEndLat(), road.getEndLong(), road.getDistance(), road.getDuration(), road.getPrice())));
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    // 模拟资源使用过程
                    try {
                        Thread.sleep((long) road.getDuration()); // 随机等待时间，模拟资源使用时间
                    } catch (InterruptedException e) {
                        // Log exception
                    }
                    resourceManager.releaseResources(this, Collections.singletonList(
                            new RouteData(road.getStartLat(), road.getStartLong(), road.getEndLat(), road.getEndLong(), road.getDistance(), road.getDuration(), road.getPrice())));
                    break;
                }
            }

            if (!relevantRoads.isEmpty()) {
                resourceManager.printState();
            }
        }

    }
}