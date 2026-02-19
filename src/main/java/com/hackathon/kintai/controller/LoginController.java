package com.hackathon.kintai.controller;

import com.hackathon.kintai.model.User;
import com.hackathon.kintai.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Controller
public class LoginController {

    @Autowired
    private UserRepository userRepo;

    // ログイン中のユーザーを管理するマップ
    public static final Map<String, String> loginUserMap = new ConcurrentHashMap<>();

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

        String userId = user.getUserId();
        String currentSessionId = session.getId();

        // --- ログ出力（ここをコンソールで確認してください） ---
        System.out.println("====== ログインチェック開始 ======");
        System.out.println("ログイン試行ユーザー: " + userId);
        System.out.println("自分のセッションID: " + currentSessionId);
        System.out.println("現在のマップの状態: " + loginUserMap);

        // 1. 二重ログインチェック
        if (loginUserMap.containsKey(userId)) {
            String activeId = loginUserMap.get(userId);
            System.out.println("既にマップにあるセッションID: " + activeId);

            if (!activeId.equals(currentSessionId)) {
                System.out.println("【結果】別人なので拒否します！");
                return "redirect:/?error=already_logged_in";
            }
            System.out.println("【結果】本人（同セッション）なので通します。");
        } else {
            System.out.println("【結果】新規ログインとして許可します。");
        }

        // 2. 成功処理：マップに保存
        loginUserMap.put(userId, currentSessionId);
        session.setAttribute("user", user);
        
        System.out.println("保存後のマップ: " + loginUserMap);
        System.out.println("====== ログインチェック終了 ======");
        
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

    @GetMapping("/debug/reset-login")
    @ResponseBody
    public String resetLogin() {
        loginUserMap.clear(); 
        return "ログイン状態をリセットしました。";
    }
}