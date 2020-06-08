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

package de.efdis.tangenerator.gui.qrscanner;

import android.util.Log;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.Result;
import com.google.zxing.qrcode.decoder.Mode;

import java.io.ByteArrayInputStream;
import java.util.Arrays;
import java.util.zip.Checksum;

import de.efdis.tangenerator.activetan.CRC16Checksum;
import me.dm7.barcodescanner.zxing.ResultHandler;

/** Filter and parse QR codes in Banking QR code format */
public class QrCodeHandler implements ResultHandler {

    private final BankingQrCodeListener listener;

    public QrCodeHandler(BankingQrCodeListener listener) {
        this.listener = listener;
    }

    /**
     * Parse the QR code content and inform the registered {@link BankingQrCodeListener}.
     *
     * @param result
     *      Detection event from ZXing.
     */
    @Override
    public void handleResult(Result result) {
        Log.i(getClass().getSimpleName(),
                "Barcode detected");

        byte[] bqr;
        try {
            bqr = extractBinaryQrCodeContent(result);
        } catch (NoBankingQrCodeException e) {
            listener.onInvalidBankingQrCode(e.getMessage());
            return;
        }

        if (bqr.length < 4) {
            listener.onInvalidBankingQrCode("no BQR container, data too short");
            return;
        }

        unscramble(bqr);

        if (!checkCrc16(bqr)) {
            listener.onInvalidBankingQrCode("invalid CRC-16 checksum");
            return;
        }

        // 2 bytes prefix
        // wrapped content
        // 2 bytes CRC-16
        ByteArrayInputStream blockInputStream
                = new ByteArrayInputStream(bqr, 2, bqr.length - 4);

        if (blockInputStream.available() == 0) {
            listener.onInvalidBankingQrCode("empty BQR container");
            return;
        }

        // 'DK' prefix: chipTAN QR codes with transaction data
        if (bqr[0] == 0x44 && bqr[1] == 0x4b) {
            try {
                boolean amsFlag = readAmsFlag(blockInputStream);
                byte[] hhduc = readDataBlock(blockInputStream);

                if (amsFlag) {
                    // skip optional AMS data block
                    // content is ignored
                    readDataBlock(blockInputStream);
                }

                if (blockInputStream.available() > 0) {
                    throw new NoBankingQrCodeException(
                            "Unexpected data after last block found");
                }

                listener.onTransactionData(hhduc);
            } catch (NoBankingQrCodeException e) {
                listener.onInvalidBankingQrCode(e.getMessage());
            }
            return;
        }

        // 'KM' prefix: key material for device initialization
        if (bqr[0] == 0x4b && bqr[1] == 0x4d) {
            byte[] hhdkm = new byte[blockInputStream.available()];
            blockInputStream.read(hhdkm, 0, hhdkm.length);
            listener.onKeyMaterial(hhdkm);
            return;
        }

        listener.onInvalidBankingQrCode("unsupported prefix");
    }

    private byte[] extractBinaryQrCodeContent(Result result) throws NoBankingQrCodeException {
        if (!BarcodeFormat.QR_CODE.equals(result.getBarcodeFormat())) {
            // This should not happen, ZXing can ignore other barcode formats.
            assert false;
            throw new NoBankingQrCodeException(
                    "Barcode is not in QR code format");
        }

        byte[] rawBytes = result.getRawBytes();
        if (rawBytes.length < 2) {
            // The raw data must be at lease 2 bytes long:
            //  - 1 half-byte for mode indicator
            //  - 1 byte for data length (2 bytes for long data)
            //  - data
            //  - 1 half-byte for end of message terminator
            //  - padding
            throw new NoBankingQrCodeException(
                    "Not a valid QR code, no data has been read");
        }

        int modeIndicator = (rawBytes[0] & 0xf0) >> 4;
        if (modeIndicator != Mode.BYTE.getBits()) {
            throw new NoBankingQrCodeException(
                    "QR code is not in byte encoding mode");
        }

        // The byte values are offset by 4 bits, because of the mode indicator. Undo this.
        // We lose the last 4 bits, which might include the mandatory end of message terminator.
        byte[] lengthContentAndPadding = new byte[rawBytes.length - 1];
        for (int i = 0; i < lengthContentAndPadding.length; i++) {
            int firstHalfByte = (rawBytes[i] & 0x0f) << 4;
            int secondHalfByte = (rawBytes[i + 1] & 0xf0) >> 4;
            lengthContentAndPadding[i] = (byte) (firstHalfByte | secondHalfByte);
        }

        int length;
        int contentOffset;
        if (lengthContentAndPadding.length >= 256) {
            // For long messages the length is encoded with 2 bytes (unsigned integer)
            length = (lengthContentAndPadding[0] & 0xff) << 8 | (lengthContentAndPadding[1] & 0xff);
            contentOffset = 2;
        } else {
            // For short messages the length is encoded with 1 byte (unsigned integer)
            length = lengthContentAndPadding[0] & 0xff;
            contentOffset = 1;
        }

        byte[] content = Arrays.copyOfRange(lengthContentAndPadding,
                contentOffset, contentOffset + length);

        return content;
    }

    private void unscramble(byte[] bqr) {
        if (bqr.length < 2) {
            return;
        }

        for (int i = 2; i < bqr.length; i++) {
            bqr[i] = (byte) ((bqr[i] & 0xff) ^ bqr[i % 2]);
        }
    }

    private boolean checkCrc16(byte[] bqr) {
        final int expectedChecksum =
                ((bqr[bqr.length - 2] & 0xff) << 8) | (bqr[bqr.length - 1] & 0xff);

        final int actualChecksum;
        {
            Checksum checksum = new CRC16Checksum(0);
            checksum.update(bqr, 0, bqr.length - 2);
            actualChecksum = (int) checksum.getValue();

        }

        return expectedChecksum == actualChecksum;
    }

    private boolean readAmsFlag(ByteArrayInputStream in) throws NoBankingQrCodeException {
        int flag = in.read();
        if (flag < 0) {
            throw new NoBankingQrCodeException(
                    "No AMS flag available");
        }

        switch (flag) {
            case 0x4e: // N
               return false;
            case 0x4a: // J
                return true;
            default:
                throw new NoBankingQrCodeException(
                        "Invalid AMS flag value");
        }
    }

    private byte[] readDataBlock(ByteArrayInputStream in) throws NoBankingQrCodeException {
        // according to specification, maximum length is limited to 255 Bytes
        int length = in.read();
        if (length < 0) {
            throw new NoBankingQrCodeException(
                    "No data block available");
        }

        byte[] block = new byte[length + 1];
        block[0] = (byte) length;

        if (length != in.read(block, 1, length)) {
            throw new NoBankingQrCodeException(
                    "Declared block length is too large");
        }

        return block;
    }

    private static class NoBankingQrCodeException extends Exception {
        public NoBankingQrCodeException(String message) {
            super(message);
        }
    }
}
