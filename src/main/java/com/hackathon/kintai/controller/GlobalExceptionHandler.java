package com.hackathon.kintai.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.NoHandlerFoundException;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // 🌟 追加：404エラー（ページが見つからない）専用のキャッチ網
    @ExceptionHandler(NoHandlerFoundException.class)
    public String handle404(NoHandlerFoundException ex) {
        // エラーログは出さず、単に404画面へ案内する
        return "error/404";
    }

    // 今までの：上記以外の「すべてのシステムエラー（バグ）」のキャッチ網
    @ExceptionHandler(Exception.class)
    public String handleAllExceptions(Exception ex, Model model) {
        logger.error("重大なエラーが発生しました: ", ex);
        model.addAttribute("errorMessage", "サーバー内部でエラーが発生しました。操作をやり直してください。");
        return "error/500";
    }
}