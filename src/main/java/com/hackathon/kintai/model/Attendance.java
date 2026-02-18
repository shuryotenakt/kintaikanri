package com.hackathon.kintai.model;

import java.time.LocalDateTime;
import java.time.Duration;
import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "attendances")
@Data
public class Attendance {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String userId;
    private String userName;
    private LocalDateTime startTime;
    private LocalDateTime endTime;

    // 🆕 休憩用の時間を追加
    private LocalDateTime breakStartTime;
    private LocalDateTime breakEndTime;

    // 労働時間計算（※簡易的に休憩時間を引くロジックはまだ入れていませんが、まずは表示用）
    public String getWorkTime() {
        if (startTime == null || endTime == null) return "-";
        long minutes = Duration.between(startTime, endTime).toMinutes();
        
        // もし休憩していたら、その分を引く（簡易実装）
        if (breakStartTime != null && breakEndTime != null) {
            long breakMinutes = Duration.between(breakStartTime, breakEndTime).toMinutes();
            minutes = minutes - breakMinutes;
        }

        if (minutes < 0) return "エラー";

        if (minutes < 60) {
            return minutes + "分";
        } else {
            return (minutes / 60) + "時間" + (minutes % 60) + "分";
        }
    }
}