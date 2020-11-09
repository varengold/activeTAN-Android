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

import de.efdis.tangenerator.R;
import de.efdis.tangenerator.api.DeviceKeyApi;
import de.efdis.tangenerator.api.SecuredRestApiEndpoint;
import de.efdis.tangenerator.persistence.keystore.BankingKeyComponents;

/**
 * Generate a new device key component and upload it to the banking backend.
 * <p/>
 * The backend returns a serial number (<code>tokenId</code>) for the stored device key information.
 */
public class UploadEncryptedDeviceKeyTask
        extends AbstractBackgroundTask<UploadEncryptedDeviceKeyTask.Input, UploadEncryptedDeviceKeyTask.Output> {

    public static class Input {
        public Context context;
    }

    public static class Output {
        public String tokenId;
        public byte[] deviceKeyComponent;
    }

    public UploadEncryptedDeviceKeyTask(BackgroundTaskListener<Output> listener) {
        super(listener);
    }

    @Override
    protected Output doInBackground(Input input) {
        Output result = new Output();
        {
            BankingKeyComponents keyComponents = new BankingKeyComponents();
            keyComponents.generateDeviceKeyComponent();
            result.deviceKeyComponent = keyComponents.deviceKeyComponent;
        }

        try {
            result.tokenId = DeviceKeyApi.uploadDeviceKey(input.context, result.deviceKeyComponent);
        } catch (SecuredRestApiEndpoint.ConnectException e) {
            Log.e(getClass().getSimpleName(),
                    "unable to upload device key, because server is unreachable", e);
            failedReason = R.string.initialization_failed_offline;
            failedAndProcessShouldBeRepeated = true;
            failedCause = e;
            return null;
        } catch (SecuredRestApiEndpoint.IncompatibleClientException e) {
            Log.e(getClass().getSimpleName(),
                    "unable to upload device key, because client is old", e);
            failedReason = R.string.initialization_failed_outdated;
            failedAndProcessShouldBeRepeated = false;
            failedCause = e;
            return null;
        } catch (SecuredRestApiEndpoint.CallFailedException e) {
            Log.e(getClass().getSimpleName(),
                    "unable to upload device key, because of (temporary) I/O problem", e);
            failedReason = R.string.initialization_failed_communication;
            failedAndProcessShouldBeRepeated = true;
            failedCause = e;
            return null;
        }

        return result;
    }
}
