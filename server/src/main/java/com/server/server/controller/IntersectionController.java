package com.server.server.controller;
import com.server.server.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/intersection")
public class IntersectionController {

    @Autowired
    private IntersectionService intersectionService;

    @PostMapping("/startVehicleSimulation")
    public String startVehicleSimulation(@RequestParam int numberOfVehicles) {
        intersectionService.startVehicleSimulation(numberOfVehicles);
        return "Vehicle simulation started";
    }

    @PostMapping("/startPhilosopherSimulation")
    public String startPhilosopherSimulation(@RequestParam int numberOfPhilosophers) {
        intersectionService.startPhilosopherSimulation(numberOfPhilosophers);
        return "Philosopher simulation started";
    }

    @GetMapping("/finalState")
    public String printFinalState() {
        intersectionService.printFinalState();
        return "Final state printed";
    }
}
