package com.hackathon.kintai.controller;

import com.hackathon.kintai.model.*;
import com.hackathon.kintai.repository.*;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

@Controller
@RequestMapping("/admin")
public class AdminController {

    @Autowired private UserRepository userRepo;
    @Autowired private AttendanceRepository attendanceRepo;

    @GetMapping
    public String dashboard(@RequestParam(required = false) String userId,
                            @RequestParam(required = false) String month,
                            @RequestParam(required = false) String startDate,
                            @RequestParam(required = false) String endDate,
                            Model model) {
        
        List<User> userList = userRepo.findAll();
        model.addAttribute("userList", userList);

        // 1. 検索期間の作成（月指定 or 範囲指定）
        LocalDateTime startDatetime = null;
        LocalDateTime endDatetime = null;

        if (startDate != null && !startDate.isEmpty() && endDate != null && !endDate.isEmpty()) {
            // 範囲指定が優先
            startDatetime = LocalDate.parse(startDate).atStartOfDay();
            endDatetime = LocalDate.parse(endDate).plusDays(1).atStartOfDay().minusNanos(1);
        } else if (month != null && !month.isEmpty()) {
            // 月指定の場合、その月の1日から月末までを計算
            YearMonth ym = YearMonth.parse(month);
            startDatetime = ym.atDay(1).atStartOfDay();
            endDatetime = ym.atEndOfMonth().plusDays(1).atStartOfDay().minusNanos(1);
        }

        // 2. データベースから検索
        List<Attendance> histories;
        boolean hasUser = (userId != null && !userId.isEmpty());
        boolean hasDate = (startDatetime != null && endDatetime != null);

        if (hasUser && hasDate) {
            histories = attendanceRepo.findAllByUserIdAndStartTimeBetweenOrderByStartTimeDesc(userId, startDatetime, endDatetime);
        } else if (hasUser) {
            histories = attendanceRepo.findAllByUserIdOrderByStartTimeDesc(userId);
        } else if (hasDate) {
            histories = attendanceRepo.findAllByStartTimeBetweenOrderByStartTimeDesc(startDatetime, endDatetime);
        } else {
            histories = attendanceRepo.findAllByOrderByStartTimeDesc();
        }
        
        // 3. 画面に入力状態を保持するためのデータ渡し
        model.addAttribute("selectedUserId", userId);
        model.addAttribute("selectedMonth", month);
        model.addAttribute("selectedStartDate", startDate);
        model.addAttribute("selectedEndDate", endDate);
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

    @PostMapping("/delete-user")
    public String deleteUser(@RequestParam Long targetId, @RequestParam String adminPassword, HttpSession session, RedirectAttributes redirectAttributes) {
        User admin = (User) session.getAttribute("user");
        if (!admin.getPassword().equals(adminPassword)) {
            redirectAttributes.addFlashAttribute("error", "パスワードが間違っています。削除できませんでした。");
            return "redirect:/admin";
        }
        User targetUser = userRepo.findById(targetId).orElse(null);
        if (targetUser != null) {
            List<Attendance> userAttendances = attendanceRepo.findAllByUserIdOrderByStartTimeDesc(targetUser.getUserId());
            attendanceRepo.deleteAll(userAttendances);
            userRepo.delete(targetUser);
            redirectAttributes.addFlashAttribute("success", "ユーザー「" + targetUser.getName() + "」を削除しました。");
        }
        return "redirect:/admin";
    }
}