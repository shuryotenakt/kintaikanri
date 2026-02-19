package com.hackathon.kintai.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "users") // テーブル名も users に変わるぞ
@Data
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String userId; // 自動で割り振るユーザー番号（例：1001）

    private String name;     // 名前
    private String password; // パスワード
    private String role;     // "ADMIN"（管理者）か "PARTNER"（パートナー）
    private String currentSessionId;    
}