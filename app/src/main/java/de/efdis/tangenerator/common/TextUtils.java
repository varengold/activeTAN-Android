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

package de.efdis.tangenerator.common;

import android.os.Build;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.widget.TextView;

import androidx.annotation.StringRes;
import androidx.core.text.HtmlCompat;

/**
 * Utility functions for text display
 */
public abstract class TextUtils {

    private TextUtils() {}

    /**
     * Utility method to apply HTML formatting to text.
     */
    private static Spanned fromHtml(String html) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            // Android 6.0 does not support lists.
            html = html
                    .replace("<li>", "<p>&#x2022; ")
                    .replace("</li>", "</p>");
        }
        return HtmlCompat.fromHtml(html, HtmlCompat.FROM_HTML_MODE_LEGACY);
    }

    /**
     * Utility method to display HTML formatted text in a {@link TextView}.
     * Links are interactive.
     */
    public static void setHtmlText(TextView textView, String html) {
        Spanned formatted = fromHtml(html);
        textView.setText(formatted);
        textView.setMovementMethod(LinkMovementMethod.getInstance());
    }

    /**
     * Utility method to display HTML formatted text from a string resource in a {@link TextView}.
     * Links are interactive.
     */
    public static void setHtmlText(TextView textView, @StringRes int html) {
        setHtmlText(textView, textView.getContext().getText(html).toString());
    }
}
