package ru.portfolio.portfolio.view.excel;

import org.springframework.stereotype.Component;
import ru.portfolio.portfolio.converter.SecurityEntityConverter;
import ru.portfolio.portfolio.converter.TransactionEntityConverter;
import ru.portfolio.portfolio.entity.PortfolioEntity;
import ru.portfolio.portfolio.entity.SecurityEntity;
import ru.portfolio.portfolio.pojo.CashFlowType;
import ru.portfolio.portfolio.pojo.SecurityEventCashFlow;
import ru.portfolio.portfolio.pojo.Transaction;
import ru.portfolio.portfolio.repository.SecurityRepository;
import ru.portfolio.portfolio.repository.TransactionCashFlowRepository;
import ru.portfolio.portfolio.repository.TransactionRepository;
import ru.portfolio.portfolio.view.*;

import java.util.Collection;
import java.util.Deque;
import java.util.LinkedList;

import static ru.portfolio.portfolio.view.excel.DerivativesMarketExcelProfitTableHeader.*;
import static ru.portfolio.portfolio.view.excel.StockMarketExcelProfitTableFactory.convertPaidInterestToExcelFormula;

@Component
public class DerivativesMarketExcelProfitTableFactory extends ProfitTableFactory {

    public DerivativesMarketExcelProfitTableFactory(TransactionRepository transactionRepository,
                                                    SecurityRepository securityRepository,
                                                    TransactionCashFlowRepository transactionCashFlowRepository,
                                                    TransactionEntityConverter transactionEntityConverter,
                                                    SecurityEntityConverter securityEntityConverter,
                                                    PaidInterestFactory paidInterestFactory) {
        super(transactionRepository, securityRepository, transactionCashFlowRepository,
                transactionEntityConverter, securityEntityConverter, paidInterestFactory);
    }


    @Override
    protected Collection<String> getSecuritiesIsin(PortfolioEntity portfolio) {
        return transactionRepository.findDistinctDerivativeByPortfolioOrderByTimestamp(portfolio);
    }

    @Override
    protected Deque<SecurityEventCashFlow> getRedemption(PortfolioEntity portfolio, SecurityEntity securityEntity) {
        return new LinkedList<>();
    }

    @Override
    protected ProfitTable.Record getOpenedPositionProfit(OpenedPosition position) {
        ProfitTable.Record record = new ProfitTable.Record();
        Transaction transaction = position.getOpenTransaction();
        record.put(OPEN_DATE, transaction.getTimestamp());
        record.put(COUNT, position.getCount());
        record.put(OPEN_QUOTE, getTransactionCashFlow(transaction, CashFlowType.PRICE, 1d / transaction.getCount()));
        double multipier = Math.abs(1d * position.getCount() / transaction.getCount());
        record.put(OPEN_COMMISSION, getTransactionCashFlow(transaction, CashFlowType.COMMISSION, multipier));
        return record;
    }

    @Override
    protected ProfitTable.Record getClosedPositionProfit(ClosedPosition position) {
        // open transaction info
        ProfitTable.Record row = new ProfitTable.Record(getOpenedPositionProfit(position));
        // close transaction info
        Transaction transaction = position.getCloseTransaction();
        double multipier = Math.abs(1d * position.getCount() / transaction.getCount());
        row.put(CLOSE_DATE, transaction.getTimestamp());
        row.put(CLOSE_QUOTE, getTransactionCashFlow(transaction, CashFlowType.PRICE, 1d / transaction.getCount()));
        row.put(CLOSE_COMMISSION, getTransactionCashFlow(transaction, CashFlowType.COMMISSION, multipier));
        row.put(FORECAST_TAX, getForecastTax());
        row.put(PROFIT, getClosedPositionProfit());
        return row;
    }

    @Override
    protected ProfitTable.Record getPaidInterestProfit(Position position,
                                                       PaidInterest paidInterest) {
        ProfitTable.Record info = new ProfitTable.Record();
        info.put(PAYMENT, convertPaidInterestToExcelFormula(paidInterest.get(CashFlowType.DERIVATIVE_PROFIT, position)));
        return info;
    }

    private String getForecastTax() {
        return "=ЕСЛИ(" + PAYMENT.getCellAddr() + ">0;0,13*" + PAYMENT.getCellAddr() + ";0)";
    }

    private String getClosedPositionProfit() {
        return "=" + PAYMENT.getCellAddr() +
                " - " + OPEN_COMMISSION.getCellAddr() +
                " - " + CLOSE_COMMISSION.getCellAddr() +
                " - " + FORECAST_TAX.getCellAddr();
    }
}
