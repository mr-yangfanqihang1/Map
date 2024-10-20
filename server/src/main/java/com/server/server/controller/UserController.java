package com.server.server.controller;
import com.server.server.service.*;
import com.server.server.data.*;
import com.sun.xml.bind.v2.schemagen.xmlschema.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController {
    @Autowired
    private UserService userService;

    @PostMapping("/create")
    public void createUser(@RequestBody User user) {
        userService.createUser(user);
    }
    @PostMapping("/update")
    public void updateUser(@RequestBody User user) {
        System.out.println("user update"+user);
        userService.updateUser(user);
    }
    @GetMapping("/{id}")
    public User getUserById(@PathVariable int id) {
        return userService.getUserById(id);
    }
}