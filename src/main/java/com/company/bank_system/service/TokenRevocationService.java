package com.company.bank_system.service;


import com.company.bank_system.entity.RevokedToken;
import com.company.bank_system.repo.RevokedTokenRepository;
import org.springframework.stereotype.Service;

@Service
public class TokenRevocationService {

    private final RevokedTokenRepository revokedTokenRepository;

    public TokenRevocationService(RevokedTokenRepository revokedTokenRepository) {
        this.revokedTokenRepository = revokedTokenRepository;
    }

    public void revoke(String token) {
        revokedTokenRepository.save(new RevokedToken(token));
    }

    public boolean isRevoked(String token) {
        return revokedTokenRepository.existsByToken(token);
    }
}