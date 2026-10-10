package com.dataquadinc.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;
import java.time.LocalDate;

public class PayslipGenerateRequest {

    @NotBlank
    private String userId;

    /** yyyy-MM */
    @NotBlank
    @Pattern(regexp = "^\\d{4}-(0[1-9]|1[0-2])$", message = "payPeriod must be yyyy-MM")
    private String payPeriod;

    @NotNull
    @Min(1)
    @Max(31)
    private Integer payableDays;

    @NotNull
    private LocalDate paymentDate;

    @NotNull
    @DecimalMin(value = "0.01", message = "grossConsultancyFee must be positive")
    private BigDecimal grossConsultancyFee;

    private String createdBy;

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getPayPeriod() {
        return payPeriod;
    }

    public void setPayPeriod(String payPeriod) {
        this.payPeriod = payPeriod;
    }

    public Integer getPayableDays() {
        return payableDays;
    }

    public void setPayableDays(Integer payableDays) {
        this.payableDays = payableDays;
    }

    public LocalDate getPaymentDate() {
        return paymentDate;
    }

    public void setPaymentDate(LocalDate paymentDate) {
        this.paymentDate = paymentDate;
    }

    public BigDecimal getGrossConsultancyFee() {
        return grossConsultancyFee;
    }

    public void setGrossConsultancyFee(BigDecimal grossConsultancyFee) {
        this.grossConsultancyFee = grossConsultancyFee;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }
}
