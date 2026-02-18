package com.hackathon.kintai;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import com.hackathon.kintai.model.User;
import com.hackathon.kintai.repository.UserRepository;

@SpringBootApplication
public class KintaiApplication {

    public static void main(String[] args) {
        SpringApplication.run(KintaiApplication.class, args);
    }

    @Bean
    CommandLineRunner init(UserRepository repo) {
        return args -> {
            // ユーザーが一人もいない時だけ、初期管理者を作成する
            if (repo.count() == 0) {
                User admin = new User();
                admin.setUserId("admin");
                admin.setName("管理者");
                admin.setPassword("admin123");
                admin.setRole("ADMIN");
                repo.save(admin);
                System.out.println("★初期管理者を登録しました (ID: admin / Pass: admin123)");
            }
        };
    }
}