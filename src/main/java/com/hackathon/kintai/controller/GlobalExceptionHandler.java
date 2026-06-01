package com.hackathon.kintai.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler {

    // ターミナルにエラーのログを出すための準備
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * システム内のすべてのException（例外）をここでキャッチします
     */
    @ExceptionHandler(Exception.class)
    public String handleAllExceptions(Exception ex, Model model) {
        
        // 1. エラー内容をコンソール（ターミナル）に出力（デバッグ用）
        logger.error("重大なエラーが発生しました: ", ex);

        // 2. 画面（error.html）に渡すメッセージをセット
        model.addAttribute("errorMessage", "サーバー内部でエラーが発生しました。操作をやり直してください。");

        // 3. ここを "error/error" から "error/500" に変更！
        return "error/500";
    }
}