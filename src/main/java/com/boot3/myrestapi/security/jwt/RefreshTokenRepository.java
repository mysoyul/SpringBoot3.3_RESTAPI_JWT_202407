package com.boot3.myrestapi.security.jwt;

import com.boot3.myrestapi.security.userinfos.RefreshToken;
import com.boot3.myrestapi.security.userinfos.UserInfo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken,Integer> {
    Optional<RefreshToken> findByToken(String token);
    Optional<RefreshToken> findByUserInfo(UserInfo userInfo);
}