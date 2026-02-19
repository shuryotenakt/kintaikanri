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
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/partner")
public class PartnerController {

    @Autowired private AttendanceRepository attendanceRepo;
    @Autowired private UserRepository userRepo;

    @GetMapping
    public String dashboard(HttpSession session, Model model, 
                            @RequestParam(name = "month", required = false) String month) {
        User user = (User) session.getAttribute("user");
        if (user == null) return "redirect:/";
        
        // 勤務ステータスの判定
        Attendance last = attendanceRepo.findTopByUserIdOrderByStartTimeDesc(user.getUserId());
        String currentStatus = "OFF";
        if (last != null && last.getEndTime() == null) {
            currentStatus = (last.getBreakStartTime() != null && last.getBreakEndTime() == null) ? "REST" : "WORK";
        }

        // 全履歴の取得
        List<Attendance> allHistories = attendanceRepo.findAllByUserIdOrderByStartTimeDesc(user.getUserId());
        
        // 月別フィルタリング（指定がなければ今月を表示）
        String targetMonth = (month != null) ? month : LocalDate.now().toString().substring(0, 7);
        List<Attendance> filteredHistories = allHistories.stream()
                .filter(h -> h.getStartTime().toString().startsWith(targetMonth))
                .collect(Collectors.toList());

        model.addAttribute("user", user);
        model.addAttribute("myHistories", filteredHistories);
        model.addAttribute("status", currentStatus);
        model.addAttribute("selectedMonth", targetMonth); // 画面表示用
        
        return "partner_dash";
    }

    // --- 打刻関連メソッド（変更なし） ---
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