package com.hackathon.kintai.controller;

import com.hackathon.kintai.model.*;
import com.hackathon.kintai.repository.*;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@Controller
@RequestMapping("/partner")
public class PartnerController {

    @Autowired private AttendanceRepository attendanceRepo;

    @GetMapping
    public String dashboard(HttpSession session, Model model) {
        User user = (User) session.getAttribute("user");
        if (user == null) return "redirect:/"; // ログインしてなければ戻す
        
        model.addAttribute("user", user);
        model.addAttribute("myHistories", attendanceRepo.findAllByUserIdOrderByStartTimeDesc(user.getUserId()));
        return "partner_dash"; // パートナー用画面
    }

    @PostMapping("/clock-in")
    public String clockIn(HttpSession session) {
        User user = (User) session.getAttribute("user");
        Attendance a = new Attendance();
        a.setUserId(user.getUserId());
        a.setUserName(user.getName());
        a.setStartTime(LocalDateTime.now());
        attendanceRepo.save(a);
        return "redirect:/partner";
    }

    @PostMapping("/clock-out")
    public String clockOut(HttpSession session) {
        User user = (User) session.getAttribute("user");
        Attendance a = attendanceRepo.findTopByUserIdOrderByStartTimeDesc(user.getUserId());
        if (a != null && a.getEndTime() == null) {
            a.setEndTime(LocalDateTime.now());
            attendanceRepo.save(a);
        }
        return "redirect:/partner";
    }
}