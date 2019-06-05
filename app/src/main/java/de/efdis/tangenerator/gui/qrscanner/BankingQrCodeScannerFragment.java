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

import android.os.Bundle;
import androidx.annotation.ColorInt;
import androidx.fragment.app.Fragment;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.zxing.BarcodeFormat;

import java.util.Arrays;

import de.efdis.tangenerator.R;
import me.dm7.barcodescanner.zxing.ZXingScannerView;

/**
 * Start the camera, scan for QR codes and show a live camera preview
 * to support alignment of the device.
 * <p/>
 * Detected QR codes are parsed and forwarded to the Listener
 * defined by {@link #setBankingQrCodeListener(BankingQrCodeListener)}.
 */
public class BankingQrCodeScannerFragment extends Fragment {

    private ZXingScannerView previewImage;
    private ZXingScannerView.ResultHandler detectionHandler;

    public void setBankingQrCodeListener(BankingQrCodeListener listener) {
        if (listener == null) {
            detectionHandler = null;
        } else {
            detectionHandler = new QrCodeHandler(listener);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        if (container == null) {
            previewImage = new ZXingScannerView(getContext());
        } else {
            previewImage = new ZXingScannerView(container.getContext());
        }

        previewImage.setFlash(false);
        previewImage.setAutoFocus(true);
        previewImage.setFormats(Arrays.asList(BarcodeFormat.QR_CODE));
        previewImage.setAspectTolerance(.5f);
        previewImage.setLaserEnabled(false);
        previewImage.setSquareViewFinder(true);

        {
            TypedValue tv = new TypedValue();
            getContext().getTheme().resolveAttribute(R.attr.qrScannerBorderColor, tv, true);
            @ColorInt
            int borderColor = tv.data;
            previewImage.setBorderColor(borderColor);
        }

        {
            TypedValue tv = new TypedValue();
            getContext().getTheme().resolveAttribute(R.attr.qrScannerMaskColor, tv, true);
            @ColorInt
            int maskColor = tv.data;
            maskColor = (maskColor & 0x00ffffff) | 0x40000000; // add opacity
            previewImage.setMaskColor(maskColor);
        }

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
