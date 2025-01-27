package ai.shreds.domain.value_objects;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;
import java.util.Objects;

public final class DomainValueMoney {
    private final BigDecimal amount;
    private final String currency;
    
    private static final int DECIMAL_PLACES = 2;

    private DomainValueMoney(BigDecimal amount, String currency) {
        validateAmount(amount);
        validateCurrency(currency);
        this.amount = amount.setScale(DECIMAL_PLACES, RoundingMode.HALF_UP);
        this.currency = currency;
    }

    public static DomainValueMoney of(BigDecimal amount, String currency) {
        return new DomainValueMoney(amount, currency);
    }

    public static DomainValueMoney zero(String currency) {
        return new DomainValueMoney(BigDecimal.ZERO, currency);
    }

    private void validateAmount(BigDecimal amount) {
        if (amount == null) {
            throw new IllegalArgumentException("Amount cannot be null");
        }
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Amount cannot be negative");
        }
    }

    private void validateCurrency(String currency) {
        if (currency == null || currency.trim().isEmpty()) {
            throw new IllegalArgumentException("Currency cannot be null or empty");
        }
        try {
            Currency.getInstance(currency);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid currency code: " + currency);
        }
    }

    public DomainValueMoney add(DomainValueMoney other) {
        validateSameCurrency(other);
        return new DomainValueMoney(
            this.amount.add(other.amount),
            this.currency
        );
    }

    public DomainValueMoney subtract(DomainValueMoney other) {
        validateSameCurrency(other);
        BigDecimal result = this.amount.subtract(other.amount);
        if (result.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Subtraction would result in negative amount");
        }
        return new DomainValueMoney(result, this.currency);
    }

    public DomainValueMoney multiply(int multiplier) {
        if (multiplier < 0) {
            throw new IllegalArgumentException("Multiplier cannot be negative");
        }
        return new DomainValueMoney(
            this.amount.multiply(new BigDecimal(multiplier)),
            this.currency
        );
    }

    public DomainValueMoney multiply(BigDecimal multiplier) {
        if (multiplier.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Multiplier cannot be negative");
        }
        return new DomainValueMoney(
            this.amount.multiply(multiplier).setScale(DECIMAL_PLACES, RoundingMode.HALF_UP),
            this.currency
        );
    }

    private void validateSameCurrency(DomainValueMoney other) {
        if (!this.currency.equals(other.currency)) {
            throw new IllegalArgumentException(
                String.format("Currency mismatch: %s vs %s", this.currency, other.currency)
            );
        }
    }

    public boolean isGreaterThan(DomainValueMoney other) {
        validateSameCurrency(other);
        return this.amount.compareTo(other.amount) > 0;
    }

    public boolean isLessThan(DomainValueMoney other) {
        validateSameCurrency(other);
        return this.amount.compareTo(other.amount) < 0;
    }

    public boolean isZero() {
        return this.amount.compareTo(BigDecimal.ZERO) == 0;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DomainValueMoney that = (DomainValueMoney) o;
        return amount.compareTo(that.amount) == 0 && 
               currency.equals(that.currency);
    }

    @Override
    public int hashCode() {
        return Objects.hash(amount, currency);
    }

    @Override
    public String toString() {
        return String.format("%s %s", 
            amount.setScale(DECIMAL_PLACES, RoundingMode.HALF_UP).toString(),
            currency
        );
    }
}
