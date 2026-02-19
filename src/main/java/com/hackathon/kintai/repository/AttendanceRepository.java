package com.hackathon.kintai.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.hackathon.kintai.model.Attendance;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AttendanceRepository extends JpaRepository<Attendance, Long> {
    
    // 特定のユーザーの履歴
    List<Attendance> findAllByUserIdOrderByStartTimeDesc(String userId);

    // 特定のユーザーの最新の1件
    Attendance findTopByUserIdOrderByStartTimeDesc(String userId);

    // 全員分の履歴
    List<Attendance> findAllByOrderByStartTimeDesc();

    // 🆕 全員分の履歴（期間指定）
    List<Attendance> findAllByStartTimeBetweenOrderByStartTimeDesc(LocalDateTime start, LocalDateTime end);

    // 🆕 特定ユーザーの履歴（期間指定）
    List<Attendance> findAllByUserIdAndStartTimeBetweenOrderByStartTimeDesc(String userId, LocalDateTime start, LocalDateTime end);
}