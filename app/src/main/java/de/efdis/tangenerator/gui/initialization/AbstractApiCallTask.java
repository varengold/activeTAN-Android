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

import android.content.Context;
import android.os.Build;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.security.spec.X509EncodedKeySpec;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.net.ssl.HttpsURLConnection;

import de.efdis.tangenerator.BuildConfig;
import de.efdis.tangenerator.R;

/**
 * API calls are sent over https using a simple REST api.
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
public abstract class AbstractApiCallTask<INPUT, OUTPUT>
        extends AbstractBackgroundTask<INPUT, OUTPUT> {
    /** Algorithm used to encrypt the uploaded data of a POST request. */
    private static final String API_UPLOAD_ALGORITHM = KeyProperties.KEY_ALGORITHM_RSA;
    private static final String API_UPLOAD_CIPHER =
            API_UPLOAD_ALGORITHM + "/ECB/OAEPPadding";
    /** Algorithm used to sign the encrypted request and response data payload. */
    private static final String API_DOWNLOAD_SIGNATURE = "SHA512withRSA";
    /** Response HTTP header field which contains the signature in BASE64 encoding. */
    private static final String API_SIGNATURE_HEADER = "X-Signature";
    private static final String API_USER_AGENT =
            BuildConfig.APPLICATION_ID + "/" + BuildConfig.VERSION_CODE +
                    " Android/" + Build.VERSION.SDK_INT;

    /** HTTPS URL for the REST api call */
    private final URL apiUrl;
    /** Key used to encrypt the request data and verify the api response. */
    private final PublicKey apiKey;

    private HttpsURLConnection connection;
    private Cipher cipher;
    private Signature signature;

    public static class CallFailedException extends Exception {
        public CallFailedException(String message) {
            super(message);
        }

        public CallFailedException(String message, Exception reason) {
            super(message, reason);
        }
    }

    public static class OutdatedClientException extends CallFailedException {
        public OutdatedClientException(String message) {
            super(message);
        }
    }

    public AbstractApiCallTask(BackgroundTaskListener<OUTPUT> listener, URL apiUrl, Context context) {
        super(listener);
        this.apiUrl = apiUrl;
        try {
            this.apiKey = loadApiKey(context);
        } catch (IOException | NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new RuntimeException("cannot load api key");
        }
    }

    private static PublicKey loadApiKey(Context context)
            throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
        KeyFactory keyFactory = KeyFactory.getInstance(API_UPLOAD_ALGORITHM);

        InputStream in = context.getResources().openRawResource(R.raw.api_key);
        BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.US_ASCII));
        ByteBuffer encodedKey = ByteBuffer.allocate(in.available());
        try {
            while (reader.ready()) {
                String line = reader.readLine();
                if ("-----BEGIN PUBLIC KEY-----".equals(line)) {
                    continue;
                }
                if ("-----END PUBLIC KEY-----".equals(line)) {
                    break;
                }
                encodedKey.put(Base64.decode(line, Base64.DEFAULT));
            }
        } finally {
            reader.close();
        }

        encodedKey.flip();
        encodedKey = encodedKey.slice();

        KeySpec keySpec = new X509EncodedKeySpec(encodedKey.array());
        PublicKey publicKey = keyFactory.generatePublic(keySpec);
        return publicKey;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        prepareConnection();
        prepareCrypto();
    }

    private void prepareConnection() {
        try {
            URLConnection urlConnection = apiUrl.openConnection();
            if (urlConnection instanceof HttpsURLConnection) {
                connection = (HttpsURLConnection) urlConnection;
            } else {
                Log.e(getClass().getSimpleName(),
                        "communication is only allowed via HTTPS for server verification");
                return;
            }
        } catch (IOException e) {
            Log.e(getClass().getSimpleName(),
                    "unable to initialize connection for API URL", e);
            return;
        }

        connection.setAllowUserInteraction(false);
        connection.setUseCaches(false);
        connection.setConnectTimeout(10000);
        connection.setReadTimeout(10000);
        connection.setRequestProperty("User-Agent", API_USER_AGENT);
    }

    private void prepareCrypto() {
        try {
            cipher = Cipher.getInstance(API_UPLOAD_CIPHER);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            throw new RuntimeException("api cipher unusable", e);
        }
        try {
            cipher.init(Cipher.ENCRYPT_MODE, apiKey);
        } catch (InvalidKeyException e) {
            throw new RuntimeException("api key unusable for encryption", e);
        }

        try {
            signature = Signature.getInstance(API_DOWNLOAD_SIGNATURE);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("api signature unusable", e);
        }
        try {
            signature.initVerify(apiKey);
        } catch (InvalidKeyException e) {
            throw new RuntimeException("api key unusable for signature verification", e);
        }
    }

    private byte[] performRequest(byte[] postData) throws IOException, CallFailedException {
        if (connection == null) {
            throw new CallFailedException("connection has not be initialized");
        }

        if (postData == null) {
            connection.setRequestMethod("GET");
        } else {
            connection.setDoOutput(true);

            connection.setRequestMethod("POST");

            connection.setRequestProperty("Content-Type", "application/octet-stream");

            OutputStream os;
            try {
                os = connection.getOutputStream();
            } catch (IOException e) {
                throw new ConnectException("I/O problem during connect: " + e.getMessage());
            }
            try {
                byte[] encryptedPostData = cipher.doFinal(postData);
                os.write(encryptedPostData);
                signature.update(encryptedPostData);
            } catch (IllegalBlockSizeException | BadPaddingException e) {
                throw new IOException("could not encrypt request data", e);
            } catch (SignatureException e) {
                throw new IOException("could not verify request data", e);
            } finally {
                os.close();
            }
        }

        try {
            connection.connect();
        } catch (IOException e) {
            throw new ConnectException("I/O problem during connect: " + e.getMessage());
        }

        switch (connection.getResponseCode()) {
            case HttpURLConnection.HTTP_OK:
            case HttpURLConnection.HTTP_CREATED:
                // success
                break;

            case HttpURLConnection.HTTP_BAD_REQUEST:
            case HttpURLConnection.HTTP_FORBIDDEN:
            case HttpURLConnection.HTTP_GONE:
            case HttpURLConnection.HTTP_UNSUPPORTED_TYPE:
                throw new OutdatedClientException("HTTP request did not succeed, " +
                        "this client should be updated");

            default:
                throw new CallFailedException(
                        "HTTP request did not succeed, response code = "
                                + connection.getResponseCode());
        }

        byte[] responseData = new byte[Math.max(0, connection.getContentLength())];
        if (responseData.length > 0) {
            InputStream is = connection.getInputStream();
            if (responseData.length != is.read(responseData)) {
                throw new IOException(
                        "unexpected end of content stream");
            }
            is.close();
        }

        byte[] sigData;
        {
            String sigBase64 = connection.getHeaderField(API_SIGNATURE_HEADER);
            if (sigBase64 == null) {
                throw new CallFailedException("missing signature for api response");
            }
            try {
                sigData = Base64.decode(sigBase64, Base64.DEFAULT);
            } catch (IllegalArgumentException e) {
                throw new CallFailedException("illegal format of api response signature", e);
            }
        }

        try {
            signature.update(responseData);

            if (!signature.verify(sigData)) {
                throw new CallFailedException("invalid api response signature");
            }
        } catch (SignatureException e) {
            throw new IOException("unable to verify api response signature", e);
        }

        return responseData;
    }

    protected byte[] performGetRequest() throws IOException, CallFailedException {
        return performRequest(null);
    }

    protected byte[] performPostRequest(byte[] requestData) throws IOException, CallFailedException {
        return performRequest(requestData);
    }

}
