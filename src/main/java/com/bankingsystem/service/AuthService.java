package com.bankingsystem.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.bankingsystem.dto.AuthResponse;
import com.bankingsystem.dto.BusinessRegisterRequest;
import com.bankingsystem.dto.LoginRequest;
import com.bankingsystem.dto.RefreshRequest;
import com.bankingsystem.dto.RegisterRequest;
import com.bankingsystem.entity.Company;
import com.bankingsystem.entity.Role;
import com.bankingsystem.entity.User;
import com.bankingsystem.entity.UserStatus;
import com.bankingsystem.entity.UserType;
import com.bankingsystem.exception.AlreadyExistsException;
import com.bankingsystem.exception.BadRequestException;
import com.bankingsystem.exception.NotFoundException;
import com.bankingsystem.repository.CompanyRepository;
import com.bankingsystem.repository.UserRepository;
import com.bankingsystem.security.JwtService;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
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
                .company(null)
                .build();

        userRepository.save(user);
        return buildAuthResponse(user);
    }

    @Transactional
    public AuthResponse registerBusiness(BusinessRegisterRequest request) {
        String phone = normalizePhone(request.getPhone());
        String inn = normalizeInn(request.getCompanyInn());

        if (userRepository.existsByPhone(phone)) {
            throw new AlreadyExistsException("Phone number already registered");
        }

        if (companyRepository.existsByInn(inn)) {
            throw new AlreadyExistsException("INN already registered");
        }

        Company company = Company.builder()
                .name(request.getCompanyName().trim())
                .inn(inn)
                .director(request.getDirector().trim())
                .build();
        companyRepository.save(company);

        User user = User.builder()
                .fullName(request.getFullName().trim())
                .phone(phone)
                .password(passwordEncoder.encode(request.getPassword()))
                .passport(request.getPassport().trim().toUpperCase(Locale.ROOT))
                .pinfl(request.getPinfl().trim())
                .status(UserStatus.ACTIVE)
                .type(UserType.BUSINESS)
                .role(Role.USER)
                .company(company)
                .build();

        userRepository.save(user);
        return buildAuthResponse(user);
    }

    public AuthResponse login(LoginRequest request) {
        String phone = normalizePhone(request.getPhone());

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(phone, request.getPassword())
        );

        User user = userRepository.findByPhone(phone)
                .orElseThrow(() -> new NotFoundException("Foydalanuvchi topilmadi"));

        return buildAuthResponse(user);
    }

    public AuthResponse refresh(RefreshRequest request) {
        String refreshToken = request.getRefreshToken().trim();
        String username = jwtService.extractUsername(refreshToken);

        User user = userRepository.findByPhone(username)
                .orElseThrow(() -> new NotFoundException("Foydalanuvchi topilmadi"));

        if (!jwtService.isTokenValid(refreshToken, user) || !jwtService.isRefreshToken(refreshToken)) {
            throw new BadRequestException("Refresh token yaroqsiz");
        }

        return buildAuthResponse(user);
    }

    private AuthResponse buildAuthResponse(User user) {
        return AuthResponse.builder()
                .accessToken(jwtService.generateAccessToken(user))
                .refreshToken(jwtService.generateRefreshToken(user))
                .userId(user.getId())
                .role(user.getRole().name())
                .userType(user.getType().name())
                .companyId(user.getCompany() != null ? user.getCompany().getId() : null)
                .build();
    }

    private String normalizePhone(String phone) {
        return phone.trim();
    }

    private String normalizeInn(String inn) {
        String normalizedInn = inn.trim();
        if (!normalizedInn.matches("\\d{9}")) {
            throw new BadRequestException("INN 9 xonali bo'lishi kerak");
        }
        return normalizedInn;
    }
}
