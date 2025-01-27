package ai.shreds.domain.value_objects;

import ai.shreds.domain.exceptions.DomainExceptionInvalidOrder;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;
import java.util.Objects;

/**
 * Value object representing monetary values in the domain.
 * Ensures proper handling of currency amounts with precise decimal arithmetic.
 */
public final class DomainMoneyValue {

    private static final int DECIMAL_PLACES = 2;
    private static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_UP;

    private final BigDecimal amount;
    private final String currency;

    private DomainMoneyValue(BigDecimal amount, String currency) {
        validateAmount(amount);
        validateCurrency(currency);
        this.amount = amount.setScale(DECIMAL_PLACES, ROUNDING_MODE);
        this.currency = currency;
    }

    /**
     * Creates a new money value.
     *
     * @param amount The monetary amount
     * @param currency The currency code
     * @return A new DomainMoneyValue instance
     * @throws DomainExceptionInvalidOrder if amount or currency is invalid
     */
    public static DomainMoneyValue of(BigDecimal amount, String currency) {
        return new DomainMoneyValue(amount, currency);
    }

    /**
     * Creates a zero money value in the specified currency.
     *
     * @param currency The currency code
     * @return A new DomainMoneyValue instance with zero amount
     */
    public static DomainMoneyValue zero(String currency) {
        return new DomainMoneyValue(BigDecimal.ZERO, currency);
    }

    private void validateAmount(BigDecimal amount) {
        if (amount == null) {
            throw new DomainExceptionInvalidOrder("Amount cannot be null");
        }
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new DomainExceptionInvalidOrder("Amount cannot be negative");
        }
    }

    private void validateCurrency(String currency) {
        if (currency == null || currency.trim().isEmpty()) {
            throw new DomainExceptionInvalidOrder("Currency cannot be null or empty");
        }
        try {
            Currency.getInstance(currency);
        } catch (IllegalArgumentException e) {
            throw new DomainExceptionInvalidOrder("Invalid currency code: " + currency);
        }
    }

    /**
     * Adds another money value to this one.
     *
     * @param other The money value to add
     * @return A new DomainMoneyValue with the sum
     * @throws DomainExceptionInvalidOrder if currencies don't match
     */
    public DomainMoneyValue add(DomainMoneyValue other) {
        validateSameCurrency(other);
        return new DomainMoneyValue(
            this.amount.add(other.amount),
            this.currency
        );
    }

    /**
     * Subtracts another money value from this one.
     *
     * @param other The money value to subtract
     * @return A new DomainMoneyValue with the difference
     * @throws DomainExceptionInvalidOrder if currencies don't match or result would be negative
     */
    public DomainMoneyValue subtract(DomainMoneyValue other) {
        validateSameCurrency(other);
        BigDecimal result = this.amount.subtract(other.amount);
        if (result.compareTo(BigDecimal.ZERO) < 0) {
            throw new DomainExceptionInvalidOrder("Subtraction would result in negative amount");
        }
        return new DomainMoneyValue(result, this.currency);
    }

    /**
     * Multiplies this money value by a factor.
     *
     * @param multiplier The multiplication factor
     * @return A new DomainMoneyValue with the product
     * @throws DomainExceptionInvalidOrder if multiplier is negative
     */
    public DomainMoneyValue multiply(BigDecimal multiplier) {
        if (multiplier.compareTo(BigDecimal.ZERO) < 0) {
            throw new DomainExceptionInvalidOrder("Multiplier cannot be negative");
        }
        return new DomainMoneyValue(
            this.amount.multiply(multiplier).setScale(DECIMAL_PLACES, ROUNDING_MODE),
            this.currency
        );
    }

    /**
     * Multiplies this money value by an integer factor.
     *
     * @param multiplier The multiplication factor
     * @return A new DomainMoneyValue with the product
     * @throws DomainExceptionInvalidOrder if multiplier is negative
     */
    public DomainMoneyValue multiply(int multiplier) {
        if (multiplier < 0) {
            throw new DomainExceptionInvalidOrder("Multiplier cannot be negative");
        }
        return multiply(new BigDecimal(multiplier));
    }

    private void validateSameCurrency(DomainMoneyValue other) {
        if (!this.currency.equals(other.currency)) {
            throw new DomainExceptionInvalidOrder(
                String.format("Currency mismatch: %s vs %s", this.currency, other.currency)
            );
        }
    }

    /**
     * Checks if this amount is greater than another.
     *
     * @param other The money value to compare with
     * @return true if this amount is greater
     * @throws DomainExceptionInvalidOrder if currencies don't match
     */
    public boolean isGreaterThan(DomainMoneyValue other) {
        validateSameCurrency(other);
        return this.amount.compareTo(other.amount) > 0;
    }

    /**
     * Checks if this amount is less than another.
     *
     * @param other The money value to compare with
     * @return true if this amount is less
     * @throws DomainExceptionInvalidOrder if currencies don't match
     */
    public boolean isLessThan(DomainMoneyValue other) {
        validateSameCurrency(other);
        return this.amount.compareTo(other.amount) < 0;
    }

    /**
     * Checks if this amount is zero.
     *
     * @return true if amount is zero
     */
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
        DomainMoneyValue that = (DomainMoneyValue) o;
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
            amount.setScale(DECIMAL_PLACES, ROUNDING_MODE).toString(),
            currency);
    }
}
