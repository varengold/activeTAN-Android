/*
 * Copyright (c) 2019-2020 EFDIS AG Bankensoftware, Freising <info@efdis.de>.
 *
 * This file is part of the activeTAN app for Android.
 *
 * The activeTAN app is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * The activeTAN app is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with the activeTAN app.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.efdis.tangenerator.activetan;

import java.util.Map;
import java.util.TreeMap;

public enum DataElementType {
    ADDRESS(10, "Adresse:",
            Format.ALPHANUMERIC, 36),
    QUOTE(11, "Angebots-Nr:",
            Format.ALPHANUMERIC, 12),
    QUANTITY(12, "Anzahl:",
            Format.NUMERIC, 12),
    ORDER_ID(13, "Auftrags-ID:",
            Format.ALPHANUMERIC, 12),
    AUTH_TOKEN(14, "Aut.Merkmal:",
            Format.ALPHANUMERIC, 12),
    BANK_DATA(15, "Bankdaten:",
            Format.ALPHANUMERIC, 12),
    AMOUNT(16, "Betrag:",
            Format.NUMERIC, 12, 2),
    BIC_RECIPIENT(17, "BIC Empf.:",
            Format.ALPHANUMERIC, 12),
    BANK_CODE_SENDER(18, "BLZ Abs.:",
            Format.NUMERIC, 12),
    BANK_CODE_RECIPIENT(19, "BLZ Empf.:",
            Format.NUMERIC, 12),
    BANK_CODE_CARD(20, "BLZ Karte:",
            Format.NUMERIC, 12),
    BANK_CODE_PAYER(21, "BLZ Zahler:",
            Format.NUMERIC, 12),
    BANK_CODE_OWN(22, "Eigene BLZ:",
            Format.NUMERIC, 12),
    IBAN_OWN(23, "Eigene IBAN:",
            Format.ALPHANUMERIC, 36),
    ACCOUNT_NUMBER_OWN(24, "Eigenes Kto:",
            Format.NUMERIC, 12),
    MERCHANT(26, "Händlername:",
            Format.ALPHANUMERIC, 36),
    IBAN_SENDER(29, "IBAN Abs.:",
            Format.ALPHANUMERIC, 36),
    IBAN_RECIPIENT(32, "IBAN Empf.:",
            Format.ALPHANUMERIC, 36),
    IBAN_PAYER(33, "IBAN Zahler", // sic!
            Format.ALPHANUMERIC, 36),
    ISIN(36, "ISIN:",
            Format.ALPHANUMERIC, 12),
    CARD_NUMBER(37, "Kartennummer",
            Format.ALPHANUMERIC, 12),
    ACCOUNT_NUMBER_SENDER(38, "Konto Abs.:",
            Format.NUMERIC, 12),
    ACCOUNT_NUMBER_RECIPIENT(39, "Konto Empf.:",
            Format.NUMERIC, 12),
    ACCOUNT_NUMBER_PAYER(40, "Konto Zahler",
            Format.NUMERIC, 12),
    CREDIT_CARD(41, "Kreditkarte:",
            Format.NUMERIC, 12),
    LIMIT(42, "Limit:",
            Format.NUMERIC, 12, 2),
    VOLUME(43, "Menge:",
            Format.NUMERIC, 12, 3),
    MOBILE_PHONE(44, "Mobilfunknr:",
            Format.ALPHANUMERIC, 17),
    NAME_RECIPIENT(45, "Name Empf.:",
            Format.ALPHANUMERIC, 12),
    POST_CODE(46, "Postleitzahl",
            Format.ALPHANUMERIC, 12),
    RATE(47, "Rate:",
            Format.NUMERIC, 12, 2),
    REFERENCE_ACCOUNT_NUMBER(48, "Referenzkto:",
            Format.NUMERIC, 12),
    REFERENCE_NUMBER(49, "Referenzzahl",
            Format.ALPHANUMERIC, 36),
    PIECES(50, "Stücke/Nom.:",
            Format.NUMERIC, 12, 3),
    TAN_MEDIA(51, "TAN-Medium", // sic!
            Format.ALPHANUMERIC, 12),
    /* According to chipTAN specification DE52 must be numeric.
     * However, the date is formatted by the backend. Disable numeric formatting locally.
     */
    DATE(52, "Termin:",
            Format.ALPHANUMERIC, 12),
    CONTRACT(53, "Vertrag.Kenn",
            Format.ALPHANUMERIC, 12),
    WKN(54, "WP-Kenn-Nr:",
            Format.ALPHANUMERIC, 12),
    ACCOUNT_SENDER(55, "Konto Abs.:",
            Format.ALPHANUMERIC, 12),
    ACCOUNT_RECIPIENT(56, "Konto Empf.:",
            Format.ALPHANUMERIC, 12),
    ACCOUNT_PAYER(57, "Konto Zahler",
            Format.ALPHANUMERIC, 12),
    CURRENCY(58, "Währung:",
            Format.ALPHANUMERIC, 3),
    ORDER_TYPE(61, "Auftragsart:",
            Format.ALPHANUMERIC, 12),
    ASSETS(62, "Anz.Posten:", // sic!
            Format.NUMERIC, 12),
    BANK_RECIPIENT(63, "Bank Empf.:",
            Format.ALPHANUMERIC, 36),
    PRODUCT(64, "Produkt:",
            Format.ALPHANUMERIC, 36),
    ;

    public enum Format { ALPHANUMERIC, NUMERIC }

    private final int id;
    private final String visDataLine1;
    private final Format format;
    private final int maxLength;
    private final int integerDigits;
    private final int fractionDigits;

    DataElementType(int id, String visDataLine1, Format format, int maxLength) {
        this(id, visDataLine1, format, maxLength, 0);
    }

    DataElementType(int id, String visDataLine1, Format format, int maxLength, int fractionDigits) {
        this.id = id;
        this.visDataLine1 = visDataLine1;
        this.format = format;
        this.maxLength = maxLength;
        this.fractionDigits = fractionDigits;

        if (maxLength < 0) {
            throw new RuntimeException("illegal maximum length");
        }

        if (Format.NUMERIC.equals(format)) {
            if (fractionDigits == 0) {
                this.integerDigits = maxLength;
            } else {
                this.integerDigits = maxLength - 1 - fractionDigits; // -1 because of decimal point
                if (integerDigits < 0) {
                    throw new RuntimeException("fraction digits exceeds maximum length");
                }
            }
        } else {
            if (fractionDigits != 0) {
                throw new RuntimeException("only numeric format may use fraction digits");
            }
            this.integerDigits = 0;
        }

        if (Format.ALPHANUMERIC.equals(format) && fractionDigits != 0) {
            throw new RuntimeException("alphanumeric format cannot use fraction digits");
        }
    }

    public int getId() {
        return id;
    }

    public String getVisDataLine1() { return visDataLine1; }

    public Format getFormat() {
        return format;
    }

    public int getMaxLength() {
        return maxLength;
    }

    public int getIntegerDigits() {
        return integerDigits;
    }

    public int getFractionDigits() {
        return fractionDigits;
    }

    private static final Map<Integer, DataElementType> byId;
    static {
        byId = new TreeMap<>();
        for (DataElementType det : DataElementType.values()) {
            byId.put(det.getId(), det);
        }
    }

    public static DataElementType forId(int id) {
        return byId.get(id);
    }

}
