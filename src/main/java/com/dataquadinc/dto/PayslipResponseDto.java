package com.dataquadinc.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class PayslipResponseDto {

    private Long id;
    private String userId;
    private String consultantName;
    private String payPeriod;
    private String payPeriodLabel;
    private Integer payableDays;
    private LocalDate paymentDate;
    private BigDecimal grossConsultancyFee;
    private BigDecimal tdsRate;
    private BigDecimal tdsAmount;
    private BigDecimal netPayable;
    private String amountInWords;
    private String createdBy;
    private LocalDateTime createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getConsultantName() {
        return consultantName;
    }

    public void setConsultantName(String consultantName) {
        this.consultantName = consultantName;
    }

    public String getPayPeriod() {
        return payPeriod;
    }

    public void setPayPeriod(String payPeriod) {
        this.payPeriod = payPeriod;
    }

    public String getPayPeriodLabel() {
        return payPeriodLabel;
    }

    public void setPayPeriodLabel(String payPeriodLabel) {
        this.payPeriodLabel = payPeriodLabel;
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

    public BigDecimal getTdsRate() {
        return tdsRate;
    }

    public void setTdsRate(BigDecimal tdsRate) {
        this.tdsRate = tdsRate;
    }

    public BigDecimal getTdsAmount() {
        return tdsAmount;
    }

    public void setTdsAmount(BigDecimal tdsAmount) {
        this.tdsAmount = tdsAmount;
    }

    public BigDecimal getNetPayable() {
        return netPayable;
    }

    public void setNetPayable(BigDecimal netPayable) {
        this.netPayable = netPayable;
    }

    public String getAmountInWords() {
        return amountInWords;
    }

    public void setAmountInWords(String amountInWords) {
        this.amountInWords = amountInWords;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
