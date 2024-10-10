package com.server.server.request.traffic;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class TrafficDataRequest implements Comparable<TrafficDataRequest> {
    private static final AtomicInteger counter = new AtomicInteger(0);
    private final int id;
    private long createdTime;
    private int priority;

    public TrafficDataRequest() {
        this.id = counter.incrementAndGet();
        this.createdTime = System.currentTimeMillis();
        this.priority = 1; // 默认优先级
    }
    public void setPriority(int priority) {
        this.priority = priority;
    }
    public void increasePriority() {
        this.priority++; // 增加优先级
    }

    public long getCreatedTime() {
        return createdTime;
    }

    public int getId() {
        return id;
    }

    public int getPriority() {
        return priority;
    }

    @Override
    public int compareTo(TrafficDataRequest other) {
        // 优先级高的请求排在前面
        int priorityComparison = Integer.compare(other.priority, this.priority);
        if (priorityComparison != 0) {
            return priorityComparison;
        }
        // 如果优先级相同，按创建时间排序
        return Long.compare(this.createdTime, other.createdTime);
    }
}






