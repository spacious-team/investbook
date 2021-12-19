package ru.investbook.parser;

import org.spacious_team.broker.pojo.Security.SecurityBuilder;

import java.util.function.Supplier;

public interface SecurityRegistrar {

    /**
     * Checks is stock with ISIN exists and creates provided if not
     *
     * @return security ID
     */
    int declareStock(String isin, Supplier<SecurityBuilder> supplier);

    int declareBond(String isin, Supplier<SecurityBuilder> supplier);

    int declareStockOrBond(String isin, Supplier<SecurityBuilder> supplier);

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
}
