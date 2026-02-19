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

@Controller
@RequestMapping("/partner")
public class PartnerController {

    @Autowired private AttendanceRepository attendanceRepo;
    @Autowired private UserRepository userRepo;

    @GetMapping
    public String dashboard(HttpSession session, Model model) {
        User user = (User) session.getAttribute("user");
        if (user == null) return "redirect:/";
        
        Attendance last = attendanceRepo.findTopByUserIdOrderByStartTimeDesc(user.getUserId());
        String currentStatus = "OFF";

        if (last != null && last.getEndTime() == null) {
            if (last.getBreakStartTime() != null && last.getBreakEndTime() == null) {
                currentStatus = "REST";
            } else {
                currentStatus = "WORK";
            }
        }

        model.addAttribute("user", user);
        model.addAttribute("myHistories", attendanceRepo.findAllByUserIdOrderByStartTimeDesc(user.getUserId()));
        model.addAttribute("status", currentStatus);
        
        return "partner_dash";
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

    @PostMapping("/break-start")
    public String breakStart(HttpSession session) {
        User user = (User) session.getAttribute("user");
        Attendance a = attendanceRepo.findTopByUserIdOrderByStartTimeDesc(user.getUserId());
        if (a != null && a.getEndTime() == null) {
            a.setBreakStartTime(LocalDateTime.now());
            attendanceRepo.save(a);
        }
        return "redirect:/partner";
    }

    @PostMapping("/break-end")
    public String breakEnd(HttpSession session) {
        User user = (User) session.getAttribute("user");
        Attendance a = attendanceRepo.findTopByUserIdOrderByStartTimeDesc(user.getUserId());
        if (a != null && a.getEndTime() == null) {
            a.setBreakEndTime(LocalDateTime.now());
            attendanceRepo.save(a);
        }
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

    @PostMapping("/password-reset")
    public String resetPassword(@RequestParam String newPassword, HttpSession session, RedirectAttributes ra) {
        User sessionUser = (User) session.getAttribute("user");
        if (sessionUser == null) return "redirect:/";
        
        User user = userRepo.findById(sessionUser.getId()).orElseThrow();
        user.setPassword(newPassword);
        userRepo.save(user);
        
        session.setAttribute("user", user);
        ra.addFlashAttribute("success", "パスワードを更新しました。");
        return "redirect:/partner";
    }
}