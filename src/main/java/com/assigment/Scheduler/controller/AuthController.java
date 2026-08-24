package com.assigment.Scheduler.controller;

import com.assigment.Scheduler.dto.SignupRequest;
import com.assigment.Scheduler.entity.CoordinatorUser;
import com.assigment.Scheduler.repository.CoordinatorUserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final CoordinatorUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthController(CoordinatorUserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/signup")
    public ResponseEntity<Map<String, String>> signup(@RequestBody SignupRequest request) {
        String username = request == null || request.username() == null ? "" : request.username().trim();
        String password = request == null || request.password() == null ? "" : request.password();

        if (username.length() < 3) {
            return ResponseEntity.badRequest().body(Map.of("message", "Username must contain at least 3 characters."));
        }
        if (password.length() < 8) {
            return ResponseEntity.badRequest().body(Map.of("message", "Password must contain at least 8 characters."));
        }
        if (userRepository.existsByUsername(username)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("message", "That username is already registered. Please sign in instead."));
        }

        CoordinatorUser newUser = new CoordinatorUser(
            username,
            passwordEncoder.encode(password),
            "COORDINATOR"
        );
        userRepository.save(newUser);

        return ResponseEntity.status(HttpStatus.CREATED)
            .body(Map.of("status", "CREATED", "message", "Coordinator account created. You can now sign in."));
    }
}
