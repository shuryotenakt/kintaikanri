package com.hackathon.kintai.controller;

import com.hackathon.kintai.model.User;
import com.hackathon.kintai.repository.UserRepository;
import jakarta.servlet.annotation.WebListener; // 💡 追加
import jakarta.servlet.http.HttpSessionEvent;
import jakarta.servlet.http.HttpSessionListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.support.WebApplicationContextUtils;

@WebListener // 💡 追加：大元に頼らず、このファイル単体でシステムに自動認識させます！
@Component
public class SessionListener implements HttpSessionListener {

    @Autowired
    private UserRepository userRepo;

    @Override
    public void sessionCreated(HttpSessionEvent se) {
        System.out.println("【セッション開始】新しいセッションが作成されました。ID: " + se.getSession().getId());
    }

    @Override
    public void sessionDestroyed(HttpSessionEvent se) {
        if (userRepo == null) {
            WebApplicationContextUtils
                .getRequiredWebApplicationContext(se.getSession().getServletContext())
                .getAutowireCapableBeanFactory()
                .autowireBean(this);
        }

        User user = (User) se.getSession().getAttribute("user");
        
        if (user != null) {
            try {
                User dbUser = userRepo.findById(user.getId()).orElse(null);
                
                if (dbUser != null && se.getSession().getId().equals(dbUser.getCurrentSessionId())) {
                    dbUser.setCurrentSessionId(null);
                    userRepo.save(dbUser);
                    System.out.println("【自動ロック解除】セッション終了によりDBのロックを解放しました: " + user.getUserId());
                }
            } catch (Exception e) {
                System.out.println("セッション解放中にエラーが発生しました。: " + e.getMessage());
            }
        }
    }
}