/*
 * InvestBook
 * Copyright (C) 2022  Spacious Team <spacious-team@ya.ru>
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

import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Stream;

import static java.lang.Character.isAlphabetic;
import static java.lang.Character.isDigit;
import static java.lang.Integer.parseInt;
import static java.util.Optional.empty;
import static java.util.stream.Collectors.toMap;

/**
 * Converts derivative short names (for ex. Si-6.21) to secid (a.k.a ticker codes, for ex SiM1)
 *
 * @see <a href="https://fs.moex.com/f/17282/onepager-dlja-klientov-opciony-na-akcii.pdf">Опционы на акции</a>
 * @see <a href="https://www.moex.com/s205">Specifications ticker codes for Futures and Options</a>
 */
@Slf4j
@Component
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
            {"RB", "RGBI"}, // Индекс RGBI

            {"AF", "AFLT"}, // ПАО "Аэрофлот" (о.а.)
            {"AL", "ALRS"}, // АК "АЛРОСА" (ПАО) (о.а.)
            {"CH", "CHMF"}, // ПАО "Северсталь" (о.а.)
            {"FS", "FEES"}, // ПАО "ФСК ЕЭС" (о.а.)
            {"GZ", "GAZR"}, // ПАО "Газпром" (о.а.) - для фьючерса и маржируемого опциона
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
            {"PZ", "PLZL"}, // ПАО "Полюс" (о.а.)
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
            {"NA", "NASD"}, // Invesco QQQ ETF Trust Unit Series 1
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
            {"PS", "POSI"}, // ПАО Группа Позитив
            {"SX", "STOX"}, // iShares Core EURO STOXX 50 UCITS ETF EUR (Dist)
            {"HS", "HANG"}, // Tracker Fund of Hong Kong ETF
            {"DX", "DAX"},  // iShares Core DAX UCITS ETF (DE)
            {"N2", "NIKK"}, // iShares Core Nikkei 225 ETF
            {"IS", "ISKJ"}, // ПАО "ИСКЧ"
            {"WU", "WUSH"}, // ПАО "ВУШ Холдинг"
            {"MV", "MVID"}, // ПАО "М.видео"
            {"CM", "CBOM"}, // ПАО "М.видео"
            {"SZ", "SGZH"}, // ПАО "Сегежа Групп"
            {"BE", "BELU"}, // ПАО "Белуга Групп"
            {"FL", "FLOT"}, // ПАО "Совкомфлот"

            {"RR", "RUON"}, // ставка RUONIA
            {"MF", "1MFR"}, // ставка RUSFAR
            {"DF", "1MDR"}, // Ставка RUSFARUSD

            {"CY", "CY"},   // курс китайский юань – российский рубль (старый)
            {"CR", "CNY"},  // курс китайский юань – российский рубль (актуальный)
            {"Eu", "Eu"},   // курс евро – российский рубль
            {"Si", "Si"},   // курс доллар США – российский рубль
            {"USDRUBF", "USDRUBF"},   // курс доллара США - российский рубль
            {"EURRUBF", "EURRUBF"},   // курс евро - российский рубль
            {"CNYRUBF", "CNYRUBF"},   // курс китайский юань – российский рубль
            {"TY", "TRY"},  // курс турецкая лира – российский рубль
            {"HK", "HKD"},  // курс гонконгский доллар – российский рубль
            {"AE", "AED"},  // курс дирхам ОАЭ – российский рубль
            {"I2", "INR"},  // курс индийская рупия – российский рубль
            {"KZ", "KZT"},  // курс казахстанский тенге – российский рубль
            {"AR", "AMD"},  // курс армянский драм – российский рубль
            {"ED", "ED"},   // курс евро – доллар США
            {"AU", "AUDU"}, // курс австралийский доллар – доллар США
            {"GU", "GBPU"}, // курс фунт стерлингов – доллар США
            {"CA", "UCAD"}, // курс доллар США – канадский доллар
            {"CF", "UCHF"}, // курс доллар США – швейцарский франк
            {"JP", "UJPY"}, // курс доллар США – японская йена
            {"TR", "UTRY"}, // курс доллар США – турецкая лира
            {"UC", "UCNY"}, // курс доллар США – китайский юань
            {"IN", "UINR"}, // курс доллара США к индийской рупии
            {"UU", "UUAH"}, // курс доллар США – украинская гривна
            {"EG", "EGBP"}, // курс евро – фунт стерлингов
            {"EC", "ECAD"}, // курс евро – канадский доллар
            {"EJ", "EJPY"}, // курс евро – японская йена

            {"BR", "BR"},   // нефть BRENT
            {"CU", "CU"},   // медь
            {"GD", "GOLD"}, // золото
            {"GL", "GL"},   // Золото (в рублях)
            {"GLDRUBF", "GLDRUBF"}, // золото
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
            {"WH", "WH4"},  // пшеница
            {"W4", "WHEAT"} // Индекс пшеницы
    }).collect(toMap(a -> a[0], a -> a[1]));

    private final Map<String, String> shortnameToCodes;

    private final Character[] futuresMonthCodes =
            new Character[]{'F', 'G', 'H', 'J', 'K', 'M', 'N', 'Q', 'U', 'V', 'X', 'Z'};
    private final Character[] callOptionMonthCodes =
            new Character[]{'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L'};
    private final Character[] putOptionMonthCodes =
            new Character[]{'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X'};

    private final int currentYear = LocalDate.now().getYear();
    // 2000-th, 2010-th, 2020-th and so on
    private final int currentYearDecade = currentYear / 10 * 10;

    private MoexDerivativeCodeService() {
        this.shortnameToCodes = codeToShortnames.entrySet()
                .stream()
                .collect(toMap(
                        Entry::getValue,
                        Entry::getKey,
                        (e1, e2) -> {
                            throw new RuntimeException();
                        },
                        HashMap::new));
        this.shortnameToCodes.put("GAZP", "GZ");  // ПАО "Газпром" (о.а.) - для опциона на акции
    }

    public boolean isDerivative(String contract) {
        return isFutures(contract) || isOption(contract);
    }

    /**
     * @return true for futures contract (in {@code Si-6.21} or {@code SiM1} format)
     */
    public boolean isFutures(String contract) {
        return isFuturesCode(contract) || isFuturesShortname(contract);
    }

    /**
     * @return true for futures in format {@code SiM1}
     */
    public boolean isFuturesCode(@Nullable String contract) {
        return contract != null &&
                contract.length() == 4 &&
                hasOnlyLetters(contract, 0, 2) && // smells like a futures code
                getFuturesMonth(contract.charAt(2)) != -1 &&
                isDigit(contract.charAt(3));
    }

    /**
     * @return true for futures in format {@code Si-3.21}
     */
    public boolean isFuturesShortname(@Nullable String contract) {
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
            if (isYearSymbolsDigit && hasOnlyLetters(contract, 0, dashIdx)) {  // smells like a futures code
                int month = parseInt(contract.substring(dashIdx + 1, dotIdx));
                return month >= 1 && month <= 12;
            }
        } catch (Exception ignore) {
        }
        return false;
    }

    private boolean hasOnlyLetters(String string, @SuppressWarnings("SameParameterValue") int beginIndex, int endIndex) {
        int i = Math.max(0, beginIndex);
        i = Math.min(string.length(), i);
        int cnt = Math.max(0, endIndex);
        cnt = Math.min(string.length(), cnt);
        for (; i < cnt; i++) {
            int c = string.charAt(i);
            if (!isAlphabetic(c)) {
                return false;
            }
        }
        return true;
    }

    /**
     * @return true for options (in {@code Si65000BC9}, {@code Si65000BC9D}, {@code Si-6.19M280319CA65000}
     * or {@code BR-7.16M270616CA 50} format)
     */
    public boolean isOption(String contract) {
        return isOptionCode(contract) || isOptionShortname(contract);
    }

    /**
     * @return true for option in format {@code BR10BF0}, {@code BR-10BF0} (знак "-" это часть цены) and {@code GZ300CG2D}
     */
    public boolean isOptionCode(@Nullable String contract) {
        if (contract == null) return false;
        int length = contract.length();
        if (length > 5) {
            int yearIdx = length;
            boolean hasYearDigit = isDigit(contract.charAt(--yearIdx)) || isDigit(contract.charAt(--yearIdx));
            if (hasYearDigit) {
                int monthIdx = yearIdx - 1;
                int month = getOptionMonth(contract.charAt(monthIdx));
                if (month != -1 && isValidOptionTypeAndStrike(contract, monthIdx)) {
                    return hasOnlyLetters(contract, 0, 2);  // smells like a option code
                }
            }
        }
        return false;
    }

    /**
     * @return true for option in format {@code BR-7.20M250620СA10}, {@code BR-7.20M250620СA-10},
     * {@code BR-7.16M270616CA 50} and {@code GAZPP220722CE 300}
     */
    public boolean isOptionShortname(@Nullable String contract) {
        if (contract == null) return false;
        int EIdx = getOptionExpirationTypeCharPosition(contract);
        if (EIdx == -1) {
            return false;
        }
        int AIdx = getOptionAccountTypeCharPosition(contract, EIdx);
        if (AIdx == -1) {
            return false;
        }
        char optionType = contract.charAt(EIdx - 1);  // CALL or PUT
        if (optionType == 'C' || optionType == 'P') {
            for (int i = AIdx + 1, cnt = AIdx + 7; i < cnt; i++) {
                if (!isDigit(contract.charAt(i))) {
                    return false;  // date expected
                }
            }
            char signCharOrDigit = contract.charAt(EIdx + 1);
            if (isDigit(signCharOrDigit) || signCharOrDigit == '-' || signCharOrDigit == ' ') {
                for (int i = EIdx + 2, cnt = contract.length(); i < cnt; i++) {
                    if (!isDigit(contract.charAt(i))) {
                        return false;  // price expected
                    }
                }
                return hasOnlyLetters(contract, 0, AIdx) || // похоже на код акции (значит это опцион на акции)
                        isFuturesShortname(contract.substring(0, AIdx));
            }
        }
        return false;
    }

    /**
     * Finds last 'A' or 'E' char in option short name ({@code BR-7.20M250620СA10}, {@code BR-7.20M250620СA-10},
     * {@code GAZPP220722CE 300})
     */
    private int getOptionExpirationTypeCharPosition(String contract) {
        int length = contract.length();
        for (int i = length - 1; i >= 0; i--) {
            char c = contract.charAt(i);
            if (c == 'A' || c == 'E') {  // тип экспирации: американский или европейский
                return i;
            } else if (!isDigit(c) && c != '-' && c != ' ') {
                return -1;
            }
        }
        return -1;
    }

    /**
     * Finds 'M' or 'P' char in option short name ({@code BR-7.20M250620СA10}, {@code BR-7.20M250620СA-10},
     * {@code GAZPP220722CE 300})
     */
    private int getOptionAccountTypeCharPosition(String contract, int expirationCharPosition) {
        for (int i = expirationCharPosition - 2; i >= 0; i--) {
            char c = contract.charAt(i);
            if (c == 'M' || c == 'P') {  // тип расчетов: маржируемый или премиальный
                return i;
            } else if (!isDigit(c)) {
                return -1;
            }
        }
        return -1;
    }

    /**
     * @return {@code SiM1} for futures contract in {@code Si-6.21} or {@code SiM1} format
     */
    public Optional<String> getFuturesCode(@Nullable String contract) {
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
    public Optional<String> getFuturesShortname(@Nullable String contract) {
        if (contract == null) {
            return Optional.empty();
        } else if (isFuturesShortname(contract)) {
            return Optional.of(contract);
        } else if (contract.length() != 4) {
            return empty();
        }
        int month = getFuturesMonth(contract.charAt(2));
        if (month != -1) {
            char yearChar = contract.charAt(3);
            if (isDigit(yearChar)) {
                @Nullable String prefix = codeToShortnames.get(contract.substring(0, 2));
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
     * Convert derivative codes before storing to DB if you need
     */
    public @Nullable String convertDerivativeCode(@Nullable String code) {
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
        if ((type == 'C' || type == 'B' || type == 'A') && strikeIdx > 1) {
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
