package com.server.server.service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
interface ResourceManager {
    void printState();
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
        // Example: consume resources
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
        // Example: access resources
        resourceManager.printState();
    }
}

@Service
public class IntersectionService {
    private final ResourceManager resourceManager;
    private final List<Vehicle> vehicles = new ArrayList<>();
    private final List<Philosopher> philosophers = new ArrayList<>();

    public IntersectionService(ResourceManager resourceManager) {
        this.resourceManager = resourceManager;
    }

    public void startVehicleSimulation(int numberOfVehicles) {
        for (int i = 0; i < numberOfVehicles; i++) {
            int maxDemand = new Random().nextInt(2) + 1;
            Vehicle vehicle = new Vehicle(resourceManager, maxDemand);
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
            Philosopher philosopher = new Philosopher(resourceManager, leftResource, rightResource);
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

