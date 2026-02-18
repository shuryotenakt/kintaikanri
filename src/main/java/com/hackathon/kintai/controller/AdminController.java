package com.hackathon.kintai.controller;

import com.hackathon.kintai.model.*;
import com.hackathon.kintai.repository.*;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.List;

@Controller
@RequestMapping("/admin")
public class AdminController {

    @Autowired private UserRepository userRepo;
    @Autowired private AttendanceRepository attendanceRepo;

    @GetMapping
    public String dashboard(@RequestParam(required = false) String userId, Model model) {
        List<User> userList = userRepo.findAll();
        model.addAttribute("userList", userList);

        List<Attendance> histories;
        if (userId != null && !userId.isEmpty()) {
            histories = attendanceRepo.findAllByUserIdOrderByStartTimeDesc(userId);
            model.addAttribute("selectedUserId", userId);
        } else {
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

    // 🆕 ユーザー削除機能
    @PostMapping("/delete-user")
    public String deleteUser(@RequestParam Long targetId, @RequestParam String adminPassword, HttpSession session, RedirectAttributes redirectAttributes) {
        User admin = (User) session.getAttribute("user");
        
        // 1. 管理者のパスワードチェック
        if (!admin.getPassword().equals(adminPassword)) {
            redirectAttributes.addFlashAttribute("error", "パスワードが間違っています。削除できませんでした。");
            return "redirect:/admin";
        }

        // 2. 削除対象のユーザーを取得
        User targetUser = userRepo.findById(targetId).orElse(null);
        if (targetUser != null) {
            // 3. そのユーザーの勤怠データを全て消す（これをしないとゴミデータが残る）
            List<Attendance> userAttendances = attendanceRepo.findAllByUserIdOrderByStartTimeDesc(targetUser.getUserId());
            attendanceRepo.deleteAll(userAttendances);

            // 4. ユーザー本体を削除
            userRepo.delete(targetUser);
            redirectAttributes.addFlashAttribute("success", "ユーザー「" + targetUser.getName() + "」を削除しました。");
        }

        return "redirect:/admin";
    }
}