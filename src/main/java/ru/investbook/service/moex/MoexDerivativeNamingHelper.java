/*
 * InvestBook
 * Copyright (C) 2021  Vitalii Ananev <spacious-team@ya.ru>
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

import java.time.LocalDate;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.Character.isDigit;
import static java.lang.Integer.parseInt;

/**
 * Converts derivative short names (for ex. Si-6.21) to secid (a.k.a ticker codes, for ex SiM1)
 *
 * @see <a href="https://www.moex.com/s205">Specifications ticker codes for Futures and Options</a>
 */
@Component
public class MoexDerivativeNamingHelper {

    private final Map<String, String> codeToShortnames = Stream.of(new String[][]{
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
            {"MG", "MAGN"}, // ПАО "Магнитогорский металлургический комбинат" (о.а.)
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
    }).collect(Collectors.toMap(a -> a[0], a -> a[1]));

    private final Map<String, String> shortnameToCodes = codeToShortnames.entrySet()
            .stream()
            .collect(Collectors.toMap(Entry::getValue, Entry::getKey));

    private final Character[] futuresMonthCodes =
            new Character[]{'F', 'G', 'H', 'J', 'K', 'M', 'N', 'Q', 'U', 'V', 'X', 'Z'};
    private final Character[] callOptionMonthCodes =
            new Character[]{'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L'};
    private final Character[] putOptionMonthCodes =
            new Character[]{'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X'};

    private final int currentYear = LocalDate.now().getYear();
    // 2000-th, 2010-th, 2020-th and so on
    private final int currentYearDecade = currentYear / 10 * 10;

    /**
     * Only for derivative contracts.
     *
     * @return true for futures contract (in {@code Si-6.21} or {@code SiM1} format),
     * false for options (in {@code Si65000BC9}, {@code Si65000BC9D} or {@code Si-6.19M280319CA65000} format)
     */
    public boolean isFutures(String contract) {
        return isFuturesCode(contract) || isFuturesShortname(contract);
    }

    /**
     * @return true for futures in format {@code SiM1}
     */
    private boolean isFuturesCode(String code) {
        return code.length() == 4 &&
                shortnameToCodes.containsValue(code.substring(0, 2)) &&
                getFuturesMonth(code.charAt(2)) != -1 &&
                isDigit(code.charAt(3));
    }

    /**
     * @return true for futures in format {@code Si-3.21}
     */
    private boolean isFuturesShortname(String shortname) {
        try {
            int dotIdx = shortname.length() - 3;
            if (shortname.charAt(dotIdx) != '.') {
                return false;
            }
            int dashIdx = shortname.indexOf('-');
            if (dashIdx == -1) {
                return false;
            }
            boolean isYearSymbolsDigit = isDigit(shortname.charAt(dotIdx + 1)) &&
                    isDigit(shortname.charAt(dotIdx + 2));
            if (isYearSymbolsDigit && shortnameToCodes.containsKey(shortname.substring(0, dashIdx))) {
                int month = parseInt(shortname.substring(dashIdx + 1, dotIdx));
                return month >= 1 && month <= 12;
            }
        } catch (Exception ignore) {
        }
        return false;
    }

    /**
     * @return {@code SiM1} for futures contract in {@code Si-6.21} or {@code SiM1} format
     */
    public Optional<String> getFuturesCode(String contract) {
        try {
            int dashIdx = contract.indexOf('-');
            if (dashIdx == -1) {
                return isFuturesCode(contract) ? Optional.of(contract) : Optional.empty();
            }
            int dotIdx = contract.length() - 3;
            if (contract.charAt(dotIdx) != '.') {
                return Optional.empty();
            }
            int month = parseInt(contract.substring(dashIdx + 1, dotIdx));
            int year = parseInt(contract.substring(dotIdx + 1));
            return Optional.ofNullable(shortnameToCodes.get(contract.substring(0, dashIdx)))
                    .map(prefix -> prefix + futuresMonthCodes[month - 1] + (year % 10));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    /**
     * @return {@code Si-6.21} for futures contract in {@code Si-6.21} or {@code SiM1} format
     */
    public Optional<String> getFuturesShortname(String contract) {
        if (isFuturesShortname(contract)) {
            return Optional.of(contract);
        } else if (contract.length() != 4) {
            return Optional.empty();
        }
        int month = getFuturesMonth(contract.charAt(2));
        if (month != -1) {
            char yearChar = contract.charAt(3);
            if (isDigit(yearChar)) {
                String prefix = codeToShortnames.get(contract.substring(0, 2));
                if (prefix != null) {
                    int year = yearChar - '0';
                    int shortnameYear = currentYearDecade + year;
                    if (shortnameYear > (currentYear + 3)) {
                        shortnameYear -= 10;
                    }
                    return Optional.of(prefix + '-' + (month + 1) + '.' + shortnameYear % 100);
                }
            }
        }
        return Optional.empty();
    }

    /**
     * @param optionCode option's moex secid in {@code Si65000BC9}, {@code Si65000BC9D}, {@code RI180000BD1} or
     *                   {@code RI180000BD1A} format
     * @return futures contract secid (for ex. {@code SiH9}) if it can be calculated, empty optional otherwise
     */
    public Optional<String> getOptionUnderlingFutures(String optionCode) {
        int length = optionCode.length();
        if (length > 5) {
            int yearIdx = length;
            boolean hasYearDigit = isDigit(optionCode.charAt(--yearIdx)) || isDigit(optionCode.charAt(--yearIdx));
            if (hasYearDigit) {
                int monthIdx = yearIdx - 1;
                int month = getOptionMonth(optionCode.charAt(monthIdx));
                if (month != -1 && isValidOptionTypeAndStrike(optionCode, monthIdx)) {
                    String prefix = optionCode.substring(0, 2);
                    if (shortnameToCodes.containsValue(prefix)) {
                        char year = optionCode.charAt(yearIdx);
                        return Optional.of(prefix + futuresMonthCodes[month] + year);
                    }
                }
            }
        }
        return Optional.empty();
    }

    private static boolean isValidOptionTypeAndStrike(String code, int monthIdx) {
        int typeIdx = monthIdx - 1;
        char type = code.charAt(typeIdx);
        int strikeIdx = typeIdx - 1;
        if ((type == 'B' || type == 'A') && strikeIdx > 1) {
            for (; strikeIdx > 2; strikeIdx--) {
                if (!isDigit(code.charAt(strikeIdx))) {
                    return false;
                }
            }
            char signCharOrDigit = code.charAt(3);
            return isDigit(signCharOrDigit) || (signCharOrDigit == '-');
        }
        return false;
    }

    private int getFuturesMonth(char monthChar) {
        int month = 0;
        for (char e : futuresMonthCodes) {
            if (e == monthChar) {
                return month;
            }
            month++;
        }
        return -1;
    }

    /**
     * @return option month (0-based) or -1 if not found
     */
    private int getOptionMonth(char monthChar) {
        int month = monthChar - callOptionMonthCodes[0];
        if (month >= 0 && month <= 11) {
            return month;
        }
        month = monthChar - putOptionMonthCodes[0];
        if (month >= 0 && month <= 11) {
            return month;
        }
        return -1;
    }
}
