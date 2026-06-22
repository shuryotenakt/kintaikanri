package com.hackathon.kintai.controller;

import com.hackathon.kintai.model.*;
import com.hackathon.kintai.repository.*;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/partner")
public class PartnerController {

    @Autowired private AttendanceRepository attendanceRepo;
    @Autowired private UserRepository userRepo;
    @Autowired private ShiftRepository shiftRepo; 

    // セッションチェック共通ロジック
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

    // 従業員ダッシュボード画面表示
    @GetMapping
    public String dashboard(HttpSession session, 
                            HttpServletResponse response, 
                            Model model, 
                            @RequestParam(name = "startDate", required = false) String startDate,
                            @RequestParam(name = "endDate", required = false) String endDate) {
        if (isInvalidSession(session)) return "redirect:/?error=already_logged_in";
        User user = (User) session.getAttribute("user");
        
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        response.setDateHeader("Expires", 0);
        
        Attendance active = attendanceRepo.findTopByUserIdAndEndTimeIsNullOrderByStartTimeDesc(user.getUserId());
        String currentStatus = "OFF";
        if (active != null) {
            currentStatus = (active.getBreakStartTime() != null && active.getBreakEndTime() == null) ? "REST" : "WORK";
        }

        List<Attendance> allHistories = attendanceRepo.findAllByUserIdOrderByStartTimeDesc(user.getUserId());
        List<Attendance> filteredHistories;
        String titleLabel = "全期間の";

        if (startDate != null && !startDate.isEmpty() && endDate != null && !endDate.isEmpty()) {
            try {
                LocalDateTime startOfPeriod = LocalDate.parse(startDate).atStartOfDay();
                LocalDateTime endOfPeriod = LocalDate.parse(endDate).atTime(23, 59, 59);

                filteredHistories = allHistories.stream()
                        .filter(h -> h.getStartTime() != null && 
                                     !h.getStartTime().isBefore(startOfPeriod) && 
                                     !h.getStartTime().isAfter(endOfPeriod))
                        .collect(Collectors.toList());

                DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd");
                titleLabel = startOfPeriod.format(dtf) + " ～ " + endOfPeriod.format(dtf) + " の";

            } catch (Exception e) {
                filteredHistories = allHistories;
                startDate = "";
                endDate = "";
            }
        } else {
            filteredHistories = allHistories;
            startDate = "";
            endDate = "";
        }

        model.addAttribute("user", user);
        model.addAttribute("myHistories", filteredHistories);
        model.addAttribute("status", currentStatus);
        model.addAttribute("startDate", startDate); 
        model.addAttribute("endDate", endDate); 
        model.addAttribute("titleLabel", titleLabel); 

        // ==========================================
        // 🔔 【新機能】25日以降の「翌月分」シフト未提出アラートチェック
        // ==========================================
        LocalDate today = LocalDate.now();
        if (today.getDayOfMonth() >= 25) {
            // 💡 ターゲットを「翌月」の文字列（例：今が6月なら "2026-07"）にする
            String nextMonthStr = today.plusMonths(1).format(DateTimeFormatter.ofPattern("yyyy-MM"));
            
            // shiftsテーブルから、この従業員の「翌月分」のデータが1件でもあるか探す
            Optional<Shift> anyShift = shiftRepo.findAll().stream()
                    .filter(s -> user.getUserId().equals(s.getUserId()) && nextMonthStr.equals(s.getYearMonth()))
                    .findFirst();
            
            // 1件もデータが存在しない（未提出）の場合のみ、アラートを表示
            if (anyShift.isEmpty()) {
                model.addAttribute("showShiftAlert", true);
                model.addAttribute("alertMonth", nextMonthStr);
            }
        }

        return "partner_dash";
    }

    // 出勤打刻
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

    // 休憩開始
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

    // 休憩終了
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

    // 退勤打刻
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

    // パスワード変更
    @PostMapping("/password-reset")
    public String resetPassword(@RequestParam String newPassword, 
                                @RequestParam String confirmPassword, 
                                HttpSession session, 
                                RedirectAttributes ra) {
        if (isInvalidSession(session)) return "redirect:/?error=already_logged_in";
        
        if (newPassword.matches(".*[^\\x21-\\x7e].*")) {
            return "redirect:/partner?error=invalid_characters";
        }

        if (!newPassword.equals(confirmPassword)) {
            return "redirect:/partner?error=password_mismatch";
        }

        User sessionUser = (User) session.getAttribute("user");
        User user = userRepo.findById(sessionUser.getId()).orElseThrow();

        if (user.getPassword().equals(newPassword)) {
            return "redirect:/partner?error=same_as_old";
        }

        user.setPassword(newPassword);
        userRepo.save(user);
        session.setAttribute("user", user);
        ra.addFlashAttribute("success", "パスワードを更新しました。");
        return "redirect:/partner";
    }

    // シフト申請画面を表示する
    @GetMapping("/shift/request")
    public String showShiftRequest(@RequestParam(name = "month", required = false) String month, 
                                   HttpSession session, 
                                   Model model) {
        if (isInvalidSession(session)) return "redirect:/?error=already_logged_in";
        User user = (User) session.getAttribute("user");
        model.addAttribute("user", user);

        if (month == null || month.isEmpty()) {
            month = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
        }
        model.addAttribute("currentMonth", month);

        List<Shift> dbShifts = shiftRepo.findAll();

        Map<String, String> htmlShifts = new HashMap<>();
        for (Shift s : dbShifts) {
            if (user.getUserId().equals(s.getUserId()) && month.equals(s.getYearMonth())) {
                String dayStr = String.valueOf(s.getDay());
                if (s.getShiftIn() != null && !s.getShiftIn().isEmpty()) {
                    htmlShifts.put("shift_in_" + user.getUserId() + "_" + month + "_" + dayStr, s.getShiftIn());
                }
                if (s.getShiftOut() != null && !s.getShiftOut().isEmpty()) {
                    htmlShifts.put("shift_out_" + user.getUserId() + "_" + month + "_" + dayStr, s.getShiftOut());
                }
            }
        }
        model.addAttribute("dbShifts", htmlShifts);

        return "partner_shift_request";
    }

    // シフトデータを保存する
    @PostMapping("/shift/submit")
    public String submitShiftRequest(@RequestParam Map<String, String> allParams, 
                                     @RequestParam(name = "targetMonth", required = false) String targetMonth,
                                     HttpSession session) {
        if (isInvalidSession(session)) return "redirect:/?error=already_logged_in";
        User user = (User) session.getAttribute("user");

        for (Map.Entry<String, String> entry : allParams.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();

            if (key.startsWith("shift_in_")) {
                String[] parts = key.split("_");
                String userId = parts[2];
                String yearMonthStr = parts[3]; 
                int day = Integer.parseInt(parts[4]);

                String outKey = "shift_out_" + userId + "_" + yearMonthStr + "_" + day;
                String startTime = value;
                String endTime = allParams.getOrDefault(outKey, "");

                Optional<Shift> existingShift = shiftRepo.findByUserIdAndYearMonthAndDay(userId, yearMonthStr, day);
                Shift shift;
                if (existingShift.isPresent()) {
                    shift = existingShift.get();
                } else {
                    shift = new Shift();
                    shift.setUserId(userId);
                    shift.setYearMonth(yearMonthStr);
                    shift.setDay(day);
                }

                shift.setShiftIn(startTime);
                shift.setShiftOut(endTime);

                if (startTime.isEmpty() && endTime.isEmpty() && shift.getId() == null) {
                    continue;
                }

                shiftRepo.save(shift);
            }
        }

        String redirectUrl = "/partner/shift/request?success=true";
        if (targetMonth != null && !targetMonth.isEmpty()) {
            redirectUrl += "&month=" + targetMonth;
        }
        return "redirect:" + redirectUrl;
    }
}