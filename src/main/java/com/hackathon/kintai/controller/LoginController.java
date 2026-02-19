package com.hackathon.kintai.controller;

import com.hackathon.kintai.model.User;
import com.hackathon.kintai.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;

@Controller
public class LoginController {

    @Autowired
    private UserRepository userRepo;

    @GetMapping("/")
    public String loginPage() {
        return "login";
    }

    @PostMapping("/login")
    public String login(@RequestParam String loginInfo, @RequestParam String password, HttpSession session) {
        // 1. まずはIDまたは名前でユーザーを探す
        User user = userRepo.findByUserId(loginInfo)
                .orElseGet(() -> userRepo.findByName(loginInfo).orElse(null));

        // 2. ユーザーが存在しない場合
        if (user == null) {
            return "redirect:/?error=user_not_found";
        }

        // 3. パスワードが一致しない場合
        if (!user.getPassword().equals(password)) {
            return "redirect:/?error=invalid_password";
        }

        // 4. 成功
        session.setAttribute("user", user);
        return "redirect:/partner";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
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

        // パスワード一致チェック
        if (!newPassword.equals(confirmPassword)) {
            return "redirect:/forgot-password?error=password_mismatch";
        }

        // ユーザーID存在チェック
        User user = userRepo.findByUserId(userId).orElse(null);
        if (user == null) {
            return "redirect:/forgot-password?error=user_not_found";
        }

        // 名前一致チェック
        if (!user.getName().equals(name)) {
            return "redirect:/forgot-password?error=name_mismatch";
        }

        // 前と同じパスワードかチェック
        if (newPassword.equals(user.getPassword())) {
            return "redirect:/forgot-password?error=same_as_old";
        }

        // 更新実行
        user.setPassword(newPassword);
        userRepo.save(user);
        return "redirect:/?reset_success";
    }
}