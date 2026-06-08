package com.hackathon.kintai.controller;

import com.hackathon.kintai.model.*;
import com.hackathon.kintai.repository.*;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpServletResponse; // 💡 追加
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Controller
@RequestMapping("/admin")
public class AdminController {

    @Autowired private UserRepository userRepo;
    @Autowired private AttendanceRepository attendanceRepo;
    @Autowired private ShiftRepository shiftRepo; // Supabaseのシフトテーブル用リポジトリ

    // 💡 管理者のセッションチェックを行う共通ロジック
    private boolean isInvalidAdminSession(HttpSession session) {
        User sessionUser = (User) session.getAttribute("user");
        if (sessionUser == null || !"ADMIN".equals(sessionUser.getRole())) return true;
        
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

    // 💡 キャッシュを禁止する共通ヘッダー設定
    private void setNoCacheHeaders(HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        response.setDateHeader("Expires", 0);
    }

    private void keepFilters(RedirectAttributes attrs, String userId, String month, String start, String end) {
        if (userId != null && !userId.isEmpty()) attrs.addAttribute("userId", userId);
        if (month != null && !month.isEmpty()) attrs.addAttribute("month", month);
        if (start != null && !start.isEmpty()) attrs.addAttribute("startDate", start);
        if (end != null && !end.isEmpty()) attrs.addAttribute("endDate", end);
    }

    @GetMapping
    public String dashboard(HttpSession session, HttpServletResponse response, Model model,
                            @RequestParam(name = "userId", required = false) String userId,
                            @RequestParam(name = "month", required = false) String month,
                            @RequestParam(name = "startDate", required = false) String startDate,
                            @RequestParam(name = "endDate", required = false) String endDate) {
        // 🌟 1. 管理者チェック & 2. キャッシュ禁止ヘッダー適用
        if (isInvalidAdminSession(session)) return "redirect:/?error=already_logged_in";
        setNoCacheHeaders(response);

        List<User> userlist = userRepo.findAll();
        model.addAttribute("userList", userlist);

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

        // シフトデータの取得・復元処理
        String targetMonth = (month != null && !month.isEmpty()) ? month : YearMonth.now().toString();
        List<Shift> dbShifts = shiftRepo.findAll();

        Map<String, String> savedShifts = new HashMap<>();
        for (Shift s : dbShifts) {
            String currentUid = s.getUserId();
            if ("0".equals(currentUid) || "minus".equals(currentUid) || "none".equals(currentUid)) {
                if ("0".equals(currentUid)) {
                    currentUid = "minus";
                }
                if (s.getShiftIn() != null && !s.getShiftIn().isEmpty()) {
                    savedShifts.put("shift_minus_" + currentUid + "_" + s.getYearMonth() + "_" + s.getDay(), s.getShiftIn());
                }
            } else {
                if (s.getShiftIn() != null && !s.getShiftIn().isEmpty()) {
                    savedShifts.put("shift_in_" + currentUid + "_" + s.getYearMonth() + "_" + s.getDay(), s.getShiftIn());
                }
                if (s.getShiftOut() != null && !s.getShiftOut().isEmpty()) {
                    savedShifts.put("shift_out_" + currentUid + "_" + s.getYearMonth() + "_" + s.getDay(), s.getShiftOut());
                }
            }
        }
        model.addAttribute("savedShifts", savedShifts);

        return "admin_dash";
    }

    @PostMapping("/register")
    public String register(HttpSession session, HttpServletResponse response,
                           @RequestParam String name, @RequestParam String password, @RequestParam String role,
                           @RequestParam(required = false) String filterUserId, @RequestParam(required = false) String filterMonth,
                           @RequestParam(required = false) String filterStartDate, @RequestParam(required = false) String filterEndDate,
                           RedirectAttributes redirectAttributes) {
        if (isInvalidAdminSession(session)) return "redirect:/?error=already_logged_in";
        setNoCacheHeaders(response);

        User user = new User();
        user.setName(name);
        user.setPassword(password);
        user.setRole(role);

        // 💡 重複を絶対に防ぐ安全な採番ロジック
        // DB内の全ユーザーから、最大のuser_id（を数値変換したもの）を見つけて +1 する
        int maxId = userRepo.findAll().stream()
                .map(u -> {
                    try {
                        return Integer.parseInt(u.getUserId());
                    } catch (NumberFormatException e) {
                        return 1000; // 数値変換できないイレギュラーなID（admin等）は1000として扱う
                    }
                })
                .max(Integer::compareTo)
                .orElse(1000); // 1人もいない場合は1000からスタート

        user.setUserId(String.valueOf(maxId + 1)); // 最大のIDの「次の番号」を確実に割り当てる
        userRepo.save(user);

        keepFilters(redirectAttributes, filterUserId, filterMonth, filterStartDate, filterEndDate);
        return "redirect:/admin";
    }

    @PostMapping("/edit")
    public String edit(HttpSession session, HttpServletResponse response,
                       @RequestParam Long id, @RequestParam String startTime, @RequestParam String endTime,
                       @RequestParam(required = false, defaultValue = "0") Integer breakMinutes,
                       @RequestParam(required = false) String filterUserId, @RequestParam(required = false) String filterMonth,
                       @RequestParam(required = false) String filterStartDate, @RequestParam(required = false) String filterEndDate,
                       RedirectAttributes redirectAttributes) {
        if (isInvalidAdminSession(session)) return "redirect:/?error=already_logged_in";
        setNoCacheHeaders(response);

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime start = LocalDateTime.parse(startTime);
        LocalDateTime end = null;

        if (start.isAfter(now)) {
            redirectAttributes.addFlashAttribute("error", "【エラー】未来の日時は登録・修正できません。");
            keepFilters(redirectAttributes, filterUserId, filterMonth, filterStartDate, filterEndDate);
            return "redirect:/admin";
        }

        if (!endTime.isEmpty()) {
            end = LocalDateTime.parse(endTime);
            if (end.isAfter(now)) {
                redirectAttributes.addFlashAttribute("error", "【エラー】未来の日時は登録・修正できません。");
                keepFilters(redirectAttributes, filterUserId, filterMonth, filterStartDate, filterEndDate);
                return "redirect:/admin";
            }
            if (end.isBefore(start)) {
                redirectAttributes.addFlashAttribute("error", "【エラー】退勤時間が、出勤時間より過去に設定されています。修正できませんでした。");
                keepFilters(redirectAttributes, filterUserId, filterMonth, filterStartDate, filterEndDate);
                return "redirect:/admin";
            }
            long totalMinutes = Duration.between(start, end).toMinutes();
            if (breakMinutes > totalMinutes) {
                redirectAttributes.addFlashAttribute("error", "【エラー】休憩時間が勤務時間（拘束時間）をオーバーしています。");
                keepFilters(redirectAttributes, filterUserId, filterMonth, filterStartDate, filterEndDate);
                return "redirect:/admin";
            }
        }

        Attendance a = attendanceRepo.findById(id).orElseThrow();
        a.setStartTime(start);
        a.setEndTime(end);
        a.setBreakMinutes(breakMinutes);
        attendanceRepo.save(a);

        redirectAttributes.addFlashAttribute("success", "打刻を上書き保存しました。");
        keepFilters(redirectAttributes, filterUserId, filterMonth, filterStartDate, filterEndDate);
        return "redirect:/admin";
    }

    @PostMapping("/delete-user")
    public String deleteUser(HttpSession session, HttpServletResponse response,
                             @RequestParam Long targetId, @RequestParam String adminPassword,
                             @RequestParam(required = false) String filterUserId, @RequestParam(required = false) String filterMonth,
                             @RequestParam(required = false) String filterStartDate, @RequestParam(required = false) String filterEndDate,
                             RedirectAttributes redirectAttributes) {
        if (isInvalidAdminSession(session)) return "redirect:/?error=already_logged_in";
        setNoCacheHeaders(response);

        User admin = (User) session.getAttribute("user");
        if (admin != null) {
            if (!admin.getPassword().equals(adminPassword)) {
                redirectAttributes.addFlashAttribute("error", "【エラー】パスワードが間違っています。削除できませんでした。");
                keepFilters(redirectAttributes, filterUserId, filterMonth, filterStartDate, filterEndDate);
                return "redirect:/admin";
            }
        }

        User targetUser = userRepo.findById(targetId).orElse(null);
        if (targetUser != null) {
            List<Attendance> userAttendances = attendanceRepo.findAllByUserIdOrderByStartTimeDesc(targetUser.getUserId());
            attendanceRepo.deleteAll(userAttendances);

            try {
                shiftRepo.deleteByUserId(targetUser.getUserId());
            } catch (Exception e) {
                System.out.println("シフト削除エラー: " + e.getMessage());
            }

            userRepo.delete(targetUser);
            redirectAttributes.addFlashAttribute("success", "ユーザー「" + targetUser.getName() + "」と、対応するシフト・勤怠データを削除しました。");
        }

        keepFilters(redirectAttributes, filterUserId, filterMonth, filterStartDate, filterEndDate);
        return "redirect:/admin";
    }

    @PostMapping("/create-attendance")
    public String createAttendance(HttpSession session, HttpServletResponse response,
                                   @RequestParam String targetUserId, @RequestParam String startTime, @RequestParam String endTime,
                                   @RequestParam(required = false, defaultValue = "0") Integer breakMinutes,
                                   @RequestParam(required = false) String filterUserId, @RequestParam(required = false) String filterMonth,
                                   @RequestParam(required = false) String filterStartDate, @RequestParam(required = false) String filterEndDate,
                                   RedirectAttributes redirectAttributes) {
        if (isInvalidAdminSession(session)) return "redirect:/?error=already_logged_in";
        setNoCacheHeaders(response);

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime start = LocalDateTime.parse(startTime);
        LocalDateTime end = null;

        if (start.isAfter(now)) {
            redirectAttributes.addFlashAttribute("error", "【エラー】未来の日時は登録できません。");
            keepFilters(redirectAttributes, filterUserId, filterMonth, filterStartDate, filterEndDate);
            return "redirect:/admin";
        }

        if (endTime != null && !endTime.isEmpty()) {
            end = LocalDateTime.parse(endTime);
            if (end.isAfter(now)) {
                redirectAttributes.addFlashAttribute("error", "【エラー】未来の日時は登録できません。");
                keepFilters(redirectAttributes, filterUserId, filterMonth, filterStartDate, filterEndDate);
                return "redirect:/admin";
            }
            if (end.isBefore(start)) {
                redirectAttributes.addFlashAttribute("error", "【エラー】退勤時間が、出勤時間より過去に設定されています。登録できませんでした。");
                keepFilters(redirectAttributes, filterUserId, filterMonth, filterStartDate, filterEndDate);
                return "redirect:/admin";
            }
            long totalMinutes = Duration.between(start, end).toMinutes();
            if (breakMinutes > totalMinutes) {
                redirectAttributes.addFlashAttribute("error", "【エラー】休憩時間が勤務時間（拘束時間）をオーバーしています。");
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
            a.setBreakMinutes(breakMinutes);
            attendanceRepo.save(a);
            redirectAttributes.addFlashAttribute("success", targetUser.getName() + " の打刻を新規登録しました。");
        }

        keepFilters(redirectAttributes, filterUserId, filterMonth, filterStartDate, filterEndDate);
        return "redirect:/admin";
    }

    @PostMapping("/delete-attendances")
    public String deleteAttendances(HttpSession session, HttpServletResponse response,
                                    @RequestParam(required = false) List<Long> attendanceIds,
                                    @RequestParam(required = false) String filterUserId, @RequestParam(required = false) String filterMonth,
                                    @RequestParam(required = false) String filterStartDate, @RequestParam(required = false) String filterEndDate,
                                    RedirectAttributes redirectAttributes) {
        if (isInvalidAdminSession(session)) return "redirect:/?error=already_logged_in";
        setNoCacheHeaders(response);

        if (attendanceIds != null && !attendanceIds.isEmpty()) {
            attendanceRepo.deleteAllById(attendanceIds);
            redirectAttributes.addFlashAttribute("success", attendanceIds.size() + "件の打刻履歴を削除しました。");
        } else {
            redirectAttributes.addFlashAttribute("error", "削除する項目が選択されていません。");
        }

        keepFilters(redirectAttributes, filterUserId, filterMonth, filterStartDate, filterEndDate);
        return "redirect:/admin";
    }

    @PostMapping("/save-shifts")
    public String saveShifts(HttpSession session, HttpServletResponse response,
                             @RequestParam Map<String, String> allParams,
                             @RequestParam(required = false) String month, @RequestParam(required = false) String userId,
                             @RequestParam(required = false) String startDate, @RequestParam(required = false) String endDate,
                             RedirectAttributes redirectAttributes) {
        // 🌟 シフト保存ルートにも、管理者チェック & キャッシュ禁止ヘッダーをがっちり統合！
        if (isInvalidAdminSession(session)) return "redirect:/?error=already_logged_in";
        setNoCacheHeaders(response);

        try {
            for (Map.Entry<String, String> entry : allParams.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();

                if (key.startsWith("shift_")) {
                    String[] parts = key.split("_");
                    if (parts.length < 5) continue;

                    String type = parts[1]; // "in", "out", "minus"
                    int dayNum = Integer.parseInt(parts[parts.length - 1]);
                    String recordMonth = parts[parts.length - 2];

                    String shiftUserId = "";
                    if ("minus".equals(type)) {
                        shiftUserId = "0";
                    } else {
                        StringBuilder sb = new StringBuilder();
                        for (int i = 2; i < parts.length - 2; i++) {
                            if (i > 2) sb.append("_");
                            sb.append(parts[i]);
                        }
                        shiftUserId = sb.toString();
                    }

                    Optional<Shift> existingShift = shiftRepo.findByUserIdAndYearMonthAndDay(shiftUserId, recordMonth, dayNum);
                    Shift shift;
                    if (existingShift.isPresent()) {
                        shift = existingShift.get();
                    } else {
                        shift = new Shift();
                        shift.setUserId(shiftUserId);
                        shift.setYearMonth(recordMonth);
                        shift.setDay(dayNum);
                    }

                    if ("in".equals(type)) {
                        shift.setShiftIn(value);
                    } else if ("out".equals(type)) {
                        shift.setShiftOut(value);
                    } else if ("minus".equals(type)) {
                        shift.setShiftIn(value);
                    }

                    shiftRepo.save(shift);
                }
            }
            redirectAttributes.addFlashAttribute("success", "シフトを一括更新・保存しました。");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "保存中にエラーが発生しました: " + e.getMessage());
        }

        keepFilters(redirectAttributes, userId, month, startDate, endDate);
        return "redirect:/admin";
    }
}