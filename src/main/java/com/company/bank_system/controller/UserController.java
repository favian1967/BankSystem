package com.company.bank_system.controller;

import com.company.bank_system.entity.User;
import com.company.bank_system.repo.UserRepository;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UserController {

    private final UserRepository userRepository;

    public UserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @PostMapping
    public String addUser(@RequestBody User user){
        userRepository.save(user);
        return user.toString();
    }

}
