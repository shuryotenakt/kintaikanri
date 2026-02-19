package com.hackathon.kintai.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.hackathon.kintai.model.User;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    // ユーザーIDとパスワードで検索（ログイン用）
    Optional<User> findByUserIdAndPassword(String userId, String password);
    
    // 名前とパスワードで検索（名前でもログインできるように！）
    Optional<User> findByNameAndPassword(String name, String password);
    Optional<User> findByUserIdAndName(String userId, String name);
    Optional<User> findByUserId(String userId); // これを追加
    Optional<User> findByName(String name);
    // ユーザーが何人いるか数える（自動ID割り振りに使用）
    long count();
}