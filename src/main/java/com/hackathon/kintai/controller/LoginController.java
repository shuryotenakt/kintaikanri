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
        // ユーザー検索
        User user = userRepo.findByUserId(loginInfo)
                .orElseGet(() -> userRepo.findByName(loginInfo).orElse(null));

        // 認証チェック
        if (user == null || !user.getPassword().equals(password)) {
            return "redirect:/?error=invalid_password";
        }

        String currentSessionId = session.getId();

        // 🌟 【先勝ち仕様】すでにDBにセッションIDが記録されている ＝ 誰かがログイン中！
        if (user.getCurrentSessionId() != null && !user.getCurrentSessionId().isEmpty()) {
            if (!user.getCurrentSessionId().equals(currentSessionId)) {
                System.out.println("【ブロック】不正ログインを検知しました。対象: " + user.getUserId());
                return "redirect:/?error=already_logged_in";
            }
        }

        // 誰もログインしていない（または自分自身）なら、DBに新しいセッションIDを保存
        user.setCurrentSessionId(currentSessionId);
        userRepo.save(user);

        session.setAttribute("user", user);
        return "redirect:/partner";
    }

    @GetMapping("/logout")
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
        return "全ユーザーのログイン状態をリセットしました。";
    }

    // ==========================================
    // 🆕 パスワード再設定用の機能（ここを追加！）
    // ==========================================
    
    // 1. パスワード再設定画面を表示する
    @GetMapping("/forgot-password")
    public String forgotPasswordPage() {
        return "forgot-password";
    }

    // 2. フォームから送られてきた新しいパスワードを保存する
    @PostMapping("/reset-password")
    public String resetPassword(@RequestParam String userId,
                                @RequestParam String name,
                                @RequestParam String newPassword,
                                @RequestParam String confirmPassword) {
        
        User user = userRepo.findByUserId(userId).orElse(null);
        
        // エラーチェック（HTMLの指定に合わせてエラーメッセージを出し分ける）
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

        // 全てクリアしたら、新しいパスワードをセット
        user.setPassword(newPassword);
        
        // パスワードを変えたので、もしログイン状態だった場合は強制的にロックを解除する
        user.setCurrentSessionId(null); 
        userRepo.save(user);

        // 成功したらログイン画面に戻して、成功メッセージを出す
        return "redirect:/?reset_success=true";
    }
}