package com.hackathon.kintai.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "shift_requests")
public class ShiftRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private String userId; // 従業員ID

    @Column(name = "target_date", nullable = false)
    private LocalDate targetDate; // 対象日 (2026-06-01 など)

    @Column(name = "start_time")
    private String startTime; // 出勤希望時間 (10:00)

    @Column(name = "end_time")
    private String endTime; // 退勤希望時間 (19:00)

    @Column(name = "status", nullable = false)
    private String status = "REQUEST"; // REQUEST(申請中) / APPROVED(確定)

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public LocalDate getTargetDate() { return targetDate; }
    public void setTargetDate(LocalDate targetDate) { this.targetDate = targetDate; }
    public String getStartTime() { return startTime; }
    public void setStartTime(String startTime) { this.startTime = startTime; }
    public String getEndTime() { return endTime; }
    public void setEndTime(String endTime) { this.endTime = endTime; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
