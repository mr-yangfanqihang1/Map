package com.server.server.service;

import com.server.server.consumer.TrafficDataConsumer;
import com.server.server.data.*;
import com.server.server.mapper.RoadMapper;
import com.server.server.mapper.TrafficDataMapper;
import com.server.server.mapper.UserMapper;
import com.server.server.request.traffic.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import javax.annotation.PostConstruct;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.PriorityBlockingQueue;

@Service
public class TrafficDataService {
    
    @Autowired
    private TrafficDataMapper trafficDataMapper;

    @Autowired
    private RoadMapper roadMapper;
    @Autowired
    private UserMapper userMapper;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate; // 自动注入RedisTemplate



    // 动态优先级队列
    private final PriorityBlockingQueue<TrafficDataRequest> queue = new PriorityBlockingQueue<>();

    // 存储查询请求结果的映射
    private final ConcurrentHashMap<Integer, List<TrafficData>> queryResults = new ConcurrentHashMap<>();

    // 线程池
    private final ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();

    @PostConstruct
    public void init() {
        // 初始化线程池设置
        taskExecutor.setCorePoolSize(5);
        taskExecutor.setMaxPoolSize(10);
        taskExecutor.setQueueCapacity(25);
        taskExecutor.initialize();

        System.out.println("Consumer thread pool initialized...");
        
        // 将消费者任务提交到线程池
        taskExecutor.submit(new TrafficDataConsumer(queue, trafficDataMapper, roadMapper,userMapper, queryResults, redisTemplate)); 
    }


    public void uploadTrafficData(TrafficData trafficData) {
        long start = System.currentTimeMillis();
        // 将插入请求放入队列
        TrafficDataInsertRequest insertRequest = new TrafficDataInsertRequest(trafficData);
        queue.offer(insertRequest);
        long end = System.currentTimeMillis();
        System.out.println("Insert request added to queue: " + trafficData+"\n"+"Add to queue time: "+(end-start)+"ms");
    }

    public void updateTrafficData(TrafficData trafficData) {
        long start = System.currentTimeMillis();
        // 将更新请求放入队列
        TrafficDataUpdateRequest updateRequest = new TrafficDataUpdateRequest(trafficData);
        queue.offer(updateRequest);
        long end = System.currentTimeMillis();
        System.out.println("Update request added to queue: " + trafficData+"\n"+"Add to queue time: "+(end-start)+"ms");
    }

    public List<TrafficData> getTrafficDataByRoadId(int roadId) {
        // 将查询请求放入队列
        TrafficDataQueryRequest queryRequest = new TrafficDataQueryRequest(roadId);
        queue.offer(queryRequest);
        System.out.println("Query request added to queue for roadId: " + roadId);
        
        // 等待查询结果
        while (!queryResults.containsKey(roadId)) {
            try {
                Thread.sleep(100); // 等待结果
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return null;
            }
        }

        
        // 返回查询结果
        List<TrafficData> result = queryResults.get(roadId);
        queryResults.remove(roadId); // 清理已处理的结果
        return result;
    }
}
