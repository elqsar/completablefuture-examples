package cz.boris.shop;


import java.math.BigDecimal;

public record PriceRecord(Discount.Code code, BigDecimal price) {
} 