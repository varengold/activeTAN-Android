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

package de.efdis.tangenerator.gui.qrscanner;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Build;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import de.efdis.tangenerator.R;
import me.dm7.barcodescanner.zxing.QrCodeScannerView;

/**
 * Start the camera, scan for QR codes and show a live camera preview
 * to support alignment of the device.
 * <p/>
 * Detected QR codes are parsed and forwarded to the Listener
 * defined by {@link #setBankingQrCodeListener(BankingQrCodeListener)}.
 */
public class BankingQrCodeScannerFragment extends Fragment {

    private QrCodeScannerView previewImage;
    private QrCodeHandler detectionHandler;

    public void setBankingQrCodeListener(BankingQrCodeListener listener) {
        if (listener == null) {
            detectionHandler = null;
        } else {
            detectionHandler = new QrCodeHandler(listener);
        }
    }

    @Override
    public void onInflate(@NonNull Context context, @NonNull AttributeSet attrs, @Nullable Bundle savedInstanceState) {
        super.onInflate(context, attrs, savedInstanceState);

        // Forward attributes to scanner view
        previewImage = new QrCodeScannerView(context, attrs);

        // The mask color should not have full opacity
        TypedArray a = context.getTheme().obtainStyledAttributes(
                attrs,
                R.styleable.BankingQrCodeScannerFragment,
                0, 0);
        {
            @ColorInt
            int maskColor = a.getColor(R.styleable.BankingQrCodeScannerFragment_maskColor, 0);

            if ((maskColor & 0xff000000) == 0xff000000) {
                int maskOpacity = Math.round(a.getFraction(
                        R.styleable.BankingQrCodeScannerFragment_maskOpacity,
                        255, 255, 64));

                if (0 <= maskOpacity && maskOpacity <= 255) {
                    maskColor = (maskColor & 0x00ffffff) | (maskOpacity << 24); // add opacity
                    previewImage.setMaskColor(maskColor);
                }
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            a.close();
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        previewImage.setFlash(false);
        previewImage.setAutoFocus(true);

        return previewImage;
    }

    @Override
    public void onStart() {
        super.onStart();

        if (detectionHandler != null) {
            previewImage.setResultHandler(detectionHandler);
            previewImage.startCamera();
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        if (detectionHandler != null) {
            previewImage.resumeCameraPreview(detectionHandler);
        }
    }

    @Override
    public void onPause() {
        previewImage.stopCamera();

        super.onPause();
    }

}
