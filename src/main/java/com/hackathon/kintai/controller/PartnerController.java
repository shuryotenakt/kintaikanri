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
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/partner")
public class PartnerController {

    @Autowired private AttendanceRepository attendanceRepo;
    @Autowired private UserRepository userRepo;

    private boolean isInvalidSession(HttpSession session) {
        User sessionUser = (User) session.getAttribute("user");
        if (sessionUser == null) return true;
        User dbUser = userRepo.findById(sessionUser.getId()).orElse(null);
        if (dbUser == null) {
            session.invalidate();
            return true;
        }
        String currentSessionId = session.getId();
        if (dbUser.getCurrentSessionId() == null || !dbUser.getCurrentSessionId().equals(currentSessionId)) {
            session.invalidate(); 
            return true;
        }
        return false;
    }

    @GetMapping
    public String dashboard(HttpSession session, Model model, 
                            @RequestParam(name = "startDate", required = false) String startDate,
                            @RequestParam(name = "endDate", required = false) String endDate) {
        if (isInvalidSession(session)) return "redirect:/?error=already_logged_in";
        User user = (User) session.getAttribute("user");
        
        // 1. ステータス判定（既存ロジック通り）
        Attendance active = attendanceRepo.findTopByUserIdAndEndTimeIsNullOrderByStartTimeDesc(user.getUserId());
        String currentStatus = "OFF";
        if (active != null) {
            currentStatus = (active.getBreakStartTime() != null && active.getBreakEndTime() == null) ? "REST" : "WORK";
        }

        // 2. 全履歴を取得
        List<Attendance> allHistories = attendanceRepo.findAllByUserIdOrderByStartTimeDesc(user.getUserId());
        List<Attendance> filteredHistories;
        String titleLabel = "全期間の";

        // 3. 期間検索（startDate と endDate が両方存在する場合）のフィルタリング処理
        if (startDate != null && !startDate.isEmpty() && endDate != null && !endDate.isEmpty()) {
            try {
                // 検索の開始日（00:00:00）と 終了日（23:59:59）を設定
                LocalDateTime startOfPeriod = LocalDate.parse(startDate).atStartOfDay();
                LocalDateTime endOfPeriod = LocalDate.parse(endDate).atTime(23, 59, 59);

                filteredHistories = allHistories.stream()
                        .filter(h -> h.getStartTime() != null && 
                                    !h.getStartTime().isBefore(startOfPeriod) && 
                                    !h.getStartTime().isAfter(endOfPeriod))
                        .collect(Collectors.toList());

                // カレンダーのタイトル用に成形
                DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd");
                titleLabel = startOfPeriod.format(dtf) + " ～ " + endOfPeriod.format(dtf) + " の";

            } catch (Exception e) {
                // 日付解析エラー等の場合は安全のため全件表示にする
                filteredHistories = allHistories;
                startDate = "";
                endDate = "";
            }
        } else {
            // 検索条件がない場合は、既存データの全件を表示する（これで出勤データが消えずに表示されます）
            filteredHistories = allHistories;
            startDate = "";
            endDate = "";
        }

        // 4. 画面（Thymeleaf）へデータを渡す（変数の名前を保持用カレンダーと完全一致させています）
        model.addAttribute("user", user);
        model.addAttribute("myHistories", filteredHistories);
        model.addAttribute("status", currentStatus);
        model.addAttribute("startDate", startDate); 
        model.addAttribute("endDate", endDate); 
        model.addAttribute("titleLabel", titleLabel); 

        return "partner_dash";
    }

    @PostMapping("/clock-in")
    public String clockIn(HttpSession session) {
        if (isInvalidSession(session)) return "redirect:/?error=already_logged_in";
        User user = (User) session.getAttribute("user");
        
        Attendance active = attendanceRepo.findTopByUserIdAndEndTimeIsNullOrderByStartTimeDesc(user.getUserId());
        if (active == null) {
            Attendance a = new Attendance();
            a.setUserId(user.getUserId());
            a.setUserName(user.getName());
            a.setStartTime(LocalDateTime.now());
            attendanceRepo.save(a);
        }
        return "redirect:/partner";
    }

    @PostMapping("/break-start")
    public String breakStart(HttpSession session) {
        if (isInvalidSession(session)) return "redirect:/?error=already_logged_in";
        User user = (User) session.getAttribute("user");
        Attendance a = attendanceRepo.findTopByUserIdAndEndTimeIsNullOrderByStartTimeDesc(user.getUserId());
        
        if (a != null && a.getBreakStartTime() == null) {
            a.setBreakStartTime(LocalDateTime.now());
            attendanceRepo.save(a);
        }
        return "redirect:/partner";
    }

    @PostMapping("/break-end")
    public String breakEnd(HttpSession session) {
        if (isInvalidSession(session)) return "redirect:/?error=already_logged_in";
        User user = (User) session.getAttribute("user");
        Attendance a = attendanceRepo.findTopByUserIdAndEndTimeIsNullOrderByStartTimeDesc(user.getUserId());
        
        if (a != null && a.getBreakStartTime() != null && a.getBreakEndTime() == null) {
            a.setBreakEndTime(LocalDateTime.now());
            attendanceRepo.save(a);
        }
        return "redirect:/partner";
    }

    @PostMapping("/clock-out")
    public String clockOut(HttpSession session) {
        if (isInvalidSession(session)) return "redirect:/?error=already_logged_in";
        User user = (User) session.getAttribute("user");
        Attendance a = attendanceRepo.findTopByUserIdAndEndTimeIsNullOrderByStartTimeDesc(user.getUserId());
        
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