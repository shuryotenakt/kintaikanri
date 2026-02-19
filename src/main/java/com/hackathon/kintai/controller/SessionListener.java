package com.hackathon.kintai.controller;

import com.hackathon.kintai.model.User;
import jakarta.servlet.http.HttpSessionEvent;
import jakarta.servlet.http.HttpSessionListener;
import org.springframework.stereotype.Component;

@Component
public class SessionListener implements HttpSessionListener {

    @Override
    public void sessionDestroyed(HttpSessionEvent se) {
        // セッションが切れた（ブラウザを閉じて一定時間経過、またはタイムアウト）時に自動で呼ばれる
        User user = (User) se.getSession().getAttribute("user");
        if (user != null) {
            // サーバー側のログイン中リストから削除して、他の人が入れるようにする
            LoginController.loginUserMap.remove(user.getUserId());
        }
    }
}
