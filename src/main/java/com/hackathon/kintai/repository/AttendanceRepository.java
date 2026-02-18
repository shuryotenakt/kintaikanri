package com.hackathon.kintai.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.hackathon.kintai.model.Attendance;
import java.util.List;

@Repository
public interface AttendanceRepository extends JpaRepository<Attendance, Long> {
    
    // 特定のユーザーの履歴を、新しい順に全部持ってくる（パートナー画面用）
    List<Attendance> findAllByUserIdOrderByStartTimeDesc(String userId);

    // 特定のユーザーの、一番新しい記録を1件だけ持ってくる（退勤打刻の紐付け用）
    Attendance findTopByUserIdOrderByStartTimeDesc(String userId);
}