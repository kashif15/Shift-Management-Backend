package com.example.shiftmanagement.service;

import com.example.shiftmanagement.model.ImmutableUserDetails;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private Map<String, UserDetails> users = new HashMap<>();

    @PostConstruct
    public void init() {
        users.put("manager", new ImmutableUserDetails("manager",
            passwordEncoder.encode("manager123"),
            List.of(() -> "ROLE_MANAGER"), true, true, true, true));

        users.put("employee", new ImmutableUserDetails("employee",
            passwordEncoder.encode("employee123"),
            List.of(() -> "ROLE_EMPLOYEE"), true, true, true, true));
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        System.out.println("Loading user: " + username);
        if (users.containsKey(username)) {
            UserDetails user = users.get(username);
            System.out.println("User found: " + username + " with encoded password: " + user.getPassword());
            return user;
        } else {
            System.out.println("User not found: " + username);
            throw new UsernameNotFoundException("User not found with username: " + username);
        }
    }
}
