package com.server.server.service;

import com.server.server.data.*;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Service
public class ResourceManagerService {

    private final int maxLoad;
    private int available;
    private final Lock lock = new ReentrantLock();
    private final Condition condition = lock.newCondition();
    private final Map<Object, RouteData> routeDataMap = new HashMap<>();
    private final Map<Object, Integer> allocation = new HashMap<>();

    public ResourceManagerService(Road road) {
        this.maxLoad = road.getMaxLoad();
        this.available = maxLoad;
    }

    public void registerEntity(Object entity, double routeData) {
        lock.lock();
        try {
            this.routeDataMap.put(entity, new RouteData(routeData, 0.0,0));
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
                // Not all resources could be allocated, so wait for more to become available
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
                // Resources are still needed, so signal that there are available resources
                condition.signalAll();
            }
        } finally {
            lock.unlock();
        }
    }
}/*
maxLoad：资源池的最大容量。
available：当前可用的资源数量。
lock：一个可重入锁，用于同步访问共享资源。
condition：一个条件变量，与锁一起使用，用于线程间的同步。
routeDataMap：一个HashMap，用于存储实体和其路由数据。
allocation：一个HashMap，用于存储实体的资源分配。
构造函数：ResourceManagerService(Road road)：构造函数，接收一个Road对象，从中获取最大容量maxLoad，并将初始可用资源设置为maxLoad。
方法：

registerEntity(Object entity, double routeData)：注册实体及其路由数据。首先获取锁，然后添加实体和路由数据到routeDataMap和allocation。
requestResources(Object entity, List<RouteData> routeData)：请求资源。首先获取锁，然后检查是否有足够的资源。如果没有，则等待。
如果有，则分配资源，并更新available和allocation。
releaseResources(Object entity, List<RouteData> routeData)：释放资源。首先获取锁，然后减少实体的分配数量，增加available，并可能唤醒等待的线程。
总的来说，这个服务类用于管理一组资源的分配和释放，通过锁和条件变量实现线程安全。*/

