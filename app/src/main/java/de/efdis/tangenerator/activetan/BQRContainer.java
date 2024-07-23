/*
 * Copyright (c) 2021 EFDIS AG Bankensoftware, Freising <info@efdis.de>.
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

import android.util.Pair;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.zip.Checksum;

/**
 * Container format for Banking QR codes
 */
public class BQRContainer {
    public enum ContentType {
        /**
         * Key material (HDDkm wrapper) for TAN generator initialization,
         * as defined by EFDIS AG and REINER SCT in the activeTAN specification.
         */
        KEY_MATERIAL("KM"),

        /**
         * Transaction data (HHDuc wrapper) as defined in the chipTAN specification
         * “HandHeld-Device (HHD) zur TAN-Erzeugung - HHD-Erweiterung für optische Schnittstellen”
         * version 1.5.1 published by Deutsche Kreditwirtschaft (DK).
         */
        TRANSACTION_DATA("DK"),
        ;

        private final String prefix;

        ContentType(String prefix) {
            this.prefix = prefix;
        }

        public String getPrefix() {
            return prefix;
        }

        public byte[] getPrefixBytes() {
            return prefix.getBytes(DKCharset.INSTANCE);
        }


        public static ContentType valueOfPrefixBytes(byte[] prefix) {
            for (ContentType contentType : ContentType.values()) {
                if (Arrays.equals(contentType.getPrefixBytes(), prefix)) {
                    return contentType;
                }
            }

            throw new IllegalArgumentException("Unknown prefix");
        }
    }

    public static Pair<ContentType, byte[]> unwrap(byte[] bqr) throws InvalidBankingQrCodeException {
        bqr = unscramble(bqr);

        checkCrc16(bqr);

        // 2 bytes prefix
        // wrapped content
        // 2 bytes CRC-16
        ByteArrayInputStream blockInputStream
                = new ByteArrayInputStream(bqr, 2, bqr.length - 4);

        if (blockInputStream.available() == 0) {
            throw new InvalidBankingQrCodeException("Empty BQR container");
        }

        ContentType contentType;
        try {
            contentType = ContentType.valueOfPrefixBytes(Arrays.copyOf(bqr, 2));
        } catch (IllegalArgumentException e) {
            throw new InvalidBankingQrCodeException("Unknown BQR prefix");
        }

        switch (contentType) {
            case TRANSACTION_DATA: {
                // 'DK' prefix: chipTAN QR codes with transaction data
                boolean amsFlag = readAmsFlag(blockInputStream);
                byte[] hhduc = readDataBlock(blockInputStream);

                if (amsFlag) {
                    // skip optional AMS data block
                    // content is ignored
                    readDataBlock(blockInputStream);
                }

                if (blockInputStream.available() > 0) {
                    throw new InvalidBankingQrCodeException(
                            "Unexpected data after last block found");
                }

                return new Pair<>(contentType, hhduc);
            }

            case KEY_MATERIAL: {
                // 'KM' prefix: key material for device initialization
                byte[] hhdkm = new byte[blockInputStream.available()];
                blockInputStream.read(hhdkm, 0, hhdkm.length);
                return new Pair<>(contentType, hhdkm);
            }

            default:
                throw new InvalidBankingQrCodeException("Unsupported BQR prefix");
        }
    }

    public static byte[] wrap(ContentType contentType, byte[] payload) {
        // 2 bytes prefix
        // wrapped content
        // 2 bytes CRC-16
        ByteArrayOutputStream baos = new ByteArrayOutputStream(payload.length + 4);
        Checksum checksum = new CRC16Checksum(0);

        baos.write(contentType.getPrefixBytes(), 0, 2);
        checksum.update(contentType.getPrefixBytes(), 0, 2);

        baos.write(payload, 0, payload.length);
        checksum.update(payload, 0, payload.length);

        baos.write((((int) checksum.getValue()) & 0xff00) >> 8);
        baos.write(((int) checksum.getValue()) & 0x00ff);

        byte[] bqr = baos.toByteArray();

        // We need to scramble the content, which is the same operation like unscrambling
        try {
            bqr = unscramble(bqr);
        } catch (InvalidBankingQrCodeException e) {
            throw new IllegalArgumentException(e);
        }

        return bqr;
    }

    private static byte[] unscramble(byte[] scrambledBqr) throws InvalidBankingQrCodeException {
        if (scrambledBqr.length < 2) {
            throw new InvalidBankingQrCodeException("No BQR container prefix, data too short");
        }

        byte[] unscrambledBqr = new byte[scrambledBqr.length];

        // copy prefix
        unscrambledBqr[0] = scrambledBqr[0];
        unscrambledBqr[1] = scrambledBqr[1];

        // unscramble content
        for (int i = 2; i < unscrambledBqr.length; i++) {
            unscrambledBqr[i] = (byte) ((scrambledBqr[i] & 0xff) ^ scrambledBqr[i % 2]);
        }

        return unscrambledBqr;
    }

    private static void checkCrc16(byte[] bqr) throws InvalidBankingQrCodeException {
        if (bqr.length < 4) {
            throw new InvalidBankingQrCodeException("No BQR container checksum, data too short");
        }

        final int expectedChecksum =
                ((bqr[bqr.length - 2] & 0xff) << 8) | (bqr[bqr.length - 1] & 0xff);

        final int actualChecksum;
        {
            Checksum checksum = new CRC16Checksum(0);
            checksum.update(bqr, 0, bqr.length - 2);
            actualChecksum = (int) checksum.getValue();
        }

        if (expectedChecksum != actualChecksum) {
            throw new InvalidBankingQrCodeException("CRC-16 checksum is wrong");
        }
    }

    private static boolean readAmsFlag(ByteArrayInputStream in) throws InvalidBankingQrCodeException {
        int flag = in.read();
        if (flag < 0) {
            throw new InvalidBankingQrCodeException(
                    "No AMS flag available");
        }

        switch (flag) {
            case 0x4e: // N
                return false;
            case 0x4a: // J
                return true;
            default:
                throw new InvalidBankingQrCodeException(
                        "Invalid AMS flag value");
        }
    }

    private static byte[] readDataBlock(ByteArrayInputStream in) throws InvalidBankingQrCodeException {
        // according to specification, maximum length is limited to 255 Bytes
        int length = in.read();
        if (length < 0) {
            throw new InvalidBankingQrCodeException(
                    "No data block available");
        }

        byte[] block = new byte[length + 1];
        block[0] = (byte) length;

        if (length != in.read(block, 1, length)) {
            throw new InvalidBankingQrCodeException(
                    "Declared block length is too large");
        }

        return block;
    }

    public static class InvalidBankingQrCodeException extends Exception {
        InvalidBankingQrCodeException(String message) {
            super(message);
        }
    }

}
