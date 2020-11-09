/*
 * Copyright (c) 2020 EFDIS AG Bankensoftware, Freising <info@efdis.de>.
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

package de.efdis.tangenerator.gui.common;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;

import androidx.annotation.AttrRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

public abstract class DrawableUtils {

    private DrawableUtils() {}

    public static Drawable getTintedDrawable(Context context,
                                             @NonNull Drawable drawable,
                                             @AttrRes int colorAttr
    ) {
        TypedArray ta = context.obtainStyledAttributes(
                new int[]{colorAttr});
        int textColorPrimary = ta.getColor(0, -1);
        ta.recycle();

        drawable.setTint(textColorPrimary);

        return drawable;
    }

    public static Drawable getTintedDrawable(Context context,
                                             @DrawableRes int id,
                                             @AttrRes int colorAttr
    ) {
        Drawable drawable = ContextCompat.getDrawable(context, id);
        if (drawable == null) {
            return null;
        }

        return getTintedDrawable(context, drawable, colorAttr);
    }


}
