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


    // 🌟 データベースを参照して不正セッションを弾く
    private boolean isInvalidSession(HttpSession session) {
        User sessionUser = (User) session.getAttribute("user");
        if (sessionUser == null) return true;

        // 毎回DBの最新状態を確認する
        User dbUser = userRepo.findById(sessionUser.getId()).orElse(null);
        if (dbUser == null) {
            session.invalidate();
            return true;
        }

        String currentSessionId = session.getId();

        // DBに登録されているセッションIDと、自分のIDが違う場合
        // ＝「別端末でログインされた」または「ログアウトされた」ので弾く！
        if (dbUser.getCurrentSessionId() == null || !dbUser.getCurrentSessionId().equals(currentSessionId)) {
            session.invalidate(); 
            return true;
        }
        return false;
    }

    @GetMapping
    public String dashboard(HttpSession session, Model model, 
                            @RequestParam(name = "month", required = false) String month) {
        if (isInvalidSession(session)) return "redirect:/?error=already_logged_in";
        User user = (User) session.getAttribute("user");
        
        Attendance last = attendanceRepo.findTopByUserIdOrderByStartTimeDesc(user.getUserId());
        String currentStatus = "OFF";
        if (last != null && last.getEndTime() == null) {
            currentStatus = (last.getBreakStartTime() != null && last.getBreakEndTime() == null) ? "REST" : "WORK";
        }

        List<Attendance> allHistories = attendanceRepo.findAllByUserIdOrderByStartTimeDesc(user.getUserId());
        


        String targetMonth = (month != null) ? month : LocalDate.now().toString().substring(0, 7);
        List<Attendance> filteredHistories = allHistories.stream()
                .filter(h -> h.getStartTime() != null && h.getStartTime().toString().startsWith(targetMonth))
                .collect(Collectors.toList());

        model.addAttribute("user", user);
        model.addAttribute("myHistories", filteredHistories);
        model.addAttribute("status", currentStatus);
        model.addAttribute("selectedMonth", targetMonth); 
        
        return "partner_dash";
    }

    @PostMapping("/clock-in")
    public String clockIn(HttpSession session) {
        if (isInvalidSession(session)) return "redirect:/?error=already_logged_in";
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
        if (isInvalidSession(session)) return "redirect:/?error=already_logged_in";
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
        if (isInvalidSession(session)) return "redirect:/?error=already_logged_in";
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
        if (isInvalidSession(session)) return "redirect:/?error=already_logged_in";
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
        if (isInvalidSession(session)) return "redirect:/?error=already_logged_in";
        User sessionUser = (User) session.getAttribute("user");
        User user = userRepo.findById(sessionUser.getId()).orElseThrow();
        user.setPassword(newPassword);
        userRepo.save(user);
        session.setAttribute("user", user);
        ra.addFlashAttribute("success", "パスワードを更新しました。");
        return "redirect:/partner";
    }
}