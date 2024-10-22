package com.server.server.service;

import com.server.server.data.*;
import com.server.server.mapper.RoadMapper;
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
    public RoadMapper roadMapper;
    public List<Road> roads = roadMapper.getAllRoads();

    private int maxDemand;
    private int available;
    private final Lock lock = new ReentrantLock();
    private final Condition condition = lock.newCondition();
    private final Map<Object, Integer> allocation = new HashMap<>();

    public ResourceManager() {
        this.roadMapper = roadMapper;
        this.roads = roadMapper.getAllRoads();
        this.maxDemand = maxDemand;
        this.available = maxDemand;
    }

    public void registerEntity(Object entity) {
        lock.lock();
        try {
            allocation.put(entity, 0);
        } finally {
            lock.unlock();
        }
    }

    public void requestResources(Object entity, List<RouteData> pathData) throws InterruptedException {
        lock.lock();
        try {
            while (pathData.size() > available) {
                condition.await();
            }
            int totalNeeded = pathData.size();
            int allocated = Math.min(totalNeeded, available);
            allocation.put(entity, allocation.getOrDefault(entity, 0) + allocated);
            this.available -= allocated;
            if (allocated < totalNeeded) {
                condition.signalAll();
            }
        } finally {
            lock.unlock();
        }
    }

    public void releaseResources(Object entity, List<RouteData> pathData) {
        lock.lock();
        try {
            int totalReleased = pathData.size();
            int currentAllocation = allocation.get(entity) - totalReleased;
            allocation.put(entity, currentAllocation);
            this.available += totalReleased;
            if (currentAllocation > 0) {
                condition.signalAll();
            }
        } finally {
            lock.unlock();
        }
    }

    protected void printState() {
        System.out.println("ResourceManager state:");
        System.out.println("Available: " + available);
        System.out.println("Allocation: " + allocation);
    }
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
            List<Road> roads = resourceManager.roadMapper.getAllRoads();

            // 2. 模拟车辆消耗资源
            List<Road> validRoads = new ArrayList<>(); // 用于存储符合if条件的道路
            for (Road road : roads) {
                long id = road.getId();
                if (resourceManager.roadMapper.getRoadById(id).maxLoad >= maxDemand) {
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
                if (resourceManager.roadMapper.getRoadById(id).maxLoad >= maxDemand) {
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
            List<Road> roads = resourceManager.roadMapper.getAllRoads();

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
