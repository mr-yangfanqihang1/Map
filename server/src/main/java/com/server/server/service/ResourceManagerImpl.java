package com.server.server.service;
import org.springframework.stereotype.Service;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
@Service
class ResourceManagerImpl implements ResourceManager {
    private final Lock lock = new ReentrantLock();
    private final Condition condition = lock.newCondition();
    private int resourceState = 0;

    @Override
    public void printState() {
        lock.lock();
        try {
            System.out.println("Current resource state: " + resourceState);
        } finally {
            lock.unlock();
        }
    }
}

