package com.bankingsystem.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.bankingsystem.entity.Company;

public interface CompanyRepository extends JpaRepository<Company, Long> {

    boolean existsByInn(String inn);
}
