package com.server.server.service;
import org.springframework.stereotype.Service;

@Service
public interface PhilosopherService {
    void startPhilosophers(int numberOfPhilosophers, int cycles);
}



