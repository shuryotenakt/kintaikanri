package com.hackathon.kintai.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.hackathon.kintai.model.Attendance;
import java.util.List;

@Repository
public interface AttendanceRepository extends JpaRepository<Attendance, Long> {
    
    // 特定のユーザーの履歴（新しい順）
    List<Attendance> findAllByUserIdOrderByStartTimeDesc(String userId);

    // 特定のユーザーの最新の1件（打刻判定用）
    Attendance findTopByUserIdOrderByStartTimeDesc(String userId);

    // 【追加】全員分の履歴（新しい順）
    List<Attendance> findAllByOrderByStartTimeDesc();
}