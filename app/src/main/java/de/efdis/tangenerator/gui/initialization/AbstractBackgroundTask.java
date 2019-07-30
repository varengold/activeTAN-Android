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

package de.efdis.tangenerator.gui.initialization;

import android.os.AsyncTask;

import androidx.annotation.StringRes;

public abstract class AbstractBackgroundTask<INPUT, OUTPUT>
        extends AsyncTask<INPUT, Void, OUTPUT> {
    private BackgroundTaskListener<OUTPUT> listener;

    @StringRes
    protected int failedReason;
    protected boolean failedAndProcessShouldBeRepeated;
    protected Throwable failedCause;

    protected AbstractBackgroundTask(BackgroundTaskListener<OUTPUT> listener) {
        this.listener = listener;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        listener.onStart();
    }

    @Override
    protected void onPostExecute(OUTPUT output) {
        if (output != null) {
            listener.onSuccess(output);
        } else {
            listener.onFailure(failedReason, failedAndProcessShouldBeRepeated, failedCause);
        }
        listener.onEnd();

        // prevent leak
        listener = null;
    }

    @Override
    protected void onCancelled() {
        onPostExecute(null);
    }

    public interface BackgroundTaskListener<OUTPUT> {
        void onStart();
        void onSuccess(OUTPUT output);
        void onFailure(@StringRes int reason, boolean processShouldBeRepeated, Throwable cause);
        void onEnd();
    }
}
