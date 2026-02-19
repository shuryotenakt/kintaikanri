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

    // ❌ ConcurrentHashMap はもう使わないので削除！

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

        String currentSessionId = session.getId();

        // 🌟 【後勝ち仕様】新しい端末でログインしたら、DBのセッションIDを上書きする！
        user.setCurrentSessionId(currentSessionId);
        userRepo.save(user); // DBに直接書き込む

        session.setAttribute("user", user);
        return "redirect:/partner";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user != null) {
            // DBから最新のユーザー情報を取ってきて、自分のセッションIDなら空にする
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
        // 全ユーザーのログイン状態をDBから強制リセット
        userRepo.findAll().forEach(u -> {
            u.setCurrentSessionId(null);
            userRepo.save(u);
        });
        return "全ユーザーのログイン状態をリセットしました。";
    }
}