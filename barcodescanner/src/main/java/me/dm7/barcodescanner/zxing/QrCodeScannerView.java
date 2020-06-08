/*
 * Copyright (c) 2019-2020 EFDIS AG Bankensoftware, Freising <info@efdis.de>.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package me.dm7.barcodescanner.zxing;

import android.content.Context;
import android.graphics.Rect;
import android.hardware.Camera;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.util.Log;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.PlanarYUVLuminanceSource;
import com.google.zxing.ReaderException;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;

import me.dm7.barcodescanner.core.BarcodeScannerView;

/**
 * This is a replacement for {@link me.dm7.barcodescanner.zxing.ZXingScannerView}
 * with the following optimizations:
 *
 * <ul>
 *     <li>Only QR codes can be detected</li>
 *     <li>Inverted QR codes are not supported (reduces detection time by 50%)</li>
 *     <li>Image data is not rotated before detection,
 *          since QR codes can be detected in any orientation (reduces detection time)</li>
 *     <li>Do not spend 100% cpu time of the detection thread on QR code detection. This improves
 *          performance on slow devices with few cores, which in turn accelerates auto-focus and
 *          thus detection speed. On fast devices, this barely has any impact.</li>
 * </ul>
 */
public class QrCodeScannerView extends BarcodeScannerView {
    private ResultHandler resultHandler;
    private QRCodeReader qrCodeReader;

    public QrCodeScannerView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);

        setLaserEnabled(false);
        setSquareViewFinder(true);

        qrCodeReader = new QRCodeReader();
    }

    public ResultHandler getResultHandler() {
        return resultHandler;
    }

    public void setResultHandler(ResultHandler resultHandler) {
        this.resultHandler = resultHandler;
    }

    public void resumeCameraPreview(ResultHandler resultHandler) {
        setResultHandler(resultHandler);
        resumeCameraPreview();
    }

    @Override
    public void onPreviewFrame(byte[] data, final Camera camera) {
        long startTime = System.currentTimeMillis();
        boolean detected = detectFast(data, camera);
        long endTime = System.currentTimeMillis();

        if (!detected) {
            long frameProcessingTime = endTime - startTime;

            // Sleep after unsuccessful detection to reduce cpu load
            // from 100% to approx. 66%.
            long idleTime = Math.max(0L, Math.min(500L, frameProcessingTime / 2));

            try {
                Log.v(getClass().getSimpleName(),
                        "Detection thread idle for " + idleTime + "ms ...");
                Thread.sleep(idleTime);
            } catch (InterruptedException e) {
                return;
            }

            // Request the next picture for detection
            if (getResultHandler() != null) {
                try {
                    camera.setOneShotPreviewCallback(this);
                } catch (RuntimeException e) {
                    // It is possible that this method is invoked after camera is released.
                    Log.e(getClass().getSimpleName(),
                            "Cannot continue detection", e);
                }
            }
        }
    }

    private boolean detectFast(byte[] data, Camera camera) {
        if (getResultHandler() == null) {
            return false;
        }

        int width;
        int height;
        try {
            Camera.Parameters parameters = camera.getParameters();
            Camera.Size size = parameters.getPreviewSize();
            width = size.width;
            height = size.height;
        } catch (RuntimeException e) {
            // It is possible that this method is invoked after camera is released.
            Log.e(getClass().getSimpleName(),
                    "Cannot access camera", e);
            return false;
        }

        PlanarYUVLuminanceSource source;
        try {
            Rect rect = getFramingRectInPreview(width, height);
            if (rect == null) {
                return false;
            }

            source = new PlanarYUVLuminanceSource(data,
                    width, height,
                    rect.left, rect.top,
                    rect.width(), rect.height(),
                    false);
        } catch (RuntimeException e) {
            Log.e(getClass().getSimpleName(),
                    "Cannot crop camera picture", e);
            return false;
        }

        final Result detectedResult;
        try {
            BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));

            detectedResult = qrCodeReader.decode(bitmap);
        } catch (ReaderException re) {
            // no QR code found
            return false;
        } finally {
            qrCodeReader.reset();
        }

        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(new Runnable() {
            @Override
            public void run() {
                QrCodeScannerView scanner = QrCodeScannerView.this;
                ResultHandler resultHandler = scanner.getResultHandler();

                // Stopping the preview can take a little long.
                // So we want to set result handler to null to discard subsequent calls to
                // onPreviewFrame.
                scanner.setResultHandler(null);

                scanner.stopCameraPreview();

                if (resultHandler != null) {
                    resultHandler.handleResult(detectedResult);
                }
            }
        });

        return true;
    }

}
