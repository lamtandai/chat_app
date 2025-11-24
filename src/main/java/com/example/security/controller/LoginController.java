package com.example.security.controller;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.example.security.dto.UserPrincipal;
import com.example.security.model.User;
import com.example.security.repository.UserRepository;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
public class LoginController {
    @Autowired
    UserRepository userRepository;

    @GetMapping("/login")
    public String loginPage(@RequestParam(value = "error", required = false) String error,
            @RequestParam(value = "logout", required = false) String logout,
            Model model) {

        if (error != null) {
            model.addAttribute("errorMessage", "Invalid credentials or authentication failed.");
        }

        if (logout != null) {
            model.addAttribute("logoutMessage", "You have been successfully logged out.");
        }

        return "login.html";
    }

    @GetMapping("/homepage")
    public String dashboard(@AuthenticationPrincipal UserDetails principal) {
        if (principal == null) {
            log.info("principal is null");
            return "redirect:/login";
        }
        return "dashboard.html";
    }

    @GetMapping("/user-info")
    @ResponseBody
    public Map<String, Object> getUserInfo(@AuthenticationPrincipal UserDetails principal) {
        Map<String, Object> userInfo = new HashMap<>();

        if (principal == null) {
            userInfo.put("authenticated", false);
            return userInfo;
        }
        Optional<User> userOpt = this.userRepository.findByUsername(principal.getUsername());
        if (userOpt.isEmpty()) {
            return userInfo;
        }
        User user = userOpt.get();
        userInfo.put("authenticated", true);
        userInfo.put("name", user.getName());
        userInfo.put("email", user.getUsername());
        userInfo.put("picture", user.getPicture());
        userInfo.put("provider", user.getProvider());

        // log.info("User info requested for: {}",
        // String.valueOf(principal.getAttribute("name")));

        return userInfo;
    }


}