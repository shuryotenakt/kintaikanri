package com.hackathon.kintai.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name = "shifts")
public class Shift {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private String userId;     // 従業員ID

    @Column(name = "year_month", nullable = false)
    private String yearMonth;   // 対象月 (例: "2026-05")

    @Column(name = "day", nullable = false)
    private int day;           // 日付 (1〜31)

    @Column(name = "shift_in")
    private String shiftIn;    // 入り時間 (例: "09:00")

    @Column(name = "shift_out")
    private String shiftOut;   // 出時間 (例: "18:00")
}