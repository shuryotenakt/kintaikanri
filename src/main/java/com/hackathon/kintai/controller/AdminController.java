package com.hackathon.kintai.controller;

import com.hackathon.kintai.model.*;
import com.hackathon.kintai.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@Controller
@RequestMapping("/admin")
public class AdminController {

    @Autowired private UserRepository userRepo;
    @Autowired private AttendanceRepository attendanceRepo;

    @GetMapping
    public String dashboard(Model model) {
        model.addAttribute("users", userRepo.findAll());
        model.addAttribute("histories", attendanceRepo.findAll());
        return "admin_dash"; // 管理者用画面
    }

    @PostMapping("/register")
    public String register(@RequestParam String name, @RequestParam String password, @RequestParam String role) {
        User user = new User();
        user.setName(name);
        user.setPassword(password);
        user.setRole(role);
        // IDを自動割り振り（1000 + 現在の人数 + 1）
        user.setUserId(String.valueOf(1000 + userRepo.count() + 1));
        userRepo.save(user);
        return "redirect:/admin";
    }

    @PostMapping("/edit")
    public String edit(@RequestParam Long id, @RequestParam String startTime, @RequestParam String endTime) {
        Attendance a = attendanceRepo.findById(id).orElseThrow();
        a.setStartTime(LocalDateTime.parse(startTime));
        if (!endTime.isEmpty()) a.setEndTime(LocalDateTime.parse(endTime));
        attendanceRepo.save(a);
        return "redirect:/admin";
    }
}