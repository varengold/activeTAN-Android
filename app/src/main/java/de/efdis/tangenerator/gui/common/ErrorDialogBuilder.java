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
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;

import java.io.PrintWriter;
import java.io.StringWriter;

import de.efdis.tangenerator.BuildConfig;
import de.efdis.tangenerator.R;

/**
 * Custom {@link AlertDialog} builder, which can show the stack trace of a {@link Throwable}.
 *
 * If {@link #setError(Throwable)} has been used, the dialog has an additional “details...” button,
 * which shows the stack trace of the error. The stack trace may be shared (e. g. via email) to
 * report the error to the developer.
 */
public class ErrorDialogBuilder extends AlertDialog.Builder {

    private DialogInterface.OnShowListener onShowListener;
    private CharSequence firstMessage;
    private Throwable error;

    public ErrorDialogBuilder(@NonNull Context context) {
        super(context);
    }

    public void setError(Throwable error) {
        this.error = error;
    }

    public void setOnShowListener(DialogInterface.OnShowListener onShowListener) {
        this.onShowListener = onShowListener;
    }

    @Override
    public AlertDialog.Builder setMessage(@StringRes int messageId) {
        return setMessage(getContext().getText(messageId));
    }

    @Override
    public AlertDialog.Builder setMessage(@Nullable CharSequence message) {
        this.firstMessage = message;
        return this;
    }

    @NonNull
    @Override
    public AlertDialog create() {
        if (error != null) {
            String stackTrace;
            {
                StringWriter stringWriter = new StringWriter();
                PrintWriter printWriter = new PrintWriter(stringWriter);
                error.printStackTrace(printWriter);
                stackTrace = stringWriter.toString();
            }

            super.setMessage(error.getLocalizedMessage() + "\n\n" + stackTrace);

            final Intent shareIntent = new Intent();
            shareIntent.setAction(Intent.ACTION_SEND);
            shareIntent.putExtra(Intent.EXTRA_TITLE,
                    getContext().getString(R.string.share_error_title));
            shareIntent.putExtra(Intent.EXTRA_SUBJECT,
                    getContext().getString(R.string.share_error_subject));
            shareIntent.putExtra(Intent.EXTRA_TEXT,
                    getContext().getString(R.string.share_error_instruction)
                            + "\n\n"
                            + getContext().getString(R.string.device_model,
                            Build.MANUFACTURER, Build.DEVICE, Build.MODEL)
                            + "\n"
                            + getContext().getString(R.string.os_version,
                            Build.VERSION.RELEASE, Build.VERSION.SDK_INT)
                            + "\n"
                            + getContext().getString(R.string.app_version,
                            BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE)
                            + "\n"
                            + getContext().getString(R.string.security_patch,
                            Build.VERSION.SECURITY_PATCH)
                            + "\n\n"
                            + stackTrace);
            shareIntent.setType("text/plain");

            setNeutralButton(R.string.share_error_label, (dialog, which) -> getContext().startActivity(
                    Intent.createChooser(shareIntent, null)));

            final AlertDialog detailsDialog = super.create();
            detailsDialog.setOnShowListener(onShowListener);

            setNeutralButton(R.string.show_details, (dialogInterface, i) -> {
                dialogInterface.dismiss();

                detailsDialog.show();
            });
        }

        super.setMessage(firstMessage);

        AlertDialog dialog = super.create();
        dialog.setOnShowListener(onShowListener);
        return dialog;
    }

}
