package com.hackathon.kintai.controller;

import com.hackathon.kintai.model.*;
import com.hackathon.kintai.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@Controller
@RequestMapping("/admin")
public class AdminController {

    @Autowired private UserRepository userRepo;
    @Autowired private AttendanceRepository attendanceRepo;

    @GetMapping
    public String dashboard(@RequestParam(required = false) String userId, Model model) {
        // 1. ドロップダウン用に全ユーザーを取得
        List<User> userList = userRepo.findAll();
        model.addAttribute("userList", userList);

        // 2. 履歴の取得（ユーザー指定があるかどうかで分岐）
        List<Attendance> histories;
        if (userId != null && !userId.isEmpty()) {
            // 指定されたユーザーの履歴だけ取得
            histories = attendanceRepo.findAllByUserIdOrderByStartTimeDesc(userId);
            model.addAttribute("selectedUserId", userId); // 画面で選択状態を維持するため
        } else {
            // 全員の履歴を取得（新しい順）
            histories = attendanceRepo.findAllByOrderByStartTimeDesc();
        }
        
        model.addAttribute("histories", histories);
        return "admin_dash"; 
    }

    @PostMapping("/register")
    public String register(@RequestParam String name, @RequestParam String password, @RequestParam String role) {
        User user = new User();
        user.setName(name);
        user.setPassword(password);
        user.setRole(role);
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