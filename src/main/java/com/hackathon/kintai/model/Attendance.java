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

    // 分単位での労働時間を返す
    public String getWorkTime() {
        if (startTime == null || endTime == null) return "-";
        long minutes = Duration.between(startTime, endTime).toMinutes();
        
        if (minutes < 0) return "エラー(時間が逆転)";
        
        // ◯分 という表示にする（もし1時間以上なら ◯時間◯分）
        if (minutes < 60) {
            return minutes + "分";
        } else {
            return (minutes / 60) + "時間" + (minutes % 60) + "分";
        }
    }
}