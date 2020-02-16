package ru.portfolio.portfolio.pojo;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum CashFlowEvent {
    CASH(0),   // пополнение и снятие
    PRICE(1),  // чистая стоимость (без НКД)
    ACCRUED_INTEREST(2), // НКД
    COMMISSION(3), // комиссия
    AMORTIZATION(4),    // амортизация
    REDEMPTION(5), // погашение номинала облигации
    COUPON(6), // выплата купона
    DIVIDEND(7), // выплата дивиденда
    DERIVATIVE_PROFIT(8),// вариационная маржа
    MARGIN(9), // гарантийное обеспечение
    TAX(10), // налог уплаченный
    FORECAST_TAX(11); // прогнозируемый налог

    @Getter
    private final int type;
}
