package com.bankingsystem.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import com.bankingsystem.entity.Account;
import com.bankingsystem.entity.Card;
import com.bankingsystem.entity.Loan;
import com.bankingsystem.entity.OwnerType;
import com.bankingsystem.entity.Role;
import com.bankingsystem.entity.User;
import com.bankingsystem.exception.ForbiddenException;
import com.bankingsystem.exception.NotFoundException;
import com.bankingsystem.repository.UserRepository;

@Service
@RequiredArgsConstructor
public class AccessControlService {

    private final UserRepository userRepository;

    public void requireAdmin() {
        if (getCurrentUser().getRole() != Role.ADMIN) {
            throw new ForbiddenException("Faqat administratorga ruxsat berilgan");
        }
    }

    public void requireUserAccess(Long userId) {
        User currentUser = getCurrentUser();
        if (currentUser.getRole() == Role.ADMIN) {
            return;
        }

        if (!currentUser.getId().equals(userId)) {
            throw new ForbiddenException("Boshqa foydalanuvchi ma'lumotiga kirish mumkin emas");
        }
    }

    public void requireAccountAccess(Account account) {
        User currentUser = getCurrentUser();
        if (currentUser.getRole() == Role.ADMIN) {
            return;
        }

        if (account.getOwnerType() != OwnerType.USER || !currentUser.getId().equals(account.getOwnerId())) {
            throw new ForbiddenException("Hisobga kirish ruxsati yo'q");
        }
    }

    public void requireCardAccess(Card card) {
        requireAccountAccess(card.getAccount());
    }

    public void requireLoanAccess(Loan loan) {
        User currentUser = getCurrentUser();
        if (currentUser.getRole() == Role.ADMIN) {
            return;
        }

        if (loan.getUser() == null || !currentUser.getId().equals(loan.getUser().getId())) {
            throw new ForbiddenException("Kreditga kirish ruxsati yo'q");
        }
    }

    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            throw new ForbiddenException("Autentifikatsiya talab qilinadi");
        }

        String username = extractUsername(authentication);

        return userRepository.findByPhone(username)
                .orElseThrow(() -> new NotFoundException("Foydalanuvchi topilmadi"));
    }

    private String extractUsername(Authentication authentication) {
        Object principal = authentication.getPrincipal();

        if (principal instanceof User user) {
            return user.getUsername();
        }

        if (principal instanceof UserDetails userDetails) {
            return userDetails.getUsername();
        }

        return authentication.getName();
    }
}
