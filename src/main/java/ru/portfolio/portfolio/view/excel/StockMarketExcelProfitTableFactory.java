package ru.portfolio.portfolio.view.excel;

import org.springframework.stereotype.Component;
import ru.portfolio.portfolio.converter.SecurityEntityConverter;
import ru.portfolio.portfolio.converter.SecurityEventCashFlowEntityConverter;
import ru.portfolio.portfolio.converter.TransactionEntityConverter;
import ru.portfolio.portfolio.entity.PortfolioEntity;
import ru.portfolio.portfolio.entity.SecurityEntity;
import ru.portfolio.portfolio.entity.SecurityEventCashFlowEntity;
import ru.portfolio.portfolio.pojo.CashFlowType;
import ru.portfolio.portfolio.pojo.SecurityEventCashFlow;
import ru.portfolio.portfolio.pojo.Transaction;
import ru.portfolio.portfolio.repository.SecurityEventCashFlowRepository;
import ru.portfolio.portfolio.repository.SecurityRepository;
import ru.portfolio.portfolio.repository.TransactionCashFlowRepository;
import ru.portfolio.portfolio.repository.TransactionRepository;
import ru.portfolio.portfolio.view.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import static ru.portfolio.portfolio.view.excel.StockMarketExcelProfitTableHeader.*;

@Component
public class StockMarketExcelProfitTableFactory extends ProfitTableFactory {
    private final SecurityEventCashFlowRepository securityEventCashFlowRepository;
    private final SecurityEventCashFlowEntityConverter securityEventCashFlowEntityConverter;

    public StockMarketExcelProfitTableFactory(TransactionRepository transactionRepository,
                                              SecurityRepository securityRepository,
                                              TransactionCashFlowRepository transactionCashFlowRepository,
                                              TransactionEntityConverter transactionEntityConverter,
                                              SecurityEntityConverter securityEntityConverter,
                                              PaidInterestFactory paidInterestFactory,
                                              SecurityEventCashFlowRepository securityEventCashFlowRepository,
                                              SecurityEventCashFlowEntityConverter securityEventCashFlowEntityConverter) {
        super(transactionRepository, securityRepository, transactionCashFlowRepository,
                transactionEntityConverter, securityEntityConverter, paidInterestFactory);
        this.securityEventCashFlowRepository = securityEventCashFlowRepository;
        this.securityEventCashFlowEntityConverter = securityEventCashFlowEntityConverter;
    }


    @Override
    protected Collection<String> getSecuritiesIsin(PortfolioEntity portfolio) {
        return transactionRepository.findDistinctIsinByPortfolioOrderByTimestamp(portfolio);
    }

    @Override
    protected Deque<SecurityEventCashFlow> getRedemption(PortfolioEntity portfolio, SecurityEntity securityEntity) {
        return securityEventCashFlowRepository
                .findByPortfolioAndIsinAndCashFlowTypeOrderByTimestampAsc(
                        portfolio.getPortfolio(),
                        securityEntity.getIsin(),
                        CashFlowType.REDEMPTION)
                .stream()
                .map(securityEventCashFlowEntityConverter::fromEntity)
                .collect(Collectors.toCollection(LinkedList::new));
    }

    @Override
    protected ProfitTable.Record getOpenedPositionProfit(OpenedPosition position) {
        ProfitTable.Record row = new ProfitTable.Record();
        Transaction transaction = position.getOpenTransaction();
        row.put(BUY_DATE, transaction.getTimestamp());
        row.put(COUNT, position.getCount());
        row.put(BUY_PRICE, getTransactionCashFlow(transaction, CashFlowType.PRICE, 1d / transaction.getCount()));
        double multipier = Math.abs(1d * position.getCount() / transaction.getCount());
        row.put(BUY_AMOUNT, getTransactionCashFlow(transaction, CashFlowType.PRICE, multipier));
        row.put(BUY_ACCRUED_INTEREST, getTransactionCashFlow(transaction, CashFlowType.ACCRUED_INTEREST, multipier));
        row.put(BUY_COMMISSION, getTransactionCashFlow(transaction, CashFlowType.COMMISSION, multipier));
        return row;
    }

    @Override
    protected ProfitTable.Record getClosedPositionProfit(ClosedPosition position) {
        // open transaction info
        ProfitTable.Record row = new ProfitTable.Record(getOpenedPositionProfit(position));
        // close transaction info
        Transaction transaction = position.getCloseTransaction();
        double multipier = Math.abs(1d * position.getCount() / transaction.getCount());
        row.put(CELL_DATE, transaction.getTimestamp());
        BigDecimal cellAmount;
        switch (position.getClosingEvent()) {
            case PRICE:
                cellAmount = getTransactionCashFlow(transaction, CashFlowType.PRICE, multipier);
                break;
            case REDEMPTION:
                cellAmount = getRedemptionCashFlow(transaction.getPortfolio(), transaction.getIsin(), multipier);
                break;
            default:
                throw new IllegalArgumentException("ЦБ " + transaction.getIsin() +
                        " не может быть закрыта событием типа " + position.getClosingEvent());
        }
        row.put(CELL_AMOUNT, cellAmount);
        row.put(CELL_ACCRUED_INTEREST, getTransactionCashFlow(transaction, CashFlowType.ACCRUED_INTEREST, multipier));
        row.put(CELL_COMMISSION, getTransactionCashFlow(transaction, CashFlowType.COMMISSION, multipier));
        row.put(FORECAST_TAX, getForecastTax());
        row.put(PROFIT, getClosedPositionProfit());
        return row;
    }

    @Override
    protected ProfitTable.Record getPaidInterestProfit(Position position,
                                                       PaidInterest paidInterest) {
        ProfitTable.Record info = new ProfitTable.Record();
        info.put(COUPON, convertPaidInterestToExcelFormula(paidInterest.get(CashFlowType.COUPON, position)));
        info.put(AMORTIZATION, convertPaidInterestToExcelFormula(paidInterest.get(CashFlowType.AMORTIZATION, position)));
        info.put(DIVIDEND, convertPaidInterestToExcelFormula(paidInterest.get(CashFlowType.DIVIDEND, position)));
        info.put(TAX, convertPaidInterestToExcelFormula(paidInterest.get(CashFlowType.TAX, position)));
        return info;
    }

    private BigDecimal getRedemptionCashFlow(String portfolio, String isin, double multiplier) {
        List<SecurityEventCashFlowEntity> cashFlows = securityEventCashFlowRepository
                .findByPortfolioAndIsinAndCashFlowTypeOrderByTimestampAsc(portfolio, isin, CashFlowType.REDEMPTION);
        if (cashFlows.isEmpty()) {
            return null;
        } else if (cashFlows.size() != 1) {
            throw new IllegalArgumentException("По ЦБ может быть не более одного события погашения, по бумаге " + isin +
                    " найдено " + cashFlows.size() + " событий погашения: " + cashFlows);
        }
        return cashFlows.get(0)
                .getValue()
                .multiply(BigDecimal.valueOf(multiplier))
                .abs()
                .setScale(2, RoundingMode.HALF_UP);
    }

    public static  <T extends Position> String convertPaidInterestToExcelFormula(List<SecurityEventCashFlow> pays) {
        if (pays == null || pays.isEmpty()) {
            return null;
        }
        return pays.stream()
                .map(SecurityEventCashFlow::getValue)
                .map(BigDecimal::abs)
                .map(String::valueOf)
                .collect(Collectors.joining("+", "=", ""));
    }

    private String getForecastTax() {
        String forecastTaxFormula =
                "(" + CELL_AMOUNT.getCellAddr() + "+" + CELL_ACCRUED_INTEREST.getCellAddr() + "+" + AMORTIZATION.getCellAddr() + ")" +
                        "-(" + BUY_AMOUNT.getCellAddr() + "+" + BUY_ACCRUED_INTEREST.getCellAddr() + ")" +
                        "-" + BUY_COMMISSION.getCellAddr() + "-" + CELL_COMMISSION.getCellAddr();
        return "=IF(" + forecastTaxFormula + "<0,0,0.13*(" + forecastTaxFormula + "))";
    }

    private String getClosedPositionProfit() {
        String buy = "(" + BUY_AMOUNT.getCellAddr() + "+" + BUY_ACCRUED_INTEREST.getCellAddr() + "+" + BUY_COMMISSION.getCellAddr() + ")";
        String cell = "(" + CELL_AMOUNT.getCellAddr() + "+" + CELL_ACCRUED_INTEREST.getCellAddr() + "+" +
                COUPON.getCellAddr() + "+" + AMORTIZATION.getCellAddr() + "+" + DIVIDEND.getCellAddr() +
                "-(" + CELL_COMMISSION.getCellAddr() + "+" + TAX.getCellAddr() + "+" + FORECAST_TAX.getCellAddr() + "))";
        String multiplicator = "100*365/DAYS360(" + BUY_DATE.getCellAddr() + "," + CELL_DATE.getCellAddr() + ")";
        return "=((" + cell + "-" + buy + ")/" + buy + ")*" + multiplicator;
    }
}
