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

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.Character.isDigit;
import static java.lang.Integer.parseInt;
import static java.util.Optional.empty;

/**
 * Converts derivative short names (for ex. Si-6.21) to secid (a.k.a ticker codes, for ex SiM1)
 *
 * @see <a href="https://www.moex.com/s205">Specifications ticker codes for Futures and Options</a>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MoexDerivativeCodeService {

    private final Map<String, String> codeToShortnames = Stream.of(new String[][]{
            {"MX", "MIX"},  // Индекс МосБиржи
            {"MM", "MXI"},  // Индекс МосБиржи (мини)
            {"RI", "RTS"},  // Индекс РТС
            {"RM", "RTSM"}, // Индекс РТС (мини)
            {"RS", "RTSS"}, // Индекс голубых фишек
            {"4B", "ALSI"}, // Индекс FTSE/JSE Top40
            {"VI", "RVI"},  // Волатильность российского рынка
            {"HO", "HOME"}, // Индекс московской недвижимости ДомКлик
            {"OG", "OGI"},  // Индекс МосБиржи нефти и газа
            {"MA", "MMI"},  // Индекс МосБиржи металлов и добычи
            {"FN", "FNI"},  // Индекс МосБиржи финансов
            {"CS", "CNI"},  // Индекс МосБиржи потребительского сектора
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
            {"PI", "PIKK"}, // ПИК СЗ (о.а.)
            {"SE", "SPBE"}, // ПАО "СПБ Биржа"
            {"RL", "RUAL"}, // МКПАО "Объединённая Компания "РУСАЛ"
            {"PH", "PHOR"}, // ПАО "ФосАгро"
            {"DY", "DSKY"}, // ПАО "Детский мир"
            {"SS", "SMLT"}, // ПАО "Группа компаний "Самолет"
            {"MC", "MTLR"}, // ПАО "Мечел"
            {"RE", "RSTI"}, // ПАО "Российские сети"
            {"SO", "SIBN"}, // ПАО "Газпром нефть"
            {"TI", "TCSI"}, // ГДР ТиСиЭс Груп Холдинг ПиЭлСи
            {"FV", "FIVE"}, // ГДР Икс 5 Ритейл Груп Н.В
            {"ML", "MAIL"}, // ГДР Мэйл.ру Груп Лимитед
            {"OZ", "OZON"}, // АДР Озон Холдингс Пи Эл Си
            {"BW", "GBMW"}, // BMW AG (о.а.)
            {"BI", "BIDU"}, // АДР Байду Инк
            {"BA", "BABA"}, // АДР Алибаба Груп Холдинг Лимитед
            {"SF", "SPYF"}, // SPDR S&P 500 ETF Trust
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
            {"IN", "UINR"}, // курс доллара США к индийской рупии
            {"UU", "UUAH"}, // курс доллар США – украинская гривна
            {"EG", "EGBP"}, // курс евро – фунт стерлингов
            {"EC", "ECAD"}, // курс евро – канадский доллар
            {"EJ", "EJPY"}, // курс евро – японская йена
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
     * @return true for futures contract (in {@code Si-6.21} or {@code SiM1} format)
     */
    public boolean isFutures(String contract) {
        return isFuturesCode(contract) || isFuturesShortname(contract);
    }

    /**
     * @return true for futures in format {@code SiM1}
     */
    public boolean isFuturesCode(String contract) {
        return contract != null &&
                contract.length() == 4 &&
                shortnameToCodes.containsValue(contract.substring(0, 2)) &&
                getFuturesMonth(contract.charAt(2)) != -1 &&
                isDigit(contract.charAt(3));
    }

    /**
     * @return true for futures in format {@code Si-3.21}
     */
    public boolean isFuturesShortname(String contract) {
        try {
            if (contract == null) return false;
            int dotIdx = contract.length() - 3;
            if (contract.charAt(dotIdx) != '.') {
                return false;
            }
            int dashIdx = contract.indexOf('-');
            if (dashIdx == -1) {
                return false;
            }
            boolean isYearSymbolsDigit = isDigit(contract.charAt(dotIdx + 1)) &&
                    isDigit(contract.charAt(dotIdx + 2));
            if (isYearSymbolsDigit && shortnameToCodes.containsKey(contract.substring(0, dashIdx))) {
                int month = parseInt(contract.substring(dashIdx + 1, dotIdx));
                return month >= 1 && month <= 12;
            }
        } catch (Exception ignore) {
        }
        return false;
    }

    /**
     * @return true for options (in {@code Si65000BC9}, {@code Si65000BC9D}, {@code Si-6.19M280319CA65000}
     * or {@code BR-7.16M270616CA 50} format)
     */
    public boolean isOption(String contract) {
        return isOptionCode(contract) || isOptionShortname(contract);
    }

    /**
     * @return true for option in format {@code BR10BF0} and {@code BR-10BF0}
     */
    public boolean isOptionCode(String contract) {
        if (contract == null) return false;
        int length = contract.length();
        if (length > 5) {
            int yearIdx = length;
            boolean hasYearDigit = isDigit(contract.charAt(--yearIdx)) || isDigit(contract.charAt(--yearIdx));
            if (hasYearDigit) {
                int monthIdx = yearIdx - 1;
                int month = getOptionMonth(contract.charAt(monthIdx));
                if (month != -1 && isValidOptionTypeAndStrike(contract, monthIdx)) {
                    String prefix = contract.substring(0, 2);
                    return shortnameToCodes.containsValue(prefix);
                }
            }
        }
        return false;
    }

    /**
     * @return true for option in format {@code BR-7.20M250620СA10}, {@code BR-7.20M250620СA-10}
     * and {@code BR-7.16M270616CA 50}
     */
    public boolean isOptionShortname(String contract) {
        if (contract == null) return false;
        int dashIdx = contract.indexOf('-');
        if (dashIdx == -1) {
            return false;
        }
        int MIdx = contract.indexOf('M');
        int AIdx = contract.indexOf('A');
        if (MIdx == -1 || AIdx == -1) {
            return false;
        }
        char optionType = contract.charAt(AIdx - 1);
        if (optionType == 'C' || optionType == 'P') {
            for (int i = MIdx + 1, cnt = MIdx + 7; i < cnt; i++) {
                if (!isDigit(contract.charAt(i))) {
                    return false;
                }
            }
            char signCharOrDigit = contract.charAt(AIdx + 1);
            if (isDigit(signCharOrDigit) || signCharOrDigit == '-' || signCharOrDigit == ' ') {
                for (int i = AIdx + 2, cnt = contract.length(); i < cnt; i++) {
                    if (!isDigit(contract.charAt(i))) {
                        return false;
                    }
                }
                return isFuturesShortname(contract.substring(0, MIdx));
            }
        }
        return false;
    }

    /**
     * @return {@code SiM1} for futures contract in {@code Si-6.21} or {@code SiM1} format
     */
    public Optional<String> getFuturesCode(String contract) {
        try {
            if (contract == null) return empty();
            int dashIdx = contract.indexOf('-');
            if (dashIdx == -1) {
                return isFuturesCode(contract) ? Optional.of(contract) : empty();
            }
            int dotIdx = contract.length() - 3;
            if (contract.charAt(dotIdx) != '.') {
                return empty();
            }
            int month = parseInt(contract.substring(dashIdx + 1, dotIdx));
            int year = parseInt(contract.substring(dotIdx + 1));
            return Optional.ofNullable(shortnameToCodes.get(contract.substring(0, dashIdx)))
                    .map(prefix -> prefix + futuresMonthCodes[month - 1] + (year % 10));
        } catch (Exception e) {
            return empty();
        }
    }

    /**
     * @return {@code Si-6.21} for futures contract in {@code Si-6.21} or {@code SiM1} format
     */
    public Optional<String> getFuturesShortname(String contract) {
        if (isFuturesShortname(contract)) {
            return Optional.of(contract);
        } else if (contract.length() != 4) {
            return empty();
        }
        int month = getFuturesMonth(contract.charAt(2));
        if (month != -1) {
            char yearChar = contract.charAt(3);
            if (isDigit(yearChar)) {
                String prefix = codeToShortnames.get(contract.substring(0, 2));
                if (prefix != null) {
                    int year = getShortnameYear(yearChar);
                    if (year != -1) {
                        return Optional.of(prefix + '-' + (month + 1) + '.' + year);
                    }
                }
            }
        }
        return empty();
    }

    /**
     * Convert derivative codes before storing to DB if need
     */
    public String convertDerivativeCode(String code) {
        return isFuturesCode(code) ?
                getFuturesShortname(code).orElse(code) :
                code;
    }

    /**
     * Return contracts group.
     * For example for {@code MXI-6.21}, {@code MMM1}, {@code MXI-6.21M170621CA3000} and {@code MM3000BF1} returns "MM".
     * Returns empty optional if argument is not futures or optional.
     */
    public Optional<String> getContractGroup(String contract) {
        if (isFuturesShortname(contract) || isOptionShortname(contract)) {
            String shortname = contract.substring(0, contract.indexOf('-'));
            return Optional.ofNullable(shortnameToCodes.get(shortname));
        } else if (isFuturesCode(contract) || isOptionCode(contract)) {
            return Optional.of(contract.substring(0, 2));
        }
        return empty();
    }

    public Optional<String> codePrefixToShortnamePrefix(String codePrefix) {
        return Optional.ofNullable(codeToShortnames.get(codePrefix));
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

    /**
     * @param yearChar char containing year last digit, '1' for 2021
     * @return 2-digit code like 21, 22 and so on or -1 for invalid yearChar
     */
    private int getShortnameYear(char yearChar) {
        int year = yearChar - '0';
        if (year < 0 || year > 9) {
            return -1;
        }
        int shortnameYear = currentYearDecade + year;
        if (shortnameYear > (currentYear + 3)) {
            shortnameYear -= 10;
        }
        shortnameYear = shortnameYear % 100;
        return shortnameYear;
    }

    private int getFuturesMonth(char monthChar) {
        for (int month = 0; month < futuresMonthCodes.length; month++) {
            if (futuresMonthCodes[month] == monthChar) {
                return month;
            }
        }
        return -1;
    }

    /**
     * @return option month (0-based) or -1 if not found
     */
    private int getOptionMonth(char monthChar) {
        int month = getCallOptionMonth(monthChar);
        if (month == -1) {
            month = getPutOptionMonth(monthChar);
        }
        return month;
    }

    /**
     * @return CALL option month (0-based) or -1 if not found
     */
    private int getCallOptionMonth(char monthChar) {
        int month = monthChar - callOptionMonthCodes[0];
        if (month >= 0 && month <= 11) {
            return month;
        }
        return -1;
    }

    /**
     * @return PUT option month (0-based) or -1 if not found
     */
    private int getPutOptionMonth(char monthChar) {
        int month = monthChar - putOptionMonthCodes[0];
        if (month >= 0 && month <= 11) {
            return month;
        }
        return -1;
    }
}
