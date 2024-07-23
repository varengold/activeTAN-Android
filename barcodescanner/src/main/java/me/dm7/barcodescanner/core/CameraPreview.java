/*
 * Copyright (c) 2014-2017 Dushyanth Maguluru
 * Copyright (c) 2020 EFDIS AG Bankensoftware, Freising <info@efdis.de>
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

package me.dm7.barcodescanner.core;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Point;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Display;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;

import java.util.Collections;
import java.util.List;

/*
 * In this file, the original code from https://github.com/dm77/barcodescanner/ has been modified to
 * address an issue with the selection of camera modes on certain devices and a distorted image in
 * the preview.
 *
 * See also the discussion in https://github.com/dm77/barcodescanner/issues/287
 *
 * The original code in getOptimalPreviewSize() used the aspect tolerance property to filter the
 * available camera resolutions and then select one with an "optimal" size.  Then, adjustViewSize()
 * has been used to scale the image and reduce the size of the preview to prevent distortion.  The
 * original approach had three major problems: (1) On some devices, it is not possible find camera
 * resolutions within the desired aspect tolerance, which results in a poor choice of camera
 * resolution.  A workaround to increase the tolerance has even lead to worse choices on some
 * devices.  (2) If this CameraPreview is used within a fixed layout and the camera aspect ratio is
 * not optimal, scaling the image has distorted the preview image on some devices.  (3) Scaling of
 * the preview image was broken on some devices.
 *
 * Thus, we now use a new approach:
 *
 *  1. In getOptimalPreviewSize() we select a camera resolution with an optimal aspect ratio, which
 * minimizes the invisible part of the camera image during the next step.
 *
 *  2. Instead of adjustViewSize() we now use zoomCropPreview() and do not change the size of the
 * preview as given by the layout manager.  We scale the camera image such that it is not distorted
 * within the given layout dimensions.  As a result, this view may have any size and can use any
 * camera resolution.
 *
 *  3. We have changed the base class from SurfaceView to TextureView, which fixes image scaling
 * problems on some devices.
 *
 *  --- EFDIS AG Bankensoftware, Feb 2020
 */
public class CameraPreview extends TextureView implements TextureView.SurfaceTextureListener {
    private static final String TAG = "CameraPreview";

    private CameraWrapper mCameraWrapper;
    private Handler mAutoFocusHandler;
    private boolean mPreviewing = true;
    private boolean mAutoFocus = true;
    private boolean mSurfaceCreated = false;
    private boolean mShouldScaleToFill = true;
    private Camera.PreviewCallback mPreviewCallback;

    public CameraPreview(Context context, CameraWrapper cameraWrapper, Camera.PreviewCallback previewCallback) {
        super(context);
        init(cameraWrapper, previewCallback);
    }

    public CameraPreview(Context context, AttributeSet attrs, CameraWrapper cameraWrapper, Camera.PreviewCallback previewCallback) {
        super(context, attrs);
        init(cameraWrapper, previewCallback);
    }

    public void init(CameraWrapper cameraWrapper, Camera.PreviewCallback previewCallback) {
        setCamera(cameraWrapper, previewCallback);
        mAutoFocusHandler = new Handler();
        setSurfaceTextureListener(this);
    }

    public void setCamera(CameraWrapper cameraWrapper, Camera.PreviewCallback previewCallback) {
        mCameraWrapper = cameraWrapper;
        mPreviewCallback = previewCallback;
    }

    public void setShouldScaleToFill(boolean scaleToFill) {
        mShouldScaleToFill = scaleToFill;
    }

    public void setAspectTolerance(float aspectTolerance) {
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        mSurfaceCreated = true;
        if (mPreviewing) {
            /*
             * If showCameraPreview() has been called before the surface texture was
             * available, the preview could not be started. Thus, we have to call it
             * again.
             */
            showCameraPreview();
        }
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        if (mPreviewing) {
            stopCameraPreview();
            showCameraPreview();
        }
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        mSurfaceCreated = false;
        if (mPreviewing) {
            stopCameraPreview();
        }
        return true;
    }

    public void showCameraPreview() {
        mPreviewing = true;
        if(mCameraWrapper != null && mSurfaceCreated) {
            try {
                setupCameraParameters();
                mCameraWrapper.mCamera.setPreviewTexture(getSurfaceTexture());
                mCameraWrapper.mCamera.setDisplayOrientation(getDisplayOrientation());
                mCameraWrapper.mCamera.setOneShotPreviewCallback(mPreviewCallback);
                mCameraWrapper.mCamera.startPreview();
                if(mAutoFocus) {
                    if (mSurfaceCreated) { // check if surface created before using autofocus
                        safeAutoFocus();
                    } else {
                        scheduleAutoFocus(); // wait 1 sec and then do check again
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, e.toString(), e);
            }
        }
    }

    public void safeAutoFocus() {
        try {
            mCameraWrapper.mCamera.autoFocus(autoFocusCB);
        } catch (RuntimeException re) {
            // Horrible hack to deal with autofocus errors on Sony devices
            // See https://github.com/dm77/barcodescanner/issues/7 for example
            scheduleAutoFocus(); // wait 1 sec and then do check again
        }
    }

    public void stopCameraPreview() {
        mPreviewing = false;
        if(mCameraWrapper != null) {
            try {
                mCameraWrapper.mCamera.cancelAutoFocus();
                mCameraWrapper.mCamera.setOneShotPreviewCallback(null);
                mCameraWrapper.mCamera.stopPreview();
            } catch(Exception e) {
                Log.e(TAG, e.toString(), e);
            }
        }
    }

    public void setupCameraParameters() {
        Camera.Size optimalSize = getOptimalPreviewSize();
        if (optimalSize == null) {
            Log.e(TAG, "Cannot set up camera parameters, camera not ready");
            return;
        }

        Camera.Parameters parameters = mCameraWrapper.mCamera.getParameters();
        parameters.setPreviewSize(optimalSize.width, optimalSize.height);
        mCameraWrapper.mCamera.setParameters(parameters);

        // adjustViewSize(optimalSize) is broken inside a fixed layout, thus we use this instead:
        zoomCropPreview(optimalSize);
    }


    private void zoomCropPreview(Camera.Size cameraSize) {
        Point idealPreviewSize;
        if (getDisplayOrientation() % 180 == 0) {
            idealPreviewSize = new Point(cameraSize.width, cameraSize.height);
        } else {
            idealPreviewSize = new Point(cameraSize.height, cameraSize.width);
        }

        Point targetPreviewSize = new Point(getWidth(), getHeight());

        // By default, the preview image is scaled to fit the view's exact layout dimensions.
        // We increase scaling in one dimension such that the image is no longer distorted.
        if (idealPreviewSize.x * targetPreviewSize.y >= targetPreviewSize.x * idealPreviewSize.y) {
            setScaleX(
                    (float) (idealPreviewSize.x * targetPreviewSize.y)
                            / (targetPreviewSize.x * idealPreviewSize.y));
            setScaleY(1.0f);
        } else {
            setScaleX(1.0f);
            setScaleY(
                    (float) (targetPreviewSize.x * idealPreviewSize.y)
                            / (idealPreviewSize.x * targetPreviewSize.y));
        }
    }

    @SuppressWarnings("SuspiciousNameCombination")
    private Point convertSizeToLandscapeOrientation(Point size) {
        if (getDisplayOrientation() % 180 == 0) {
            return size;
        } else {
            return new Point(size.y, size.x);
        }
    }

    @SuppressWarnings("SuspiciousNameCombination")
    private void setViewSize(int width, int height) {
        ViewGroup.LayoutParams layoutParams = getLayoutParams();
        int tmpWidth;
        int tmpHeight;
        if (getDisplayOrientation() % 180 == 0) {
            tmpWidth = width;
            tmpHeight = height;
        } else {
            tmpWidth = height;
            tmpHeight = width;
        }

        if (mShouldScaleToFill) {
            int parentWidth = ((View) getParent()).getWidth();
            int parentHeight = ((View) getParent()).getHeight();
            float ratioWidth = (float) parentWidth / (float) tmpWidth;
            float ratioHeight = (float) parentHeight / (float) tmpHeight;

            float compensation = Math.max(ratioWidth, ratioHeight);

            tmpWidth = Math.round(tmpWidth * compensation);
            tmpHeight = Math.round(tmpHeight * compensation);
        }

        layoutParams.width = tmpWidth;
        layoutParams.height = tmpHeight;
        setLayoutParams(layoutParams);
    }

    public int getDisplayOrientation() {
        if (mCameraWrapper == null) {
            //If we don't have a camera set there is no orientation so return dummy value
            return 0;
        }

        Camera.CameraInfo info = new Camera.CameraInfo();
        if(mCameraWrapper.mCameraId == -1) {
            Camera.getCameraInfo(Camera.CameraInfo.CAMERA_FACING_BACK, info);
        } else {
            Camera.getCameraInfo(mCameraWrapper.mCameraId, info);
        }

        WindowManager wm = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();

        int rotation = display.getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0: degrees = 0; break;
            case Surface.ROTATION_90: degrees = 90; break;
            case Surface.ROTATION_180: degrees = 180; break;
            case Surface.ROTATION_270: degrees = 270; break;
        }

        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360;  // compensate the mirror
        } else {  // back-facing
            result = (info.orientation - degrees + 360) % 360;
        }
        return result;
    }

    /*
     * Compute the relative error of the image by scaling it to a given target size.
     *
     * @return
     *          The result is zero, iff. the image can be scaled without distortion.
     *          Otherwise, the result is greater than zero. For example, the result is 1.0 if
     *          the image must be scaled by 100% in one dimension.
     */
    private static double relativeDistortionError(Camera.Size targetSize, Camera.Size imageSize) {
        return Math.max(
                (double) (targetSize.width * imageSize.height) / (targetSize.height * imageSize.width) - 1.0,
                (double) (targetSize.height * imageSize.width) / (targetSize.width * imageSize.height) - 1.0);
    }

    private Camera.Size getOptimalPreviewSize() {
        if(mCameraWrapper == null) {
            return null;
        }

        List<Camera.Size> sizes = mCameraWrapper.mCamera.getParameters().getSupportedPreviewSizes();
        if (sizes == null || sizes.isEmpty()) return null;

        // Sort sizes by their smallest dimension.
        Collections.sort(sizes, (o1, o2) -> Integer.compare(Math.min(o1.width, o1.height),
                Math.min(o2.width, o2.height)));

        Camera.Size idealSize;
        if (DisplayUtils.getScreenOrientation(getContext()) == Configuration.ORIENTATION_PORTRAIT) {
            idealSize = mCameraWrapper.mCamera.new Size(getHeight(), getWidth());
        } else {
            idealSize = mCameraWrapper.mCamera.new Size(getWidth(), getHeight());
        }

        Camera.Size optimalSize = null;
        double minError = Double.MAX_VALUE;

        // Select the smallest size greater or equal to 540x540 with the smallest distortion error
        for (Camera.Size size : sizes) {
            if (Math.min(size.width, size.height) < 540) {
                // It is hard to scan QR codes with very low camera resolutions
                continue;
            }

            double error = relativeDistortionError(idealSize, size);
            if (error < minError) {
                optimalSize = size;
                minError = error;
            }
        }

        // No camera resolution with at least 540x540? Use the largest size available.
        if (optimalSize == null) {
            optimalSize = sizes.get(sizes.size() - 1);
        }

        return optimalSize;
    }

    public void setAutoFocus(boolean state) {
        if(mCameraWrapper != null && mPreviewing) {
            if(state == mAutoFocus) {
                return;
            }
            mAutoFocus = state;
            if(mAutoFocus) {
                if (mSurfaceCreated) { // check if surface created before using autofocus
                    Log.v(TAG, "Starting autofocus");
                    safeAutoFocus();
                } else {
                    scheduleAutoFocus(); // wait 1 sec and then do check again
                }
            } else {
                Log.v(TAG, "Cancelling autofocus");
                mCameraWrapper.mCamera.cancelAutoFocus();
            }
        }
    }

    private final Runnable doAutoFocus = new Runnable() {
        public void run() {
            if(mCameraWrapper != null && mPreviewing && mAutoFocus && mSurfaceCreated) {
                safeAutoFocus();
            }
        }
    };

    // Mimic continuous auto-focusing
    private final Camera.AutoFocusCallback autoFocusCB = (success, camera) -> scheduleAutoFocus();

    private void scheduleAutoFocus() {
        mAutoFocusHandler.postDelayed(doAutoFocus, 1000);
    }
}
