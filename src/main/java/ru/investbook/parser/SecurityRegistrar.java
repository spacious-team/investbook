package ru.investbook.parser;

import org.spacious_team.broker.pojo.Security;
import org.spacious_team.broker.pojo.Security.SecurityBuilder;

import java.util.function.Supplier;

public interface SecurityRegistrar {

    /**
     * Checks is stock with ISIN exists and creates provided if not
     *
     * @return security ID
     */
    int declareStockByIsin(String isin, Supplier<SecurityBuilder> supplier);

    int declareBondByIsin(String isin, Supplier<SecurityBuilder> supplier);

    int declareStockOrBondByIsin(String isin, Supplier<SecurityBuilder> supplier);

    int declareStockByName(String name, Supplier<SecurityBuilder> supplier);

    int declareBondByName(String name, Supplier<SecurityBuilder> supplier);

    int declareStockOrBondByName(String name, Supplier<SecurityBuilder> supplier);

    int declareStockByTicker(String ticker, Supplier<SecurityBuilder> supplier);

    int declareBondByTicker(String ticker, Supplier<SecurityBuilder> supplier);

    int declareStockOrBondByTicker(String ticker, Supplier<SecurityBuilder> supplier);

    int declareDerivative(String code);

    /**
     * @param contract in USDRUB_TOM form
     * @return security ID
     */
    int declareCurrencyPair(String contract);

    /**
     * @return security ID
     */
    int declareAsset(String assetName, Supplier<SecurityBuilder> supplier);

    int declareSecurity(Security security);
}
