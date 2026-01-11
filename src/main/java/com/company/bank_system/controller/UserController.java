package com.company.bank_system.controller;


import com.company.bank_system.dto.ChangePasswordRequest;
import com.company.bank_system.dto.ChangePasswordResponse;
import com.company.bank_system.service.UserService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PatchMapping("/changePassword")
    public ChangePasswordResponse changePassword(
            @Valid @RequestBody ChangePasswordRequest request
    ) {
        return userService.changePassword(request);
    }


}
