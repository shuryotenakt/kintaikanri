package com.hackathon.kintai.controller;

import com.hackathon.kintai.model.User;
import com.hackathon.kintai.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

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
        User user = userRepo.findByUserId(loginInfo)
                .orElseGet(() -> userRepo.findByName(loginInfo).orElse(null));

        if (user == null || !user.getPassword().equals(password)) {
            return "redirect:/?error=invalid_password";
        }

        // 🌟 【先勝ち仕様】すでにDBにセッションIDが記録されているかチェック
        if (user.getCurrentSessionId() != null && !user.getCurrentSessionId().isEmpty()) {
            if (!user.getCurrentSessionId().equals(session.getId())) {
                System.out.println("【ブロック】すでに別端末でログイン中です。対象: " + user.getUserId());
                return "redirect:/?error=already_logged_in";
            }
        }

        user.setCurrentSessionId(session.getId());
        userRepo.save(user);

        session.setAttribute("user", user);
        return "redirect:/partner";
    }

    // 🌟 ここを変更！GET（リンク押下）でもPOST（Beacon通信）でも受け取れるようにした！
    @RequestMapping(value = "/logout", method = {RequestMethod.GET, RequestMethod.POST})
    public String logout(HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user != null) {
            User dbUser = userRepo.findById(user.getId()).orElse(null);
            if (dbUser != null && session.getId().equals(dbUser.getCurrentSessionId())) {
                dbUser.setCurrentSessionId(null);
                userRepo.save(dbUser);
            }
        }
        session.invalidate();
        return "redirect:/";
    }

    @GetMapping("/debug/reset-login")
    @ResponseBody
    public String resetLogin() {
        userRepo.findAll().forEach(u -> {
            u.setCurrentSessionId(null);
            userRepo.save(u);
        });
        return "全ユーザーのログイン状態をリセットし、ロックを解除しました。";
    }

    // ==========================================
    // パスワード再設定用の機能
    // ==========================================
    
    @GetMapping("/forgot-password")
    public String forgotPasswordPage() {
        return "forgot-password";
    }

    @PostMapping("/reset-password")
    public String resetPassword(@RequestParam String userId,
                                @RequestParam String name,
                                @RequestParam String newPassword,
                                @RequestParam String confirmPassword) {
        
        User user = userRepo.findByUserId(userId).orElse(null);
        
        if (user == null) {
            return "redirect:/forgot-password?error=user_not_found";
        }
        if (!user.getName().equals(name)) {
            return "redirect:/forgot-password?error=name_mismatch";
        }
        if (!newPassword.equals(confirmPassword)) {
            return "redirect:/forgot-password?error=password_mismatch";
        }
        if (user.getPassword().equals(newPassword)) {
            return "redirect:/forgot-password?error=same_as_old";
        }

        user.setPassword(newPassword);
        user.setCurrentSessionId(null); 
        userRepo.save(user);

        return "redirect:/?reset_success=true";
    }
}