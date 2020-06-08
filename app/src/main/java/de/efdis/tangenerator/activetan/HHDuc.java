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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.zip.Checksum;

public class HHDuc {

    /**
     * Legacy start codes have a maximum of 8 digits.
     */
    private static final int MAX_SHORT_START_CODE_DIGITS = 8;

    /**
     * Start codes with visualization class (prefix 1 and 2) have a maximum of 12 digits.
     */
    private static final int MAX_START_CODE_DIGITS = 12;

    private int unpredictableNumber;
    private final VisualisationClass visualisationClass;
    private final LinkedHashMap<DataElementType, String> dataElements = new LinkedHashMap<>();

    /**
     * Create a new, empty HHDuc object without visualization class.
     */
    public HHDuc() {
        this.visualisationClass = null;
    }

    /**
     * Create a new, empty HHDuc object for the specified visualisation class with the default
     * data elements of the visualisation class.
     */
    public HHDuc(VisualisationClass visualisationClass) {
        this.visualisationClass = visualisationClass;
        for (DataElementType dataElementType : visualisationClass.getDataElements()) {
            dataElements.put(dataElementType, "");
        }
    }

    /**
     * Create a new, empty HHDuc object for the specified visualisation class with
     * custom data elements.
     */
    public HHDuc(VisualisationClass visualisationClass, DataElementType... selectedElements) {
        this.visualisationClass = visualisationClass;
        for (DataElementType dataElementType : selectedElements) {
            dataElements.put(dataElementType, "");
        }
    }

    public VisualisationClass getVisualisationClass() {
        return visualisationClass;
    }

    public String getDataElement(DataElementType type) {
        return dataElements.get(type);
    }

    public void setDataElement(DataElementType type, String value) {
        if (!dataElements.containsKey(type)) {
            throw new NoSuchElementException(type + " is not available for this HHDuc");
        }

        if (type.getMaxLength() < value.length()) {
            String ellipsis = "...";
            value = value.substring(0, type.getMaxLength() - ellipsis.length())
                    + ellipsis;
        }

        dataElements.put(type, value);
    }

    public void setDataElement(DataElementType type, long value) {
        if (!DataElementType.Format.NUMERIC.equals(type.getFormat())) {
            throw new IllegalArgumentException(type + " is not numeric");
        }

        setDataElement(type, Long.toString(value));
    }

    public void setDataElement(DataElementType type, BigDecimal value) {
        if (!DataElementType.Format.NUMERIC.equals(type.getFormat())) {
            throw new IllegalArgumentException(type + " is not numeric");
        }

        DecimalFormat format = new DecimalFormat();
        format.setMaximumFractionDigits(type.getFractionDigits());
        format.setMaximumIntegerDigits(type.getIntegerDigits());
        DecimalFormatSymbols symbols = format.getDecimalFormatSymbols();
        symbols.setDecimalSeparator(',');
        format.setDecimalFormatSymbols(symbols);
        format.setMinimumFractionDigits(value.scale());
        format.setGroupingUsed(false);

        setDataElement(type, format.format(value));
    }

    public List<DataElementType> getDataElementTypes() {
        // The key set of a LinkedHashSet is ordered
        return new ArrayList<>(dataElements.keySet());
    }

    public int getUnpredictableNumber() {
        return unpredictableNumber;
    }

    public void setUnpredictableNumber(int unpredictableNumber) {
        if (unpredictableNumber < 0) {
            throw new IllegalArgumentException("Random number cannot be negative");
        }
        this.unpredictableNumber = unpredictableNumber;
    }

    public byte[] getStartCode() {
        StringBuilder startCode = new StringBuilder();
        int maxStartCodeDigits;

        if (getVisualisationClass() == null) {
            maxStartCodeDigits = MAX_SHORT_START_CODE_DIGITS;

            // Special case:
            // The only supported case for HHDuc w/o visualisation class is start code '08...'
            // for static TAN computation with display of the ATC.
            startCode.append("08");
        } else {
            maxStartCodeDigits = MAX_START_CODE_DIGITS;

            if (getVisualisationClass().getDataElements().equals(
                    new LinkedList<>(dataElements.keySet()))) {
                startCode.append("1");
                startCode.append(String.format(Locale.US,
                        "%02d", visualisationClass.getId()));
            } else {
                startCode.append("2");
                startCode.append(String.format(Locale.US,
                        "%02d", visualisationClass.getId()));

                for (DataElementType dataElementType : dataElements.keySet()) {
                    startCode.append(String.format(Locale.US,
                            "%02d", dataElementType.getId()));
                }
                if (dataElements.size() < 3) {
                    startCode.append("0");
                }
            }
        }

        String randomNumber = String.format(Locale.US,
                "%0" + maxStartCodeDigits + "d", unpredictableNumber);
        startCode.append(randomNumber.substring(
                randomNumber.length() + startCode.length() - maxStartCodeDigits));

        assert startCode.length() == maxStartCodeDigits;

        return FieldEncoding.bcdEncode(startCode.toString());
    }

    /** In Germany only one value is allowed according to HHDuc version 1.4 */
    private static final int HHD_CONTROL_BYTE = 0x01;

    public static HHDuc parse(byte[] rawBytes) throws UnsupportedDataFormatException {
        ByteArrayInputStream bais = new ByteArrayInputStream(rawBytes);

        // LC
        {
            int challengeLength = bais.read();
            if (challengeLength != bais.available()) {
                throw new UnsupportedDataFormatException("LC contains wrong value");
            }
        }

        // LS
        int startCodeLength;
        FieldEncoding startCodeFormat;
        {
            int lsByte = bais.read();
            if (lsByte < 0) {
                throw new UnsupportedDataFormatException("LS is missing");
            }

            boolean withControlByte = (lsByte & 0x80) != 0;
            startCodeFormat = (lsByte & 0x40) != 0 ? FieldEncoding.ASCII : FieldEncoding.BCD;
            startCodeLength = (lsByte & 0x3f);

            // the control byte has been introduced with HHDuc version 1.4
            if (!withControlByte) {
                throw new UnsupportedDataFormatException("Control byte missing according to LS");
            }
        }

        // Control
        int controlByte;
        {
            controlByte = bais.read();
            if (controlByte < 0) {
                throw new UnsupportedDataFormatException("Control is missing");
            }

            if (controlByte != HHD_CONTROL_BYTE) {
                throw new UnsupportedDataFormatException("Control has unknown value");
            }
        }

        // Start Code
        byte[] startCode;
        {
            if (bais.available() < startCodeLength) {
                throw new UnsupportedDataFormatException("Start code is missing");
            }

            startCode = new byte[startCodeLength];
            bais.read(startCode, 0, startCode.length);
        }

        // Data elements 1..3
        List<FieldEncoding> dataElementEncodings = new ArrayList<>(3);
        List<byte[]> dataElements = new ArrayList<>(3);
        while (bais.available() > 1) {
            int ldeByte = bais.read();

            FieldEncoding encoding = ((ldeByte & 0x40) != 0) ? FieldEncoding.ASCII : FieldEncoding.BCD;
            int length = ldeByte & 0x3f;

            if (bais.available() < length) {
                throw new UnsupportedDataFormatException(
                        "DE" + (dataElements.size() + 1) + " is incomplete");
            }

            if (length > 36 || (FieldEncoding.BCD.equals(encoding) && length > 18)) {
                throw new UnsupportedDataFormatException(
                        "DE" + (dataElements.size() + 1) + " exceeds the maximum length");
            }

            byte[] de = new byte[length];
            bais.read(de, 0, de.length);

            dataElementEncodings.add(encoding);
            dataElements.add(de);
        }

        // Check byte
        {
            if (bais.available() == 0) {
                throw new UnsupportedDataFormatException(
                        "Check byte is missing");
            }

            int checkByte = bais.read();

            Checksum luhnDigit = new LuhnChecksum();
            luhnDigit.update(controlByte);
            luhnDigit.update(startCode, 0, startCode.length);
            for (byte[] dataElement : dataElements) {
                luhnDigit.update(dataElement, 0, dataElement.length);
            }

            Checksum xor = new XorChecksum();
            xor.update(rawBytes, 0, rawBytes.length - 1);

            int computedCheckByte = (int) ((luhnDigit.getValue() << 4) | xor.getValue());

            if (checkByte != computedCheckByte) {
                throw new UnsupportedDataFormatException("Check byte is wrong");
            }
        }

        if (bais.available() > 0) {
            throw new UnsupportedDataFormatException(
                    "Unexpected data after check byte");
        }

        return parseApplicationData(startCodeFormat, startCode, dataElementEncodings, dataElements);
    }

    private static HHDuc parseApplicationData(FieldEncoding startCodeEncoding, byte[] rawStartCode, List<FieldEncoding> dataElementEncodings, List<byte[]> rawDataElements) throws UnsupportedDataFormatException {
        assert dataElementEncodings.size() == rawDataElements.size();

        long startCode;
        switch (startCodeEncoding) {
            case ASCII:
                try {
                    startCode = Integer.parseInt(new String(rawStartCode, DKCharset.INSTANCE));
                } catch (NumberFormatException e) {
                    throw new UnsupportedDataFormatException("Start code is not numeric");
                }
                break;

            case BCD:
                try {
                    startCode = FieldEncoding.bcdDecode(rawStartCode);
                } catch (NumberFormatException e) {
                    throw new UnsupportedDataFormatException("Illegal start code format");
                }
                break;

            default:
                throw new UnsupportedDataFormatException("Unsupported start code encoding");
        }

        final HHDuc hhduc;
        if (8_000_000L <= startCode && startCode <= 8_999_999L) {
            // Start code prefix 08: No visualization class
            hhduc = new HHDuc();
            hhduc.setUnpredictableNumber((int) (startCode % 1_000_000L));
        } else {
            if (startCode < 100_000_000_000L || startCode > 299_999_999_999L) {
                throw new UnsupportedDataFormatException(
                        "Only start codes with length 12 and prefix 1 or 2 are supported");
            }

            int vc = (int) (startCode / 1_000_000_000L) % 100;
            VisualisationClass visualisationClass = VisualisationClass.forId(vc);
            if (visualisationClass == null) {
                throw new UnsupportedDataFormatException("Visualisation class " + vc + " unknown");
            }

            if (startCode < 200_000_000_000L) {
                hhduc = new HHDuc(visualisationClass);
                hhduc.setUnpredictableNumber((int) (startCode % 1_000_000_000L));
            } else {
                List<DataElementType> dataElements = new ArrayList<>(3);

                int unpredictableNumber;

                int p = (int) ((startCode / 10_000_000) % 100);
                int s = (int) ((startCode / 100_000) % 100);
                int t = (int) ((startCode / 1000) % 100);
                if (p >= 10) {
                    dataElements.add(DataElementType.forId(p));
                    if (s >= 10) {
                        dataElements.add(DataElementType.forId(s));
                        if (t >= 10) {
                            dataElements.add(DataElementType.forId(t));
                            unpredictableNumber = (int) (startCode % 1000L);
                        } else {
                            unpredictableNumber = (int) (startCode % 10_000L);
                        }
                    } else {
                        unpredictableNumber = (int) (startCode % 1_000_000L);
                    }
                } else {
                    unpredictableNumber = (int) (startCode % 100_000_000L);
                }

                for (DataElementType dataElement : dataElements) {
                    if (dataElement == null) {
                        throw new UnsupportedDataFormatException(
                                "Start code contains an unknown data element ID");
                    }
                }

                hhduc = new HHDuc(visualisationClass, dataElements.toArray(new DataElementType[0]));
                hhduc.setUnpredictableNumber(unpredictableNumber);
            }
        }

        List<DataElementType> definedTypes = hhduc.getDataElementTypes();
        if (definedTypes.size() < rawDataElements.size()) {
            throw new UnsupportedDataFormatException(
                    "More data elements provided than declared by the start code");
        }

        for (int i = 0; i < rawDataElements.size(); i ++) {
            DataElementType type = definedTypes.get(i);
            switch (dataElementEncodings.get(i)) {
                case ASCII:
                    String stringValue = new String(rawDataElements.get(i), DKCharset.INSTANCE);
                    hhduc.setDataElement(type, stringValue);
                    break;

                case BCD:
                    if (!DataElementType.Format.NUMERIC.equals(type.getFormat())) {
                        throw new UnsupportedDataFormatException(
                                "Only numeric data can be BCD coded");
                    }

                    if (rawDataElements.get(i).length == 0) {
                        hhduc.setDataElement(type, "");
                    } else {
                        long longValue;
                        try {
                            longValue = FieldEncoding.bcdDecode(rawDataElements.get(i));
                        } catch (NumberFormatException e) {
                            throw new UnsupportedDataFormatException(
                                    "Illegal numeric data");
                        }
                        hhduc.setDataElement(type, longValue);
                    }
                    break;

                default:
                    throw new UnsupportedDataFormatException("Unsupported data element encoding");
            }
        }

        return hhduc;
    }

    public byte[] getBytes() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Checksum luhnDigit = new LuhnChecksum();

        // LC, will de defined later
        baos.write(0);

        // Start code
        {
            byte[] startCodeEncoded = getStartCode();

            // LS, with control byte, BCD encoding
            baos.write(0x80 | startCodeEncoded.length);

            // Control byte
            baos.write(HHD_CONTROL_BYTE);
            luhnDigit.update(HHD_CONTROL_BYTE);

            // Start code
            baos.write(startCodeEncoded, 0, startCodeEncoded.length);
            luhnDigit.update(startCodeEncoded, 0, startCodeEncoded.length);
        }

        for (Map.Entry<DataElementType, String> entry : dataElements.entrySet()) {
            DataElementType type = entry.getKey();
            String value = entry.getValue();

            byte[] valueEncoded;
            if (DataElementType.Format.NUMERIC.equals(type.getFormat())
                    && type.getFractionDigits() == 0
                    && !value.contains("-")) {
                // non-negative integers can be BCD encoded
                valueEncoded = FieldEncoding.bcdEncode(value);

                // L(DEx), BCD encoding
                baos.write(valueEncoded.length);
            } else {
                valueEncoded = value.getBytes(DKCharset.INSTANCE);

                // L(DEx), ASCII encoding
                baos.write(0x40 | valueEncoded.length);
            }

            baos.write(valueEncoded, 0, valueEncoded.length);
            luhnDigit.update(valueEncoded, 0, valueEncoded.length);
        }

        // Check byte, will be computed later
        baos.write(0);

        byte[] challenge = baos.toByteArray();

        // LC
        challenge[0] = (byte) (baos.size() - 1);

        // Control byte
        Checksum xor = new XorChecksum();
        xor.update(challenge, 0, challenge.length - 1);
        int controlByte = (int) ((luhnDigit.getValue() << 4) | xor.getValue());
        challenge[challenge.length - 1] = (byte) controlByte;

        return challenge;
    }

    public static class UnsupportedDataFormatException extends Exception {
        public UnsupportedDataFormatException(String message) { super(message); }
    }
}
