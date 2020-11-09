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

package de.efdis.tangenerator.api;

import android.content.Context;

import java.nio.charset.StandardCharsets;

/**
 * Internet API of the banking system to upload device keys during initialization of the app.
 * <p/>
 * API calls are sent over HTTPS using a simple REST api.
 * <p/>
 * To provide an extra layer of data protection, a public key for the backend is compiled into this
 * app, which secures the data on the application level.
 * <ul>
 *     <li>
 *         Request data is encrypted. This is particularly important since we upload sensitive data
 *         to the server: the device key component for the generated security token.
 *     </li>
 *     <li>
 *         Request and response data is signed by the server. This way we know that the app is
 *         connected to the correct backend and no “man in the middle” has compromised the network
 *         infrastructure and trusted CA certificates.
 *     </li>
 * </ul>
 */
public class DeviceKeyApi {

    /**
     * Sends the device key component to the banking system and receives a token ID for
     * identification of this device.
     * <p/>
     * The uploaded data is encrypted and communication uses HTTPS.
     *
     * @param deviceKey AES key component
     * @return token ID
     */
    public static String uploadDeviceKey(Context context, byte[] deviceKey)
            throws SecuredRestApiEndpoint.CallFailedException {
        SecuredRestApiEndpoint apiEndpoint = new SecuredRestApiEndpoint(context);
        byte[] rawTokenId = apiEndpoint.performRequest(deviceKey);
        return new String(rawTokenId, StandardCharsets.UTF_8);
    }

}
