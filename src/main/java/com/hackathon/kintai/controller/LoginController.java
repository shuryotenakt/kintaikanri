package com.hackathon.kintai.controller;

import com.hackathon.kintai.model.User;
import com.hackathon.kintai.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Controller
public class LoginController {

    @Autowired
    private UserRepository userRepo;

    // ログイン中のユーザーを管理
    public static final Map<String, String> loginUserMap = new ConcurrentHashMap<>();

    @GetMapping("/debug/reset-login")
    @ResponseBody
    public String resetLogin() {
        loginUserMap.clear(); 
        return "全ユーザーのログイン予約状態をリセットしました。";
    }

    @GetMapping("/")
    public String loginPage() {
        return "login";
    }

    @PostMapping("/login")
    public String login(@RequestParam String loginInfo, @RequestParam String password, HttpSession session) {
        User user = userRepo.findByUserId(loginInfo)
                .orElseGet(() -> userRepo.findByName(loginInfo).orElse(null));

        if (user == null) {
            return "redirect:/?error=user_not_found";
        }

        if (!user.getPassword().equals(password)) {
            return "redirect:/?error=invalid_password";
        }

        // --- 二重ログインチェック（改善版） ---
        String userId = user.getUserId();
        if (loginUserMap.containsKey(userId)) {
            // ★ポイント：リストにあるセッションIDと、今の自分のセッションIDが違う場合だけ弾く
            // これにより、同じブラウザで「戻る」ボタンや「別タブ」でログインし直す時は弾かれません
            if (!loginUserMap.get(userId).equals(session.getId())) {
                return "redirect:/?error=already_logged_in";
            }
        }

        // 成功処理：最新のセッションIDを保存
        loginUserMap.put(userId, session.getId());
        session.setAttribute("user", user);
        return "redirect:/partner";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user != null) {
            loginUserMap.remove(user.getUserId());
        }
        session.invalidate();
        return "redirect:/";
    }

    @GetMapping("/forgot-password")
    public String forgotPasswordPage() {
        return "forgot-password";
    }

    @PostMapping("/reset-password")
    @Transactional
    public String resetPassword(@RequestParam String userId, 
                                @RequestParam String name, 
                                @RequestParam String newPassword,
                                @RequestParam String confirmPassword) {

        if (!newPassword.equals(confirmPassword)) {
            return "redirect:/forgot-password?error=password_mismatch";
        }

        User user = userRepo.findByUserId(userId).orElse(null);
        if (user == null) {
            return "redirect:/forgot-password?error=user_not_found";
        }

        if (!user.getName().equals(name)) {
            return "redirect:/forgot-password?error=name_mismatch";
        }

        if (newPassword.equals(user.getPassword())) {
            return "redirect:/forgot-password?error=same_as_old";
        }

        user.setPassword(newPassword);
        userRepo.save(user);
        return "redirect:/?reset_success";
    }
}