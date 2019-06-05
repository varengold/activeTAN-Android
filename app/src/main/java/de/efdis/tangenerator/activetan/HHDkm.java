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

import java.io.ByteArrayInputStream;

import de.efdis.tangenerator.persistence.keystore.BankingKeyComponents;

/**
 * Hand held device, key material
 */
public class HHDkm {

    private KeyMaterialType type;
    private byte[] aesKeyComponent;
    private int letterNumber;
    private String deviceSerialNumber;

    public KeyMaterialType getType() {
        return type;
    }

    public byte[] getAesKeyComponent() {
        return aesKeyComponent;
    }

    public int getLetterNumber() {
        return letterNumber;
    }

    public String getDeviceSerialNumber() {
        return deviceSerialNumber;
    }

    public static HHDkm parse(byte[] rawBytes) throws UnsupportedDataFormatException {
        HHDkm result = new HHDkm();
        ByteArrayInputStream bais = new ByteArrayInputStream(rawBytes);

        int prefix = bais.read();
        if (prefix < 0) {
            throw new UnsupportedDataFormatException("missing prefix");
        }

        for (KeyMaterialType type : KeyMaterialType.values()) {
            if (prefix == type.getHHDkmPrefix()) {
                result.type = type;
                break;
            }
        }
        if (result.type == null) {
            throw new UnsupportedDataFormatException("unsupported prefix");
        }

        result.aesKeyComponent = new byte[BankingKeyComponents.BANKING_KEY_LENGTH];
        if (bais.read(result.aesKeyComponent, 0, result.aesKeyComponent.length)
                != BankingKeyComponents.BANKING_KEY_LENGTH) {
            throw new UnsupportedDataFormatException("incomplete key data");
        }

        if (result.type == KeyMaterialType.PORTAL) {
            byte[] serialNumber = new byte[12];
            if (bais.read(serialNumber, 0, serialNumber.length) != serialNumber.length) {
                throw new UnsupportedDataFormatException("incomplete serial number");
            }
            result.deviceSerialNumber = new String(serialNumber, DKCharset.INSTANCE);
        }

        int letterNumber = bais.read();
        if (letterNumber < 0) {
            throw new UnsupportedDataFormatException("missing letter number");
        }

        try {
            result.letterNumber = (int) FieldEncoding.bcdDecode(new byte[]{(byte) letterNumber});
        } catch (NumberFormatException e) {
            throw new UnsupportedDataFormatException("illegal letter number format");
        }

        /* The remaining data contains text instructions for other hand held devices,
         * which can be ignored.
         */

        return result;

    }

    public static class UnsupportedDataFormatException extends Exception {
        public UnsupportedDataFormatException(String message) {
            super(message);
        }
    }

}
