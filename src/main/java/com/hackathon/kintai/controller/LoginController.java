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
        // 1. ユーザーをDBから探す
        User user = userRepo.findByUserId(loginInfo)
                .orElseGet(() -> userRepo.findByName(loginInfo).orElse(null));

        // 2. パスワードが間違っていたら弾く
        if (user == null || !user.getPassword().equals(password)) {
            return "redirect:/?error=invalid_password";
        }

        // 🌟 3. 【DBで二重ログインをブロック】
        // すでにDBに誰かのセッションIDが記録されているかチェック！
        if (user.getCurrentSessionId() != null && !user.getCurrentSessionId().isEmpty()) {
            // もし「今ログインしようとしている自分のセッション」と違うなら、別端末（PC2）とみなして弾く！
            if (!user.getCurrentSessionId().equals(session.getId())) {
                System.out.println("【ブロック】すでに別端末でログイン中です。対象: " + user.getUserId());
                return "redirect:/?error=already_logged_in"; // 赤いエラーメッセージを出して追い返す
            }
        }

        // 4. ログイン成功！自分のセッションIDをDBに書き込んで「使用中」にする
        user.setCurrentSessionId(session.getId());
        userRepo.save(user);

        // セッションにユーザー情報を入れて画面へ進める
        session.setAttribute("user", user);
        return "redirect:/partner";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user != null) {
            // ログアウト時に、DBのセッションIDを空っぽ（null）にして、次の人が入れるようにする
            User dbUser = userRepo.findById(user.getId()).orElse(null);
            if (dbUser != null && session.getId().equals(dbUser.getCurrentSessionId())) {
                dbUser.setCurrentSessionId(null);
                userRepo.save(dbUser);
            }
        }
        session.invalidate();
        return "redirect:/";
    }

    // 🆘 緊急時のロック解除用（ブラウザ強制終了などで誰も入れなくなった時用）
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
        // パスワードを変えたら、安全のためにログイン状態を解除しておく
        user.setCurrentSessionId(null); 
        userRepo.save(user);

        return "redirect:/?reset_success=true";
    }
}