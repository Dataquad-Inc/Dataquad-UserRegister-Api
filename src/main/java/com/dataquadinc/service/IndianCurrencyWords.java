package com.dataquadinc.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Converts amounts to Indian Rupees words, e.g. "Rupees Fifty-Six Thousand Only".
 */
public final class IndianCurrencyWords {

    private static final String[] ONES = {
            "", "One", "Two", "Three", "Four", "Five", "Six", "Seven", "Eight", "Nine",
            "Ten", "Eleven", "Twelve", "Thirteen", "Fourteen", "Fifteen", "Sixteen",
            "Seventeen", "Eighteen", "Nineteen"
    };

    private static final String[] TENS = {
            "", "", "Twenty", "Thirty", "Forty", "Fifty", "Sixty", "Seventy", "Eighty", "Ninety"
    };

    private IndianCurrencyWords() {
    }

    public static String toWords(BigDecimal amount) {
        if (amount == null) {
            return "Rupees Zero Only";
        }
        BigDecimal normalized = amount.setScale(2, RoundingMode.HALF_UP);
        long rupees = normalized.longValue();
        int paise = normalized.remainder(BigDecimal.ONE)
                .movePointRight(2)
                .setScale(0, RoundingMode.HALF_UP)
                .intValue();

        StringBuilder sb = new StringBuilder("Rupees ");
        sb.append(convert(rupees));
        if (paise > 0) {
            sb.append(" and ").append(convert(paise)).append(" Paise");
        }
        sb.append(" Only");
        return sb.toString();
    }

    private static String convert(long n) {
        if (n == 0) {
            return "Zero";
        }
        StringBuilder sb = new StringBuilder();
        long crore = n / 10000000;
        n %= 10000000;
        long lakh = n / 100000;
        n %= 100000;
        long thousand = n / 1000;
        n %= 1000;
        long hundred = n / 100;
        n %= 100;

        if (crore > 0) {
            sb.append(convertBelowThousand(crore)).append(" Crore ");
        }
        if (lakh > 0) {
            sb.append(convertBelowThousand(lakh)).append(" Lakh ");
        }
        if (thousand > 0) {
            sb.append(convertBelowThousand(thousand)).append(" Thousand ");
        }
        if (hundred > 0) {
            sb.append(ONES[(int) hundred]).append(" Hundred ");
        }
        if (n > 0) {
            if (sb.length() > 0) {
                // keep spacing clean before tens/ones
            }
            sb.append(convertBelowThousand(n)).append(" ");
        }
        return hyphenateCompound(sb.toString().trim());
    }

    private static String convertBelowThousand(long n) {
        if (n < 20) {
            return ONES[(int) n];
        }
        if (n < 100) {
            int ten = (int) (n / 10);
            int one = (int) (n % 10);
            if (one == 0) {
                return TENS[ten];
            }
            return TENS[ten] + "-" + ONES[one];
        }
        long hundred = n / 100;
        long rest = n % 100;
        if (rest == 0) {
            return ONES[(int) hundred] + " Hundred";
        }
        return ONES[(int) hundred] + " Hundred " + convertBelowThousand(rest);
    }

    /** Match sample style: "Fifty-Six Thousand" */
    private static String hyphenateCompound(String words) {
        return words.replaceAll("\\s+", " ").trim();
    }
}
