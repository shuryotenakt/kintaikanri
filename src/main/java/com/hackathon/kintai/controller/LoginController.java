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
}