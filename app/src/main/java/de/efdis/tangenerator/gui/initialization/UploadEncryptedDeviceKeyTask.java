/*
 * Copyright (c) 2019-2020 EFDIS AG Bankensoftware, Freising <info@efdis.de>.
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

import android.content.Context;
import android.util.Log;

import java.io.IOException;
import java.net.ConnectException;
import java.net.URL;
import java.nio.charset.Charset;

import de.efdis.tangenerator.R;
import de.efdis.tangenerator.persistence.keystore.BankingKeyComponents;

/**
 * Generate a new device key component and upload it to the banking backend.
 * <p/>
 * The backend returns a serial number (<code>tokenId</code>) for the stored device key information.
 */
public class UploadEncryptedDeviceKeyTask
        extends AbstractApiCallTask<Void, UploadEncryptedDeviceKeyTask.Output> {

    public static class Output {
        public String tokenId;
        public byte[] deviceKeyComponent;
    }

    public UploadEncryptedDeviceKeyTask(BackgroundTaskListener<Output> listener, URL url, Context context)
            throws CallFailedException {
        super(listener, url, context);
    }

    @Override
    protected Output doInBackground(Void... args) {
        Output result = new Output();
        {
            BankingKeyComponents keyComponents = new BankingKeyComponents();
            keyComponents.generateDeviceKeyComponent();
            result.deviceKeyComponent = keyComponents.deviceKeyComponent;
        }

        byte[] rawTokenId;
        try {
            rawTokenId = performPostRequest(result.deviceKeyComponent);
        } catch (ConnectException e) {
            Log.e(getClass().getSimpleName(),
                    "unable to upload device key, because server is unreachable", e);
            failedReason = R.string.initialization_failed_offline;
            failedAndProcessShouldBeRepeated = true;
            failedCause = e;
            return null;
        } catch (IOException e) {
            Log.e(getClass().getSimpleName(),
                    "unable to upload device key, because of (temporary) I/O problem", e);
            failedReason = R.string.initialization_failed_communication;
            failedAndProcessShouldBeRepeated = true;
            failedCause = e;
            return null;
        } catch (OutdatedClientException e) {
            Log.e(getClass().getSimpleName(),
                    "unable to upload device key, because client is old", e);
            failedReason = R.string.initialization_failed_outdated;
            failedCause = e;
            return null;
        } catch (CallFailedException e) {
            Log.e(getClass().getSimpleName(),
                    "unable to upload device key, unknown cause", e);
            failedReason = R.string.initialization_failed_unknown_reason;
            failedCause = e;
            return null;
        }

        result.tokenId = new String(rawTokenId, Charset.defaultCharset());

        return result;
    }
}
