package com.hackathon.kintai.controller;

import com.hackathon.kintai.model.User;
import com.hackathon.kintai.repository.UserRepository;
import jakarta.servlet.http.HttpSessionEvent;
import jakarta.servlet.http.HttpSessionListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class SessionListener implements HttpSessionListener {

    @Autowired
    private UserRepository userRepo;

    @Override
    public void sessionDestroyed(HttpSessionEvent se) {
        // セッションが切れた（5分間操作なし、またはログアウト）時に自動で呼ばれる
        User user = (User) se.getSession().getAttribute("user");
        
        if (user != null) {
            try {
                // DBから最新のユーザー情報を取得
                User dbUser = userRepo.findById(user.getId()).orElse(null);
                
                // DBに記録されているセッションIDが、今切れたセッションIDと同じなら、ロックを解除（null）にする
                if (dbUser != null && se.getSession().getId().equals(dbUser.getCurrentSessionId())) {
                    dbUser.setCurrentSessionId(null);
                    userRepo.save(dbUser);
                    System.out.println("【自動ロック解除】タイムアウトによりセッションを解放しました: " + user.getUserId());
                }
            } catch (Exception e) {
                System.out.println("セッション解放中にエラーが発生しました。");
            }
        }
    }
}