package com.example.api_gateway.controller;

import com.example.api_gateway.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private JwtUtil jwtUtil;

    @GetMapping("/token")
    public String getToken(@RequestParam("username") String username) {
        // In a real system, you would validate the username/password against a DB here.
        // For testing, we just generate a valid token immediately.
        return jwtUtil.generateToken(username);
    }
}