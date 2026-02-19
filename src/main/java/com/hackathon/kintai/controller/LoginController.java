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
        User user = userRepo.findByUserIdAndPassword(loginInfo, password)
                .orElseGet(() -> userRepo.findByNameAndPassword(loginInfo, password).orElse(null));

        if (user != null) {
            session.setAttribute("user", user);
            // 👇 ここを変更！ 管理者(ADMIN)でも、まずは打刻画面(/partner)へ飛ばす
            return "redirect:/partner";
        }
        return "redirect:/?error";
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
                        @RequestParam String confirmPassword) { // 引数を追加

    // 1. パスワードの一致チェック
    if (!newPassword.equals(confirmPassword)) {
        return "redirect:/forgot-password?error=password_mismatch";
    }
    
    // IDと名前だけでユーザーを特定する（パスワードは無視する）
    User user = userRepo.findByUserIdAndName(userId, name).orElse(null);

    if (user != null) {
        user.setPassword(newPassword);
        userRepo.save(user); // これで実際にDBが更新されます
        return "redirect:/?reset_success";
    }
    
    return "redirect:/forgot-password?error";
}

}