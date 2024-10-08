package com.boot3.myrestapi.security.jwt;

import com.boot3.myrestapi.security.userinfos.RefreshToken;
import com.boot3.myrestapi.security.userinfos.UserInfo;
import com.boot3.myrestapi.security.userinfos.UserInfoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class RefreshTokenService {

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;
    @Autowired
    private UserInfoRepository userInfoRepository;

    public RefreshToken createRefreshToken(String username) {
        UserInfo userInfo = userInfoRepository.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("user not found " + username));

        //Refresh_token 이 있으면
        return refreshTokenRepository.findByUserInfo(userInfo)
                //Refresh_token 이 없으면
                .orElseGet(() -> {
                    RefreshToken refreshToken = RefreshToken.builder()
                            .userInfo(userInfo)
                            .token(UUID.randomUUID().toString())
                            .expiryDate(Instant.now().plusMillis(600000))//10분 - 600000, 1분 - 60000
                            .build();
                    return refreshTokenRepository.save(refreshToken);
                });
    }


    public Optional<RefreshToken> findByToken(String token) {
        return refreshTokenRepository.findByToken(token);
    }


    public RefreshToken verifyExpiration(RefreshToken token) {
        if (token.getExpiryDate().compareTo(Instant.now()) < 0) {
            refreshTokenRepository.delete(token);
            throw new RuntimeException(token.getToken() + " Refresh token was expired. Please make a new signin request");
        }
        return token;
    }

}