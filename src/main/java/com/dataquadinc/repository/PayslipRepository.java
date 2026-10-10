package com.dataquadinc.repository;

import com.dataquadinc.model.Payslip;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PayslipRepository extends JpaRepository<Payslip, Long> {

    List<Payslip> findByUserIdOrderByPayPeriodDesc(String userId);

    Optional<Payslip> findByUserIdAndPayPeriod(String userId, String payPeriod);
}
