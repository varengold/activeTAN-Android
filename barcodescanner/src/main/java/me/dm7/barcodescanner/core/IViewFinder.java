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

import android.graphics.Rect;

public interface IViewFinder {

    void setLaserColor(int laserColor);
    void setMaskColor(int maskColor);
    void setBorderColor(int borderColor);
    void setBorderStrokeWidth(int borderStrokeWidth);
    void setBorderLineLength(int borderLineLength);
    void setLaserEnabled(boolean isLaserEnabled);

    void setBorderCornerRounded(boolean isBorderCornersRounded);
    void setBorderAlpha(float alpha);
    void setBorderCornerRadius(int borderCornersRadius);
    void setViewFinderOffset(int offset);
    void setSquareViewFinder(boolean isSquareViewFinder);
    /**
     * Method that executes when Camera preview is starting.
     * It is recommended to update framing rect here and invalidate view after that. <br/>
     * For example see: {@link ViewFinderView#setupViewFinder()}
     */
    void setupViewFinder();

    /**
     * Provides {@link Rect} that identifies area where barcode scanner can detect visual codes
     * <p>Note: This rect is a area representation in absolute pixel values. <br/>
     * For example: <br/>
     * If View's size is 1024x800 so framing rect might be 500x400</p>
     *
     * @return {@link Rect} that identifies barcode scanner area
     */
    Rect getFramingRect();

    /**
     * Width of a {@link android.view.View} that implements this interface
     * <p>Note: this is already implemented in {@link android.view.View},
     * so you don't need to override method and provide your implementation</p>
     *
     * @return width of a view
     */
    int getWidth();

    /**
     * Height of a {@link android.view.View} that implements this interface
     * <p>Note: this is already implemented in {@link android.view.View},
     * so you don't need to override method and provide your implementation</p>
     *
     * @return height of a view
     */
    int getHeight();
}
