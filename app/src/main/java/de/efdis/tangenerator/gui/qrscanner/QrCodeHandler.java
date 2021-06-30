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
import android.util.Pair;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.Result;
import com.google.zxing.qrcode.decoder.Mode;

import java.util.Arrays;

import de.efdis.tangenerator.activetan.BQRContainer;
import me.dm7.barcodescanner.zxing.ResultHandler;

/** Filter and parse QR codes in Banking QR code format */
public class QrCodeHandler implements ResultHandler {

    private static final String TAG = QrCodeHandler.class.getSimpleName();

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
            Log.e(TAG, "invalid QR code content encoding", e);
            listener.onInvalidBankingQrCode(e.getMessage());
            return;
        }


        Pair<BQRContainer.ContentType, byte[]> content;
        try {
            content = BQRContainer.unwrap(bqr);
        } catch (BQRContainer.InvalidBankingQrCodeException e) {
            Log.e(TAG, "invalid BQR format", e);
            listener.onInvalidBankingQrCode(e.getMessage());
            return;
        }

        switch (content.first) {
            case TRANSACTION_DATA:
                listener.onTransactionData(content.second);
                return;

            case KEY_MATERIAL:
                listener.onKeyMaterial(content.second);
                return;

            default:
                listener.onInvalidBankingQrCode("Unsupported BQR content type");
                return;
        }
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

    private static class NoBankingQrCodeException extends Exception {
        public NoBankingQrCodeException(String message) {
            super(message);
        }
    }
}
