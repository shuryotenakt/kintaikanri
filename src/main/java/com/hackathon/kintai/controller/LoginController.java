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
        return "login"; // login.htmlを表示
    }

    @PostMapping("/login")
    public String login(@RequestParam String loginInfo, @RequestParam String password, HttpSession session) {
        // 名前 または ユーザーID で検索して、パスワードが一致するか確認
        User user = userRepo.findByUserIdAndPassword(loginInfo, password)
                .orElseGet(() -> userRepo.findByNameAndPassword(loginInfo, password).orElse(null));

        if (user != null) {
            session.setAttribute("user", user); // ログイン情報をセッションに保存
            return user.getRole().equals("ADMIN") ? "redirect:/admin" : "redirect:/partner";
        }
        return "redirect:/?error"; // 失敗したらエラー付きで戻る
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate(); // セッションを消してログアウト
        return "redirect:/";
    }
}