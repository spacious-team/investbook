package ru.investbook.parser;

import org.spacious_team.broker.pojo.Security.SecurityBuilder;

import java.util.function.Supplier;

public interface SecurityRegistrar {

    /**
     * Checks is stock with ISIN exists and creates provided if not
     *
     * @return security ID
     */
    String declareStock(String isin, Supplier<SecurityBuilder> supplier);

    String declareBond(String isin, Supplier<SecurityBuilder> supplier);

    String declareStockOrBond(String isin, Supplier<SecurityBuilder> supplier);

    String declareDerivative(String code);

    /**
     * @param contract in USDRUB_TOM form
     * @return security ID
     */
    String declareCurrencyPair(String contract);

    /**
     * @return security ID
     */
    String declareAsset(String assetName, Supplier<SecurityBuilder> supplier);
}
