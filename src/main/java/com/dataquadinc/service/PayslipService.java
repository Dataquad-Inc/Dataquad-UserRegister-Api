package com.dataquadinc.service;

import com.dataquadinc.dto.PayslipGenerateRequest;
import com.dataquadinc.dto.PayslipResponseDto;
import com.dataquadinc.exceptions.UserNotFoundException;
import com.dataquadinc.model.Payslip;
import com.dataquadinc.model.UserDetails;
import com.dataquadinc.repository.PayslipRepository;
import com.dataquadinc.repository.UserDao;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PayslipService {

    public static final BigDecimal TDS_RATE = new BigDecimal("0.02");

    private final PayslipRepository payslipRepository;
    private final UserDao userDao;
    private final PayslipPdfService payslipPdfService;

    public PayslipService(PayslipRepository payslipRepository, UserDao userDao, PayslipPdfService payslipPdfService) {
        this.payslipRepository = payslipRepository;
        this.userDao = userDao;
        this.payslipPdfService = payslipPdfService;
    }

    @Transactional
    public PayslipResponseDto generate(PayslipGenerateRequest request) {
        UserDetails user = userDao.findByUserId(request.getUserId());
        if (user == null) {
            throw new UserNotFoundException("User not found: " + request.getUserId());
        }

        BigDecimal gross = request.getGrossConsultancyFee().setScale(2, RoundingMode.HALF_UP);
        // Match sample payment advice: TDS rounded to nearest rupee (57143 * 2% → 1143.00)
        BigDecimal tds = gross.multiply(TDS_RATE).setScale(0, RoundingMode.HALF_UP).setScale(2, RoundingMode.HALF_UP);
        BigDecimal net = gross.subtract(tds).setScale(2, RoundingMode.HALF_UP);
        String words = IndianCurrencyWords.toWords(net);

        Payslip payslip = payslipRepository
                .findByUserIdAndPayPeriod(request.getUserId(), request.getPayPeriod())
                .orElseGet(Payslip::new);

        payslip.setUserId(request.getUserId());
        payslip.setPayPeriod(request.getPayPeriod());
        payslip.setPayableDays(request.getPayableDays());
        payslip.setPaymentDate(request.getPaymentDate());
        payslip.setGrossConsultancyFee(gross);
        payslip.setTdsRate(TDS_RATE);
        payslip.setTdsAmount(tds);
        payslip.setNetPayable(net);
        payslip.setAmountInWords(words);
        payslip.setCreatedBy(request.getCreatedBy());

        try {
            payslip.setPdfBytes(payslipPdfService.generate(user, payslip));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to generate payslip PDF: " + e.getMessage(), e);
        }

        Payslip saved = payslipRepository.save(payslip);
        return toDto(saved, user.getUserName());
    }

    @Transactional(readOnly = true)
    public List<PayslipResponseDto> listByUser(String userId) {
        UserDetails user = userDao.findByUserId(userId);
        if (user == null) {
            throw new UserNotFoundException("User not found: " + userId);
        }
        return payslipRepository.findByUserIdOrderByPayPeriodDesc(userId).stream()
                .map(p -> toDto(p, user.getUserName()))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public byte[] getPdf(String userId, String payPeriod) {
        Payslip payslip = payslipRepository.findByUserIdAndPayPeriod(userId, payPeriod)
                .orElseThrow(() -> new UserNotFoundException(
                        "Payslip not found for user " + userId + " period " + payPeriod));
        if (payslip.getPdfBytes() == null || payslip.getPdfBytes().length == 0) {
            throw new IllegalStateException("Payslip PDF is missing; regenerate the payslip.");
        }
        return payslip.getPdfBytes();
    }

    private PayslipResponseDto toDto(Payslip p, String name) {
        PayslipResponseDto dto = new PayslipResponseDto();
        dto.setId(p.getId());
        dto.setUserId(p.getUserId());
        dto.setConsultantName(name);
        dto.setPayPeriod(p.getPayPeriod());
        dto.setPayPeriodLabel(PayslipPdfService.formatPayPeriod(p.getPayPeriod()));
        dto.setPayableDays(p.getPayableDays());
        dto.setPaymentDate(p.getPaymentDate());
        dto.setGrossConsultancyFee(p.getGrossConsultancyFee());
        dto.setTdsRate(p.getTdsRate());
        dto.setTdsAmount(p.getTdsAmount());
        dto.setNetPayable(p.getNetPayable());
        dto.setAmountInWords(p.getAmountInWords());
        dto.setCreatedBy(p.getCreatedBy());
        dto.setCreatedAt(p.getCreatedAt());
        return dto;
    }
}
