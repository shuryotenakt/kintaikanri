package com.hackathon.kintai.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.hackathon.kintai.model.Attendance;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AttendanceRepository extends JpaRepository<Attendance, Long> {
    
    // -------------------------------------------------------------------------
    // 1. 従業員画面（Partner）用
    // -------------------------------------------------------------------------

    // 特定のユーザーの履歴（全件）
    List<Attendance> findAllByUserIdOrderByStartTimeDesc(String userId);

    // 特定のユーザーの最新の1件（ステータス表示の補助用）
    Attendance findTopByUserIdOrderByStartTimeDesc(String userId);

    /**
     * 【重要】二重打刻防止・状態管理用
     * 「退勤していない（endTimeがNull）」かつ「最新の」勤務情報を取得します。
     * これが取得できれば、そのユーザーは現在「勤務中」または「休憩中」であると判断できます。
     */
    Attendance findTopByUserIdAndEndTimeIsNullOrderByStartTimeDesc(String userId);


    // -------------------------------------------------------------------------
    // 2. 管理者画面（Admin）用
    // -------------------------------------------------------------------------

    // 全員分の履歴
    List<Attendance> findAllByOrderByStartTimeDesc();

    // 全員分の履歴（期間指定フィルタ）
    List<Attendance> findAllByStartTimeBetweenOrderByStartTimeDesc(LocalDateTime start, LocalDateTime end);

    // 特定ユーザーの履歴（期間指定フィルタ）
    List<Attendance> findAllByUserIdAndStartTimeBetweenOrderByStartTimeDesc(String userId, LocalDateTime start, LocalDateTime end);
}