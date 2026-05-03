package com.bankingsystem.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.bankingsystem.dto.CompanyCreateRequest;
import com.bankingsystem.dto.CompanyMemberCreateRequest;
import com.bankingsystem.dto.CompanyMemberResponse;
import com.bankingsystem.dto.CompanyResponse;
import com.bankingsystem.dto.CompanyUpdateRequest;
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

import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class CompanyService {

    private final CompanyRepository companyRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AccessControlService accessControlService;

    @Transactional
    public CompanyResponse createCompany(CompanyCreateRequest request) {
        accessControlService.requireAdmin();

        String inn = normalizeInn(request.getInn());
        if (companyRepository.existsByInn(inn)) {
            throw new AlreadyExistsException("INN already registered");
        }

        Company company = Company.builder()
                .name(request.getName().trim())
                .inn(inn)
                .director(request.getDirector().trim())
                .build();

        companyRepository.save(company);
        return toCompanyResponse(company);
    }

    public List<CompanyResponse> getCompanies() {
        accessControlService.requireAdmin();
        return companyRepository.findAll()
                .stream()
                .map(this::toCompanyResponse)
                .toList();
    }

    public CompanyResponse getCompany(Long companyId) {
        accessControlService.requireCompanyAccess(companyId);
        return toCompanyResponse(getExistingCompany(companyId));
    }

    @Transactional
    public CompanyResponse updateCompany(Long companyId, CompanyUpdateRequest request) {
        accessControlService.requireAdmin();

        Company company = getExistingCompany(companyId);

        if (request.getName() != null && !request.getName().isBlank()) {
            company.setName(request.getName().trim());
        }

        if (request.getDirector() != null && !request.getDirector().isBlank()) {
            company.setDirector(request.getDirector().trim());
        }

        companyRepository.save(company);
        return toCompanyResponse(company);
    }

    public List<CompanyMemberResponse> getMembers(Long companyId) {
        accessControlService.requireCompanyAccess(companyId);
        getExistingCompany(companyId);

        return userRepository.findByCompanyIdOrderByIdAsc(companyId)
                .stream()
                .map(this::toMemberResponse)
                .toList();
    }

    @Transactional
    public CompanyMemberResponse createMember(Long companyId, CompanyMemberCreateRequest request) {
        accessControlService.requireAdmin();

        Company company = getExistingCompany(companyId);
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
                .type(UserType.BUSINESS)
                .role(Role.USER)
                .company(company)
                .build();

        userRepository.save(user);
        return toMemberResponse(user);
    }

    private Company getExistingCompany(Long companyId) {
        return companyRepository.findById(companyId)
                .orElseThrow(() -> new NotFoundException("Kompaniya topilmadi"));
    }

    private CompanyResponse toCompanyResponse(Company company) {
        return CompanyResponse.builder()
                .id(company.getId())
                .name(company.getName())
                .inn(company.getInn())
                .director(company.getDirector())
                .memberCount((long) userRepository.findByCompanyIdOrderByIdAsc(company.getId()).size())
                .build();
    }

    private CompanyMemberResponse toMemberResponse(User user) {
        if (user.getCompany() == null) {
            throw new BadRequestException("Foydalanuvchi kompaniyaga bog'lanmagan");
        }

        return CompanyMemberResponse.builder()
                .id(user.getId())
                .companyId(user.getCompany().getId())
                .fullName(user.getFullName())
                .phone(user.getPhone())
                .status(user.getStatus().name())
                .userType(user.getType().name())
                .role(user.getRole().name())
                .build();
    }

    private String normalizeInn(String inn) {
        String normalizedInn = inn.trim();
        if (!normalizedInn.matches("\\d{9}")) {
            throw new BadRequestException("INN 9 xonali bo'lishi kerak");
        }
        return normalizedInn;
    }

    private String normalizePhone(String phone) {
        return phone.trim();
    }
}
