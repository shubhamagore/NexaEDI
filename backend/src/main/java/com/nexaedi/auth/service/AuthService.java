package com.nexaedi.auth.service;

import com.nexaedi.auth.model.User;
import com.nexaedi.auth.repository.UserRepository;
import com.nexaedi.portal.model.Seller;
import com.nexaedi.portal.model.SellerPlan;
import com.nexaedi.portal.repository.SellerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final SellerRepository sellerRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    /**
     * Registers a new seller and creates their user account + seller tenant record.
     */
    @Transactional
    public Map<String, Object> register(String email, String password, String fullName, String companyName) {
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("An account with this email already exists.");
        }

        Seller seller = Seller.builder()
                .name(fullName)
                .email(email)
                .companyName(companyName)
                .plan(SellerPlan.STARTER)
                .build();
        sellerRepository.save(seller);

        User user = User.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(password))
                .fullName(fullName)
                .companyName(companyName)
                .seller(seller)
                .build();
        userRepository.save(user);

        String token = jwtService.generateToken(email, seller.getId());
        log.info("[AUTH] New seller registered: {} (sellerId={})", email, seller.getId());

        return buildResponse(user, seller, token);
    }

    /**
     * Authenticates an existing seller and returns a JWT.
     * Verifies password directly — avoids AuthenticationManager circular dependency issues.
     */
    public Map<String, Object> login(String email, String password) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid credentials");
        }

        String token = jwtService.generateToken(email, user.getSeller().getId());
        log.info("[AUTH] Seller logged in: {} (sellerId={})", email, user.getSeller().getId());

        return buildResponse(user, user.getSeller(), token);
    }

    private Map<String, Object> buildResponse(User user, Seller seller, String token) {
        return Map.of(
                "token",    token,
                "sellerId", seller.getId(),
                "email",    user.getEmail(),
                "name",     user.getFullName(),
                "company",  user.getCompanyName() != null ? user.getCompanyName() : "",
                "plan",     seller.getPlan().name()
        );
    }
}
