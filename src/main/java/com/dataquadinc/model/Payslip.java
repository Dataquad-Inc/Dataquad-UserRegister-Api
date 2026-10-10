package com.dataquadinc.model;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "payslip",
        uniqueConstraints = @UniqueConstraint(name = "uk_payslip_user_period", columnNames = {"user_id", "pay_period"})
)
@Data
public class Payslip {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, length = 64)
    private String userId;

    /** Stored as yyyy-MM, e.g. 2026-08 */
    @Column(name = "pay_period", nullable = false, length = 7)
    private String payPeriod;

    @Column(name = "payable_days", nullable = false)
    private Integer payableDays;

    @Column(name = "payment_date", nullable = false)
    private LocalDate paymentDate;

    @Column(name = "gross_consultancy_fee", nullable = false, precision = 14, scale = 2)
    private BigDecimal grossConsultancyFee;

    @Column(name = "tds_rate", nullable = false, precision = 5, scale = 4)
    private BigDecimal tdsRate;

    @Column(name = "tds_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal tdsAmount;

    @Column(name = "net_payable", nullable = false, precision = 14, scale = 2)
    private BigDecimal netPayable;

    @Column(name = "amount_in_words", length = 512)
    private String amountInWords;

    @Lob
    @Column(name = "pdf_bytes", columnDefinition = "LONGBLOB")
    private byte[] pdfBytes;

    @Column(name = "created_by", length = 64)
    private String createdBy;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
