/*
 * Copyright (c) 2019 EFDIS AG Bankensoftware, Freising <info@efdis.de>.
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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public enum VisualisationClass {
    EMPTY(0, "Bankauftrag", "allgemein"),
    USER_AUTHENTICATION_01(1, "Legitimation", "Kunde",
            DataElementType.AUTH_TOKEN),
    CREDIT_TRANSFER_NATIONAL(4, "Überweisung", "Inland",
            DataElementType.ACCOUNT_NUMBER_RECIPIENT, DataElementType.BANK_CODE_RECIPIENT, DataElementType.AMOUNT),
    TRANSFER(5, "Umbuchung", "",
            DataElementType.ACCOUNT_NUMBER_RECIPIENT, DataElementType.AMOUNT),
    TRANSFER_SCHEDULED(6, "Umbuchung", "terminiert",
            DataElementType.ACCOUNT_NUMBER_RECIPIENT, DataElementType.AMOUNT, DataElementType.DATE),
    CREDIT_TRANSFER_REFERENCE_ACCOUNT_07(7, "Überweisung", "Referenzkto",
            DataElementType.REFERENCE_ACCOUNT_NUMBER, DataElementType.AMOUNT),
    CREDIT_TRANSFER_REFERENCE_ACCOUNT_08(8, "Überweisung", "Referenzkto",
            DataElementType.IBAN_RECIPIENT, DataElementType.AMOUNT),
    CREDIT_TRANSFER_SEPA(9, "Überweisung", "SEPA/EU",
            DataElementType.IBAN_RECIPIENT, DataElementType.AMOUNT),
    CREDIT_TRANSFER_FOREIGN_10(10, "Überweisung", "Ausland",
            DataElementType.ACCOUNT_NUMBER_RECIPIENT, DataElementType.AMOUNT),
    CREDIT_TRANSFER_FOREIGN_CHEQUE(11, "Überweisung", "Ausland",
            DataElementType.NAME_RECIPIENT, DataElementType.AMOUNT),
    COLLECTIVE_TRANSFER_NATIONAL(12, "Sammelüberw.", "Inland",
            DataElementType.ACCOUNT_NUMBER_OWN, DataElementType.AMOUNT, DataElementType.QUANTITY),
    COLLECTIVE_TRANSFER_SEPA(13, "Sammelüberw.", "SEPA",
            DataElementType.IBAN_OWN, DataElementType.AMOUNT, DataElementType.QUANTITY),
    COLLECTIVE_TRANSFER_FOREIGN(14, "Sammelüberw.", "Ausland",
            DataElementType.ACCOUNT_NUMBER_OWN, DataElementType.AMOUNT, DataElementType.QUANTITY),
    DIRECT_DEBIT_NATIONAL(15, "Lastschrift", "Inland",
            DataElementType.ACCOUNT_NUMBER_PAYER, DataElementType.BANK_CODE_PAYER, DataElementType.AMOUNT),
    DIRECT_DEBIT_RETURN_16(16, "Rückgabe", "Lastschrift",
            DataElementType.ACCOUNT_NUMBER_RECIPIENT, DataElementType.BANK_CODE_RECIPIENT, DataElementType.AMOUNT),
    DIRECT_DEBIT_SEPA(17, "Lastschrift", "SEPA",
            DataElementType.IBAN_PAYER, DataElementType.AMOUNT),
    DIRECT_DEBIT_FOREIGN(18, "Lastschrift", "Ausland",
            DataElementType.ACCOUNT_NUMBER_PAYER, DataElementType.AMOUNT),
    COLLECTIVE_DEBIT_NATIONAL(19, "Sammellasts.", "Inland",
            DataElementType.ACCOUNT_NUMBER_OWN, DataElementType.AMOUNT, DataElementType.QUANTITY),
    COLLECTIVE_DEBIT_SEPA(20, "Sammellasts.", "SEPA",
            DataElementType.IBAN_OWN, DataElementType.AMOUNT, DataElementType.QUANTITY),
    COLLECTIVE_DEBIT_FOREIGN(21, "Sammellasts.", "Ausland",
            DataElementType.ACCOUNT_NUMBER_OWN, DataElementType.AMOUNT, DataElementType.QUANTITY),
    CREDIT_TRANSFER_NATIONAL_SCHEDULED(22, "Terminüberw.", "Inland",
            DataElementType.ACCOUNT_NUMBER_RECIPIENT, DataElementType.BANK_CODE_RECIPIENT, DataElementType.AMOUNT),
    CREDIT_TRANSFER_SEPA_SCHEDULED(23, "Terminüberw.", "SEPA",
            DataElementType.IBAN_RECIPIENT, DataElementType.AMOUNT),
    CREDIT_TRANSFER_FOREIGN_SCHEDULED(24, "Terminüberw.", "Ausland",
            DataElementType.ACCOUNT_NUMBER_RECIPIENT, DataElementType.AMOUNT),
    COLLECTIVE_TRANSFER_NATIONAL_SCHEDULED(25, "Terminüberw.", "Sammel Inl.",
            DataElementType.ACCOUNT_NUMBER_OWN, DataElementType.AMOUNT, DataElementType.QUANTITY),
    COLLECTIVE_TRANSFER_SEPA_SCHEDULED(26, "Terminüberw.", "Sammel SEPA",
            DataElementType.IBAN_OWN, DataElementType.AMOUNT, DataElementType.QUANTITY),
    COLLECTIVE_TRANSFER_FOREIGN_SCHEDULED(27, "Terminüberw.", "Sammel Ausl.",
            DataElementType.ACCOUNT_NUMBER_OWN, DataElementType.AMOUNT, DataElementType.QUANTITY),
    DIRECT_DEBIT_NATIONAL_SCHEDULED(28, "Terminlasts.", "Inland",
            DataElementType.ACCOUNT_NUMBER_PAYER, DataElementType.BANK_CODE_PAYER, DataElementType.AMOUNT),
    DIRECT_DEBIT_SEPA_SCHEDULED(29, "Terminlasts.", "SEPA",
            DataElementType.IBAN_PAYER, DataElementType.AMOUNT),
    DIRECT_DEBIT_FOREIGN_SCHEDULED(30, "Terminlasts.", "Ausland",
            DataElementType.ACCOUNT_NUMBER_PAYER, DataElementType.AMOUNT),
    COLLECTIVE_DEBIT_NATIONAL_SCHEDULED(31, "Terminlasts.", "Sammel Inl.",
            DataElementType.ACCOUNT_NUMBER_OWN, DataElementType.AMOUNT, DataElementType.QUANTITY),
    COLLECTIVE_DEBIT_SEPA_SCHEDULED(32, "Terminlasts.", "Sammel SEPA",
            DataElementType.IBAN_OWN, DataElementType.AMOUNT, DataElementType.QUANTITY),
    COLLECTIVE_DEBIT_FOREIGN_SCHEDULED(33, "Terminlasts.", "Sammel Ausl.",
            DataElementType.ACCOUNT_NUMBER_OWN, DataElementType.AMOUNT, DataElementType.QUANTITY),
    STANDING_ORDER_NATIONAL(34, "Dauerüberw.", "Inland",
            DataElementType.ACCOUNT_RECIPIENT, DataElementType.BANK_CODE_RECIPIENT, DataElementType.AMOUNT),
    STANDING_ORDER_SEPA(35, "Dauerüberw.", "SEPA",
            DataElementType.IBAN_RECIPIENT, DataElementType.AMOUNT),
    STANDING_ORDER_FOREIGN(36, "Dauerüberw.", "Ausland",
            DataElementType.ACCOUNT_NUMBER_RECIPIENT, DataElementType.AMOUNT),
    STANDING_ORDER_DEBIT_NATIONAL(37, "Dauerlasts.", "Inland",
            DataElementType.ACCOUNT_NUMBER_PAYER, DataElementType.BANK_CODE_PAYER, DataElementType.AMOUNT),
    STANDING_ORDER_DEBIT_SEPA(38, "Dauerlasts.", "SEPA",
            DataElementType.IBAN_PAYER, DataElementType.AMOUNT),
    PORTFOLIO_RETRIEVAL(39, "Bestand", "abfragen"),
    DELETE_ORDER_40(40, "Löschen", "Auftrag",
            DataElementType.ACCOUNT_NUMBER_OWN, DataElementType.AMOUNT, DataElementType.ORDER_ID),
    STOP_ORDER_CREDIT_TRANSFER_41(41, "Aussetzen", "Auftrag",
            DataElementType.ACCOUNT_NUMBER_RECIPIENT, DataElementType.AMOUNT),
    STOP_ORDER_DIRECT_DEBIT_42(42, "Aussetzen", "Auftrag",
            DataElementType.ACCOUNT_NUMBER_PAYER, DataElementType.AMOUNT),
    UPDATE_ORDER_CREDIT_TRANSFER_43(43, "Ändern", "Auftrag",
            DataElementType.ACCOUNT_NUMBER_RECIPIENT, DataElementType.AMOUNT),
    UPDATE_ORDER_DEBIT_TRANSFER_44(44, "Ändern", "Auftrag",
            DataElementType.ACCOUNT_PAYER, DataElementType.AMOUNT),
    RELEASE_CREDIT_TRANSFERS_NATIONAL(45, "Freigabe", "Überw. DTAUS",
            DataElementType.ACCOUNT_NUMBER_OWN, DataElementType.AMOUNT, DataElementType.QUANTITY),
    RELEASE_DIRECT_DEBITS_NATIONAL(46, "Freigabe", "Lasts. DTAUS",
            DataElementType.ACCOUNT_NUMBER_OWN, DataElementType.AMOUNT, DataElementType.QUANTITY),
    RELEASE_CREDIT_TRANSFER_FOREIGN(47, "Freigabe", "Überw. DTAZV",
            DataElementType.ACCOUNT_NUMBER_OWN, DataElementType.AMOUNT, DataElementType.QUANTITY),
    RELEASE_CREDIT_TRANSFERS_SEPA(48, "Freigabe", "Überw. SEPA",
            DataElementType.IBAN_OWN, DataElementType.AMOUNT, DataElementType.QUANTITY),
    RELEASE_DIRECT_DEBITS_SEPA(49, "Freigabe", "Lasts. SEPA",
            DataElementType.IBAN_OWN, DataElementType.AMOUNT, DataElementType.QUANTITY),
    RELEASE_FILES(50, "Freigabe", "DSRZ-Dateien",
            DataElementType.ACCOUNT_NUMBER_OWN, DataElementType.AMOUNT, DataElementType.QUANTITY),
    ELECTRONIC_STATEMENT(51, "Kontoauszug", "u. Quittung",
            DataElementType.ACCOUNT_NUMBER_OWN),
    ELECTRONIC_STATEMENT_SUBSCRIBE(52, "Kontoauszug", "an/abmelden",
            DataElementType.ACCOUNT_NUMBER_OWN),
    ELECTRONIC_MAILBOX_SUBSCRIBE(53, "Postfach", "an/abmelden",
            DataElementType.ACCOUNT_NUMBER_OWN),
    ELECTRONIC_MAILBOX(54, "Postkorb", "",
            DataElementType.ACCOUNT_NUMBER_OWN),
    DATA_VAULT(55, "Datentresor", "",
            DataElementType.ACCOUNT_NUMBER_OWN),
    SECURITIES_BUY(56, "Wertpapier", "Kauf",
            DataElementType.ISIN, DataElementType.WKN, DataElementType.PIECES),
    SECURITIES_SELL(57, "Wertpapier", "Verkauf",
            DataElementType.ISIN, DataElementType.WKN, DataElementType.PIECES),
    SECURITIES_TRADE(58, "Wertpapier", "Geschäft",
            DataElementType.ISIN, DataElementType.WKN, DataElementType.PIECES),
    CONTRACT_ASSET(59, "Anlage", "Abschluss",
            DataElementType.QUOTE, DataElementType.AMOUNT, DataElementType.RATE),
    CONTRACT_LOAN(60, "Kredit", "Abschluss",
            DataElementType.QUOTE, DataElementType.AMOUNT, DataElementType.RATE),
    CONTRACT_PRODUCT(61, "Produkt", "Kauf",
            DataElementType.AMOUNT, DataElementType.RATE),
    CONTRACT_INSURANCE(62, "Versicherung", "Abschluss",
            DataElementType.QUOTE, DataElementType.AMOUNT, DataElementType.RATE),
    MASTER_DATA_MANAGEMENT(63, "Service", "Funktionen",
            DataElementType.POST_CODE),
    TAN_MANAGEMENT(64, "TAN-Medien", "Management",
            DataElementType.TAN_MEDIA, DataElementType.MOBILE_PHONE, DataElementType.CARD_NUMBER),
    CHARGE_MOBILE_PHONE(65, "Mobiltelefon", "laden",
            DataElementType.MOBILE_PHONE, DataElementType.AMOUNT),
    CHARGE_CHIP_CARD(66, "GeldKarte", "laden",
            DataElementType.CARD_NUMBER, DataElementType.AMOUNT, DataElementType.BANK_CODE_CARD),
    INTERNET_PAYMENT_67(67, "Zahlung", "Internet",
            DataElementType.MERCHANT, DataElementType.ACCOUNT_NUMBER_RECIPIENT, DataElementType.AMOUNT),
    INTERNET_MONEY_TRANSFER(68, "Geldtransfer", "Internet",
            DataElementType.MERCHANT, DataElementType.ACCOUNT_NUMBER_RECIPIENT, DataElementType.AMOUNT),
    EXCEMPTION_ORDER(69, "Freistellung", "",
            DataElementType.AMOUNT),
    ADDRESS_CHANGE_70(70, "Adresse", "ändern",
            DataElementType.ADDRESS, DataElementType.MOBILE_PHONE),
    ADDRESS_CHANGE_71(71, "Adresse", "ändern",
            DataElementType.POST_CODE),
    CREDIT_TRANSFER_FOREIGN_72(72, "Überweisung", "Ausland",
            DataElementType.IBAN_RECIPIENT, DataElementType.AMOUNT),
    CREDIT_TRANSFER_FOREIGN_73(73, "Überweisung", "Ausland",
            DataElementType.ACCOUNT_RECIPIENT, DataElementType.AMOUNT),
    DIRECT_DEBIT_RETURN_74(74, "Rückgabe", "Lastschrift",
            DataElementType.IBAN_OWN, DataElementType.AMOUNT),
    DELETE_ORDER_75(75, "Löschen", "Auftrag",
            DataElementType.IBAN_OWN, DataElementType.AMOUNT, DataElementType.ORDER_TYPE),
    DELETE_ORDER_76(76, "Löschen", "Auftrag",
            DataElementType.IBAN_OWN, DataElementType.AMOUNT, DataElementType.ORDER_TYPE),
    STOP_ORDER_CREDIT_TRANSFER_77(77, "Aussetzen", "Auftrag",
            DataElementType.IBAN_OWN, DataElementType.AMOUNT, DataElementType.ORDER_TYPE),
    STOP_ORDER_DIRECT_DEBIT_78(78, "Aussetzen", "Auftrag",
            DataElementType.IBAN_PAYER, DataElementType.AMOUNT, DataElementType.ORDER_TYPE),
    UPDATE_ORDER_CREDIT_TRANSFER_79(79, "Ändern", "Auftrag",
            DataElementType.IBAN_OWN, DataElementType.AMOUNT, DataElementType.ORDER_TYPE),
    UPDATE_ORDER_DEBIT_TRANSFER_80(80, "Ändern", "Auftrag",
            DataElementType.IBAN_PAYER, DataElementType.AMOUNT, DataElementType.ORDER_TYPE),
    INTERNET_PAYMENT_81(81, "Zahlung", "Internet",
            DataElementType.MERCHANT, DataElementType.AMOUNT, DataElementType.CURRENCY),
    ;

    private final int id;
    private final String visDataLine1;
    private final String visDataLine2;
    private final List<DataElementType> dataElements;

    VisualisationClass(int id, String visDataLine1, String visDataLine2, DataElementType... dataElements) {
        this.id = id;
        this.visDataLine1 = visDataLine1;
        this.visDataLine2 = visDataLine2;
        this.dataElements = Collections.unmodifiableList(Arrays.asList(dataElements));
    }

    public int getId() {
        return id;
    }

    public String getVisDataLine1() { return visDataLine1; }

    public String getVisDataLine2() { return visDataLine2; }

    public List<DataElementType> getDataElements() {
        return dataElements;
    }

    private static final Map<Integer, VisualisationClass> byId;
    static {
        byId = new TreeMap<>();
        for (VisualisationClass vc : VisualisationClass.values()) {
            byId.put(vc.getId(), vc);
        }
    }

    public static VisualisationClass forId(int id) {
        return byId.get(id);
    }

}
