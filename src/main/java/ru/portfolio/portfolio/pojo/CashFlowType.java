package ru.portfolio.portfolio.pojo;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum CashFlowType {
    CASH(0),   // пополнение и снятие
    PRICE(1),  // купля/продажа, чистая стоимость (без НКД)
    ACCRUED_INTEREST(2), // НКД
    COMMISSION(3), // комиссия
    AMORTIZATION(4),    // амортизация
    REDEMPTION(5), // погашение номинала облигации
    COUPON(6), // выплата купона
    DIVIDEND(7), // выплата дивиденда
    DERIVATIVE_PROFIT(8),// вариационная маржа
    MARGIN(9), // гарантийное обеспечение
    TAX(10), // налог уплаченный
    FORECAST_TAX(11), // прогнозируемый налог
    DERIVATIVE_PRICE(12), // Стоимость сделки с деривативом, рубли
    DERIVATIVE_QUOTE(13); // Стоимость сделки с деривативом, пункты

    @Getter
    private final int id;

    public static CashFlowType valueOf(int type) {
        for (CashFlowType e : values()) {
            if (e.getId() == type) {
                return e;
            }
        }
        throw new IllegalArgumentException("Не верный тип события: " + type);
    }
}
