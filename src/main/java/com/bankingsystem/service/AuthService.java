package com.bankingsystem.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import com.bankingsystem.dto.AuthResponse;
import com.bankingsystem.dto.LoginRequest;
import com.bankingsystem.dto.RefreshRequest;
import com.bankingsystem.dto.RegisterRequest;
import com.bankingsystem.entity.Role;
import com.bankingsystem.entity.User;
import com.bankingsystem.entity.UserStatus;
import com.bankingsystem.entity.UserType;
import com.bankingsystem.exception.AlreadyExistsException;
import com.bankingsystem.exception.BadRequestException;
import com.bankingsystem.exception.NotFoundException;
import com.bankingsystem.repository.UserRepository;
import com.bankingsystem.security.JwtService;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public AuthResponse register(RegisterRequest request) {
        String phone = normalizePhone(request.getPhone());

        if (userRepository.existsByPhone(phone)) {
            throw new AlreadyExistsException("Phone number already registered");
        }

        User user = User.builder()
                .fullName(request.getFullName().trim())
                .phone(phone)
                .password(passwordEncoder.encode(request.getPassword()))
                .passport(request.getPassport().trim().toUpperCase(Locale.ROOT))
                .pinfl(request.getPinfl().trim())
                .status(UserStatus.ACTIVE)
                .type(UserType.INDIVIDUAL)
                .role(Role.USER)
                .build();

        userRepository.save(user);

        return AuthResponse.builder()
                .accessToken(jwtService.generateAccessToken(user))
                .refreshToken(jwtService.generateRefreshToken(user))
                .build();
    }

    public AuthResponse login(LoginRequest request) {
        String phone = normalizePhone(request.getPhone());

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(phone, request.getPassword())
        );

        User user = userRepository.findByPhone(phone)
                .orElseThrow(() -> new NotFoundException("Foydalanuvchi topilmadi"));

        return AuthResponse.builder()
                .accessToken(jwtService.generateAccessToken(user))
                .refreshToken(jwtService.generateRefreshToken(user))
                .build();
    }

    public AuthResponse refresh(RefreshRequest request) {
        String refreshToken = request.getRefreshToken().trim();
        String username = jwtService.extractUsername(refreshToken);

        User user = userRepository.findByPhone(username)
                .orElseThrow(() -> new NotFoundException("Foydalanuvchi topilmadi"));

        if (!jwtService.isTokenValid(refreshToken, user) || !jwtService.isRefreshToken(refreshToken)) {
            throw new BadRequestException("Refresh token yaroqsiz");
        }

        return AuthResponse.builder()
                .accessToken(jwtService.generateAccessToken(user))
                .refreshToken(jwtService.generateRefreshToken(user))
                .build();
    }

    private String normalizePhone(String phone) {
        return phone.trim();
    }
}


