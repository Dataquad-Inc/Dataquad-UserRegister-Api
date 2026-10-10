package com.dataquadinc.controller;

import com.dataquadinc.dto.ApiResponse;
import com.dataquadinc.dto.PayslipGenerateRequest;
import com.dataquadinc.dto.PayslipResponseDto;
import com.dataquadinc.exceptions.ErrorDto;
import com.dataquadinc.service.PayslipService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/users/payslips")
public class PayslipController {

    private static final Logger logger = LoggerFactory.getLogger(PayslipController.class);

    private final PayslipService payslipService;

    public PayslipController(PayslipService payslipService) {
        this.payslipService = payslipService;
    }

    @PostMapping("/generate")
    public ResponseEntity<ApiResponse<PayslipResponseDto>> generate(@Valid @RequestBody PayslipGenerateRequest request) {
        try {
            PayslipResponseDto dto = payslipService.generate(request);
            return ResponseEntity.ok(ApiResponse.success("Payment advice generated successfully", dto));
        } catch (Exception e) {
            logger.error("Failed to generate payslip for {}: {}", request.getUserId(), e.getMessage(), e);
            ErrorDto error = new ErrorDto(String.valueOf(HttpStatus.BAD_REQUEST.value()), e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, "Failed to generate payment advice", null, error));
        }
    }

    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse<List<PayslipResponseDto>>> list(@PathVariable String userId) {
        try {
            List<PayslipResponseDto> list = payslipService.listByUser(userId);
            return ResponseEntity.ok(ApiResponse.success("Payslips fetched", list));
        } catch (Exception e) {
            logger.error("Failed to list payslips for {}: {}", userId, e.getMessage(), e);
            ErrorDto error = new ErrorDto(String.valueOf(HttpStatus.NOT_FOUND.value()), e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse<>(false, "Failed to fetch payslips", null, error));
        }
    }

    @GetMapping("/{userId}/{payPeriod}/pdf")
    public ResponseEntity<byte[]> downloadPdf(@PathVariable String userId, @PathVariable String payPeriod) {
        try {
            byte[] pdf = payslipService.getPdf(userId, payPeriod);
            String filename = "PaymentAdvice_" + userId + "_" + payPeriod + ".pdf";
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdf);
        } catch (Exception e) {
            logger.error("Failed to download payslip PDF for {} {}: {}", userId, payPeriod, e.getMessage(), e);
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/preview-calc")
    public ResponseEntity<ApiResponse<PayslipResponseDto>> preview(
            @RequestParam java.math.BigDecimal grossConsultancyFee) {
        java.math.BigDecimal gross = grossConsultancyFee.setScale(2, java.math.RoundingMode.HALF_UP);
        java.math.BigDecimal tds = gross.multiply(PayslipService.TDS_RATE)
                .setScale(0, java.math.RoundingMode.HALF_UP)
                .setScale(2, java.math.RoundingMode.HALF_UP);
        java.math.BigDecimal net = gross.subtract(tds);
        PayslipResponseDto dto = new PayslipResponseDto();
        dto.setGrossConsultancyFee(gross);
        dto.setTdsRate(PayslipService.TDS_RATE);
        dto.setTdsAmount(tds);
        dto.setNetPayable(net);
        dto.setAmountInWords(com.dataquadinc.service.IndianCurrencyWords.toWords(net));
        return ResponseEntity.ok(ApiResponse.success("Preview", dto));
    }
}
