package com.hackathon.kintai.repository;

import com.hackathon.kintai.model.Shift;

import jakarta.transaction.Transactional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ShiftRepository extends JpaRepository<Shift, Long> {
    // 指定した月の全員のシフトデータをSupabaseからごっそり取得する
    List<Shift> findAllByYearMonth(String yearMonth);

    // すでに同じ日のデータがあるかチェックする用
    Optional<Shift> findByUserIdAndYearMonthAndDay(String userId, String yearMonth, int day);

    @Modifying
    @Transactional
    @Query("DELETE FROM Shift s WHERE s.userId = :userId")
    void deleteByUserId(String userId);
}