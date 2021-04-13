/*
 * InvestBook
 * Copyright (C) 2021  Vitalii Ananev <an-vitek@ya.ru>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package ru.investbook.service.moex;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.Integer.parseInt;

/**
 * Converts derivative short names (for ex. Si-6.21) to secid (a.k.a ticker codes, for ex SiM1)
 *
 * @see <a href="https://www.moex.com/s205">Specifications ticker codes for Futures and Options</a>
 */
@Component
public class MoexDerivativeSecidHelper {

    private final Map<String, String> shortnamesToSecid = Stream.of(new String[][]{
            {"MX", "MIX"},  // Индекс МосБиржи
            {"MM", "MXI"},  // Индекс МосБиржи (мини)
            {"RI", "RTS"},  // Индекс РТС
            {"RS", "RTSS"}, // Индекс голубых фишек
            {"4B", "ALSI"}, // Индекс FTSE/JSE Top40
            {"VI", "RVI"},  // Волатильность российского рынка
            {"AF", "AFLT"}, // ПАО "Аэрофлот" (о.а.)
            {"AL", "ALRS"}, // АК "АЛРОСА" (ПАО) (о.а.)
            {"CH", "CHMF"}, // ПАО "Северсталь" (о.а.)
            {"FS", "FEES"}, // ПАО "ФСК ЕЭС" (о.а.)
            {"GZ", "GAZR"}, // ПАО "Газпром" (о.а.)
            {"GK", "GMKN"}, // ПАО ГМК "Норильский Никель" (о.а.)
            {"HY", "HYDR"}, // ПАО "РусГидро" (о.а.)
            {"LK", "LKOH"}, // ПАО НК "ЛУКОЙЛ" (о.а.)
            {"MN", "MGNT"}, // ПАО "Магнит" (о.а.)
            {"ME", "MOEX"}, // ПАО Московская Биржа (о.а.)
            {"MT", "MTSI"}, // ПАО "МТС" (о.а.)
            {"NM", "NLMK"}, // ПАО "НЛМК" (о.а.)
            {"NK", "NOTK"}, // ПАО "НОВАТЭК" (о.а.)
            {"RN", "ROSN"}, // ПАО "НК "Роснефть" (о.а.)
            {"RT", "RTKM"}, // ПАО "Ростелеком" (о.а.)
            {"SP", "SBPR"}, // ПАО Сбербанк (п.а.)
            {"SR", "SBRF"}, // ПАО Сбербанк (о.а.)
            {"SG", "SNGP"}, // ПАО "Сургутнефтегаз" (п.а.)
            {"SN", "SNGR"}, // ПАО "Сургутнефтегаз" (о.а.)
            {"TT", "TATN"}, // ПАО "Татнефть" им. В.Д. Шашина (о.а.)
            {"TN", "TRNF"}, // ПАО "Транснефть" (п.а.)
            {"TF", "TRNS"}, // 0, 1 стоимости одной акции ПАО "Транснефть"
            {"VB", "VTBR"}, // Банк ВТБ (ПАО) (о.а.)
            {"MG", "MAGN"}, // ПАО "Магнитогорский металлургический ком­бинат" (о.а.)
            {"PL", "PLZL"}, // ПАО "Полюс" (о.а.)
            {"YN", "YNDF"}, // Яндекс Н.В. (о.а.)
            {"AK", "AFKS"}, // АФК Система (о.а.)
            {"IR", "IRAO"}, // ПАО "Интер РАО ЕЭС" (о.а.)
            {"PO", "POLY"}, // Полиметалл Интернэшнл (о.а.)
            {"TI", "TCSI"}, // ГДР ТиСиЭс Груп Холдинг ПиЭлСи
            {"FV", "FIVE"}, // ГДР Икс 5 Ритейл Груп Н.В
            {"ML", "MAIL"}, // ГДР Мэйл.ру Груп Лимитед
            {"OZ", "OZON"}, // АДР Озон Холдингс Пи Эл Си
            {"BW", "GBMW"}, // BMW AG (о.а.)
            {"DM", "GDAI"}, // Daimler AG (о.а.)
            {"DB", "GDBK"}, // Deutsche Bank AG (о.а.)
            {"SM", "GSIE"}, // Siemens AG (о.а.)
            {"VM", "GVW3"}, // Volkswagen AG (п.а.)
            {"OX", "OF10"}, // "десятилетние" облигации федерального займа
            {"OV", "OF15"}, // "пятнадцатилетние" облигации федерального займа
            {"O2", "OFZ2"}, // "двухлетние" облигации федерального займа
            {"O4", "OFZ4"}, // "четырехлетние" облигации федерального займа
            {"O6", "OFZ6"}, // "шестилетние" облигации федерального займа
            {"MP", "MOPR"}, // ставка MosPrime
            {"RR", "RUON"}, // ставка RUONIA
            {"MF", "1MFR"}, // ставка RUSFAR
            {"DF", "1MDR"}, // Ставка RUSFARUSD
            {"AU", "AUDU"}, // курс австралийский доллар – доллар США
            {"CY", "CY"},   // курс китайский юань – российский рубль
            {"ED", "ED"},   // курс евро – доллар США
            {"Eu", "Eu"},   // курс евро – российский рубль
            {"GU", "GBPU"}, // курс фунт стерлингов – доллар США
            {"Si", "Si"},   // курс доллар США – российский рубль
            {"CA", "UCAD"}, // курс доллар США – канадский доллар
            {"CF", "UCHF"}, // курс доллар США – швейцарский франк
            {"JP", "UJPY"}, // курс доллар США – японская йена
            {"TR", "UTRY"}, // курс доллар США – турецкая лира
            {"IN", "UINR"}, // Курс доллара США к индийской рупии
            {"UU", "UUAH"}, // курс доллар США – украинская гривна
            {"BR", "BR"},   // нефть BRENT
            {"CU", "CU"},   // медь
            {"GD", "GOLD"}, // золото
            {"PD", "PLD"},  // палладий
            {"PT", "PLT"},  // платина
            {"SV", "SILV"}, // серебро
            {"SA", "SUGR"}, // сахар-сырец
            {"SL", "SLV"},  // серебро (поставочное)
            {"AM", "ALMN"}, // алюминий
            {"CL", "CL"},   // нефть сорта Light Sweet Crude Oil
            {"Co", "Co"},   // медь категории A (Grade A)
            {"GO", "GLD"},  // золото (поставочный)
            {"Nl", "Nl"},   // никель с чистотой 99,80% (минимум)
            {"Zn", "Zn"},   // цинк
            {"NG", "NG"},   // природный газ
            {"WH", "WH4"}   // пшеница
    }).collect(Collectors.toMap(a -> a[1], a -> a[0]));

    private final Character[] futuresContractMonthCodes =
            new Character[] {'F', 'G', 'H', 'J', 'K', 'M', 'N', 'Q', 'U', 'V', 'X', 'Z'};
    private final Character[] callOptionMonthCodes =
            new Character[] {'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'G', 'K', 'L'};
    private final Character[] putOptionMonthCodes =
            new Character[] {'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X'};

    public Optional<String> getFuturesContractSecidIfCan(String contractShortName) {
        try {
            if (!isFutures(contractShortName)) {
                return Optional.empty();
            }
            int dashPosition = contractShortName.indexOf('-');
            if (dashPosition == -1) {
                return Optional.of(contractShortName);
            }
            int dotPosition = contractShortName.indexOf('.');
            if (dotPosition == -1) {
                return Optional.empty();
            }
            int month = parseInt(contractShortName.substring(dashPosition + 1, dotPosition));
            int year = parseInt(contractShortName.substring(dotPosition + 1));
            return Optional.ofNullable(shortnamesToSecid.get(contractShortName.substring(0, dashPosition)))
                    .map(prefix -> prefix + futuresContractMonthCodes[month - 1] + (year % 10));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    /**
     * Only for derivative contracts.
     *
     * @return true for futures contract (in {@code Si-6.21} or {@code SiM1} format),
     *         false for options (in {@code Si65000BC9}, {@code Si65000BC9D} or {@code Si-6.19M280319CA65000} format)
     */
    public boolean isFutures(String contract) {
        if (contract.length() == 4) {
            return true;
        } else if (contract.indexOf('-') == -1) {
            return false;
        } else {
            return contract.charAt(contract.length() - 3) == '.';
        }
    }

    /**
     * May be false positive.
     *
     * @return true for option (and true in rare case for other assets), false for other securities
     */
    public boolean isSecidPossibleOption(String moexSecid) {
        int length = moexSecid.length();
        return (length >= 10) && (length <= 12);
    }

    /**
     * @param optionSecid option's moex secid in {@code Si65000BC9}, {@code Si65000BC9D}, {@code RI180000BD1} or
     *                   {@code RI180000BD1A} format
     * @return futures contract secid (for ex. {@code SiH9}) if it can be calculated, empty optional otherwise
     */
    public Optional<String> getOptionUnderlingFuturesContract(String optionSecid) {
        try {
            String code = optionSecid.substring(0, 2);
            if (shortnamesToSecid.containsKey(code)) {
                int monthPos = Character.isDigit(optionSecid.charAt(optionSecid.length() - 1)) ?
                        optionSecid.length() - 2 :
                        optionSecid.length() - 3;
                char optionMonth = optionSecid.charAt(monthPos);
                int month = optionMonth - callOptionMonthCodes[0];
                if (month < 0 || month > 11) {
                    month = optionMonth - putOptionMonthCodes[0];
                }
                if (month >= 0 && month <= 11) {
                    char optionYear = optionSecid.charAt(monthPos + 1);
                    return Optional.of(code + futuresContractMonthCodes[month] + parseInt(Character.toString(optionYear)));
                }
            }
        } catch (Exception ignore) {
        }
        return Optional.empty();
    }
}
