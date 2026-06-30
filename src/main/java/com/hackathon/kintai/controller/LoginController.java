package com.hackathon.kintai.controller;

import com.hackathon.kintai.model.User;
import com.hackathon.kintai.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
public class LoginController {

    @Autowired
    private UserRepository userRepo;

    @GetMapping("/")
    public String loginPage(HttpSession session, HttpServletResponse response) {
        // 戻るボタン対策：ログイン画面自体もキャッシュさせない
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        response.setDateHeader("Expires", 0);

        // ログイン済みのユーザーがタブを閉じて、またログインページを開いた場合の処理
        User sessionUser = (User) session.getAttribute("user");
        if (sessionUser != null) {
            User dbUser = userRepo.findById(sessionUser.getId()).orElse(null);
            // DBのセッションIDと現在のブラウザのセッションIDが一致していれば、自動でダッシュボードへ移動
            if (dbUser != null && session.getId().equals(dbUser.getCurrentSessionId())) {
                // 💡【修正】管理者であっても、最初は一律で勤怠ページへリダイレクトします
                return "redirect:/partner";
            }
        }
        return "login";
    }

    @PostMapping("/login")
    public String login(@RequestParam String loginInfo, @RequestParam String password, HttpSession session) {
        User user = userRepo.findByUserId(loginInfo)
                .orElseGet(() -> userRepo.findByName(loginInfo).orElse(null));

        if (user == null || !user.getPassword().equals(password)) {
            return "redirect:/?error=invalid_password";
        }

        // すでにDBにセッションIDが記録されているかチェック
        if (user.getCurrentSessionId() != null && !user.getCurrentSessionId().isEmpty()) {
            if (!user.getCurrentSessionId().equals(session.getId())) {
                System.out.println("【救済】古いセッションIDを検知しましたが、正しい認証のため上書きします。対象: " + user.getUserId());
            }
        }

        // 常に最新のセッションIDで上書き保存
        user.setCurrentSessionId(session.getId());
        userRepo.save(user);

        session.setAttribute("user", user);
        
        // 💡【修正】管理者ロールであっても、まずは全員勤怠画面（/partner）へ飛ばすように統一します
        return "redirect:/partner";
    }

    @RequestMapping(value = "/logout", method = {RequestMethod.GET, RequestMethod.POST})
    public String logout(HttpSession session, HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        response.setDateHeader("Expires", 0);

        User user = (User) session.getAttribute("user");
        if (user != null) {
            User dbUser = userRepo.findById(user.getId()).orElse(null);
            if (dbUser != null && session.getId().equals(dbUser.getCurrentSessionId())) {
                dbUser.setCurrentSessionId(null);
                userRepo.save(dbUser);
            }
        }
        
        session.invalidate();
        return "redirect:/?logout";
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

        if (newPassword.matches(".*[^\\x21-\\x7e].*")) {
            return "redirect:/forgot-password?error=invalid_characters";
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

    @GetMapping("/change-lang")
    public String changeLanguage(@RequestParam("locale") String locale, 
                                 @RequestParam(value = "redirect", defaultValue = "/") String redirect) {
        return "redirect:" + redirect;
    }
}