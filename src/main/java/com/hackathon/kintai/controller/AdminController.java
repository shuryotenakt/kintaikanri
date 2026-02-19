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

    // 絞り込み状態を次の画面に引き継ぐための便利メソッド
    private void keepFilters(RedirectAttributes attrs, String userId, String month, String start, String end) {
        if (userId != null && !userId.isEmpty()) attrs.addAttribute("userId", userId);
        if (month != null && !month.isEmpty()) attrs.addAttribute("month", month);
        if (start != null && !start.isEmpty()) attrs.addAttribute("startDate", start);
        if (end != null && !end.isEmpty()) attrs.addAttribute("endDate", end);
    }

    @GetMapping
    public String dashboard(@RequestParam(required = false) String userId,
                            @RequestParam(required = false) String month,
                            @RequestParam(required = false) String startDate,
                            @RequestParam(required = false) String endDate,
                            Model model) {
        
        List<User> userList = userRepo.findAll();
        model.addAttribute("userList", userList);

        LocalDateTime startDatetime = null;
        LocalDateTime endDatetime = null;

        if (startDate != null && !startDate.isEmpty() && endDate != null && !endDate.isEmpty()) {
            startDatetime = LocalDate.parse(startDate).atStartOfDay();
            endDatetime = LocalDate.parse(endDate).plusDays(1).atStartOfDay().minusNanos(1);
        } else if (month != null && !month.isEmpty()) {
            YearMonth ym = YearMonth.parse(month);
            startDatetime = ym.atDay(1).atStartOfDay();
            endDatetime = ym.atEndOfMonth().plusDays(1).atStartOfDay().minusNanos(1);
        }

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
        
        model.addAttribute("selectedUserId", userId);
        model.addAttribute("selectedMonth", month);
        model.addAttribute("selectedStartDate", startDate);
        model.addAttribute("selectedEndDate", endDate);
        model.addAttribute("histories", histories);
        return "admin_dash"; 
    }

    @PostMapping("/register")
    public String register(@RequestParam String name, @RequestParam String password, @RequestParam String role,
                           @RequestParam(required = false) String filterUserId, @RequestParam(required = false) String filterMonth, 
                           @RequestParam(required = false) String filterStartDate, @RequestParam(required = false) String filterEndDate,
                           RedirectAttributes redirectAttributes) {
        User user = new User();
        user.setName(name);
        user.setPassword(password);
        user.setRole(role);
        user.setUserId(String.valueOf(1000 + userRepo.count() + 1));
        userRepo.save(user);
        
        keepFilters(redirectAttributes, filterUserId, filterMonth, filterStartDate, filterEndDate);
        return "redirect:/admin";
    }

    @PostMapping("/edit")
    public String edit(@RequestParam Long id, @RequestParam String startTime, @RequestParam String endTime,
                       @RequestParam(required = false) String filterUserId, @RequestParam(required = false) String filterMonth, 
                       @RequestParam(required = false) String filterStartDate, @RequestParam(required = false) String filterEndDate,
                       RedirectAttributes redirectAttributes) {
        
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime start = LocalDateTime.parse(startTime);
        LocalDateTime end = null;

        // 🛑 未来の出勤時間をブロック
        if (start.isAfter(now)) {
            redirectAttributes.addFlashAttribute("error", "【エラー】未来の日時は登録・修正できません。");
            keepFilters(redirectAttributes, filterUserId, filterMonth, filterStartDate, filterEndDate);
            return "redirect:/admin";
        }

        // 🛑 強固なエラーチェック（退勤）
        if (!endTime.isEmpty()) {
            end = LocalDateTime.parse(endTime);
            // 未来の退勤時間をブロック
            if (end.isAfter(now)) {
                redirectAttributes.addFlashAttribute("error", "【エラー】未来の日時は登録・修正できません。");
                keepFilters(redirectAttributes, filterUserId, filterMonth, filterStartDate, filterEndDate);
                return "redirect:/admin";
            }
            // 過去への矛盾をブロック
            if (end.isBefore(start)) {
                redirectAttributes.addFlashAttribute("error", "【エラー】退勤時間が、出勤時間より過去に設定されています。修正できませんでした。");
                keepFilters(redirectAttributes, filterUserId, filterMonth, filterStartDate, filterEndDate);
                return "redirect:/admin";
            }
        }

        Attendance a = attendanceRepo.findById(id).orElseThrow();
        a.setStartTime(start);
        a.setEndTime(end);
        attendanceRepo.save(a);
        
        redirectAttributes.addFlashAttribute("success", "打刻を上書き保存しました。");
        keepFilters(redirectAttributes, filterUserId, filterMonth, filterStartDate, filterEndDate);
        return "redirect:/admin";
    }

    @PostMapping("/delete-user")
    public String deleteUser(@RequestParam Long targetId, @RequestParam String adminPassword, HttpSession session, 
                             @RequestParam(required = false) String filterUserId, @RequestParam(required = false) String filterMonth, 
                             @RequestParam(required = false) String filterStartDate, @RequestParam(required = false) String filterEndDate,
                             RedirectAttributes redirectAttributes) {
        User admin = (User) session.getAttribute("user");
        if (!admin.getPassword().equals(adminPassword)) {
            redirectAttributes.addFlashAttribute("error", "パスワードが間違っています。削除できませんでした。");
            keepFilters(redirectAttributes, filterUserId, filterMonth, filterStartDate, filterEndDate);
            return "redirect:/admin";
        }
        User targetUser = userRepo.findById(targetId).orElse(null);
        if (targetUser != null) {
            List<Attendance> userAttendances = attendanceRepo.findAllByUserIdOrderByStartTimeDesc(targetUser.getUserId());
            attendanceRepo.deleteAll(userAttendances);
            userRepo.delete(targetUser);
            redirectAttributes.addFlashAttribute("success", "ユーザー「" + targetUser.getName() + "」を削除しました。");
        }
        keepFilters(redirectAttributes, filterUserId, filterMonth, filterStartDate, filterEndDate);
        return "redirect:/admin";
    }

    @PostMapping("/create-attendance")
    public String createAttendance(@RequestParam String targetUserId, @RequestParam String startTime, @RequestParam(required = false) String endTime, 
                                   @RequestParam(required = false) String filterUserId, @RequestParam(required = false) String filterMonth, 
                                   @RequestParam(required = false) String filterStartDate, @RequestParam(required = false) String filterEndDate,
                                   RedirectAttributes redirectAttributes) {
        
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime start = LocalDateTime.parse(startTime);
        LocalDateTime end = null;

        // 🛑 未来の出勤時間をブロック
        if (start.isAfter(now)) {
            redirectAttributes.addFlashAttribute("error", "【エラー】未来の日時は登録できません。");
            keepFilters(redirectAttributes, filterUserId, filterMonth, filterStartDate, filterEndDate);
            return "redirect:/admin";
        }

        // 🛑 強固なエラーチェック（退勤）
        if (endTime != null && !endTime.isEmpty()) {
            end = LocalDateTime.parse(endTime);
            // 未来の退勤時間をブロック
            if (end.isAfter(now)) {
                redirectAttributes.addFlashAttribute("error", "【エラー】未来の日時は登録できません。");
                keepFilters(redirectAttributes, filterUserId, filterMonth, filterStartDate, filterEndDate);
                return "redirect:/admin";
            }
            // 過去への矛盾をブロック
            if (end.isBefore(start)) {
                redirectAttributes.addFlashAttribute("error", "【エラー】退勤時間が、出勤時間より過去に設定されています。登録できませんでした。");
                keepFilters(redirectAttributes, filterUserId, filterMonth, filterStartDate, filterEndDate);
                return "redirect:/admin";
            }
        }

        User targetUser = userRepo.findAll().stream().filter(u -> u.getUserId().equals(targetUserId)).findFirst().orElse(null);

        if (targetUser != null) {
            Attendance a = new Attendance();
            a.setUserId(targetUser.getUserId());
            a.setUserName(targetUser.getName());
            a.setStartTime(start);
            a.setEndTime(end);
            attendanceRepo.save(a);
            redirectAttributes.addFlashAttribute("success", targetUser.getName() + " の打刻を新規登録しました。");
        }
        keepFilters(redirectAttributes, filterUserId, filterMonth, filterStartDate, filterEndDate);
        return "redirect:/admin";
    }

    @PostMapping("/delete-attendances")
    public String deleteAttendances(@RequestParam(required = false) List<Long> attendanceIds, 
                                    @RequestParam(required = false) String filterUserId, @RequestParam(required = false) String filterMonth, 
                                    @RequestParam(required = false) String filterStartDate, @RequestParam(required = false) String filterEndDate,
                                    RedirectAttributes redirectAttributes) {
        if (attendanceIds != null && !attendanceIds.isEmpty()) {
            attendanceRepo.deleteAllById(attendanceIds);
            redirectAttributes.addFlashAttribute("success", attendanceIds.size() + "件の打刻履歴を削除しました。");
        } else {
            redirectAttributes.addFlashAttribute("error", "削除する項目が選択されていません。");
        }
        keepFilters(redirectAttributes, filterUserId, filterMonth, filterStartDate, filterEndDate);
        return "redirect:/admin";
    }
}