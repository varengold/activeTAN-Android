/*
 * Copyright (c) 2014-2017 Dushyanth Maguluru
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

import android.hardware.Camera;

import androidx.annotation.NonNull;

public class CameraWrapper {
    public final Camera mCamera;
    public final int mCameraId;

    private CameraWrapper(@NonNull Camera camera, int cameraId) {
        this.mCamera = camera;
        this.mCameraId = cameraId;
    }

    public static CameraWrapper getWrapper(Camera camera, int cameraId) {
        if (camera == null) {
            return null;
        } else {
            return new CameraWrapper(camera, cameraId);
        }
    }
}
