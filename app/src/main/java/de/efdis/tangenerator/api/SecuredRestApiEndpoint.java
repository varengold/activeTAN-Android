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
import android.os.Build;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import android.util.Log;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLConnection;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.cert.CertPathValidatorException;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPublicKeySpec;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLHandshakeException;

import de.efdis.tangenerator.BuildConfig;
import de.efdis.tangenerator.R;

/**
 * Handles connection (HTTPS) and encryption for a rest api endpoint.
 */
public class SecuredRestApiEndpoint {

    /**
     * Algorithm used to encrypt the uploaded data of a POST request.
     */
    private static final String API_UPLOAD_ALGORITHM = KeyProperties.KEY_ALGORITHM_RSA;
    private static final String API_UPLOAD_CIPHER =
            API_UPLOAD_ALGORITHM + "/ECB/OAEPPadding";

    /**
     * Algorithm used to sign the encrypted request and response data payload.
     */
    private static final String API_DOWNLOAD_SIGNATURE = "SHA512withRSA";

    /**
     * Response HTTP header field which contains the signature in BASE64 encoding.
     */
    private static final String API_SIGNATURE_HEADER = "X-Signature";

    private static final String API_USER_AGENT =
            BuildConfig.APPLICATION_ID + "/" + BuildConfig.VERSION_CODE +
                    " Android/" + Build.VERSION.SDK_INT;

    /**
     * HTTPS URL for the REST api endpoint
     */
    private final URL apiUrl;

    /**
     * Key used to encrypt the request data and verify the api response.
     */
    private final PublicKey apiKey;

    /**
     * Cipher to encrypt the request data
     */
    private Cipher cipher;

    /**
     * Signature to verify the response data
     */
    private Signature signature;

    SecuredRestApiEndpoint(Context context, int backendId) {
        String[] apiUrls = context.getResources().getStringArray(R.array.backend_api_url);
        if (backendId < 0 || backendId >= apiUrls.length) {
            throw new ApiConfigurationException("Invalid backend ID");
        }
        try {
            this.apiUrl = new URL(apiUrls[backendId]);
        } catch (MalformedURLException e) {
            throw new ApiConfigurationException("Invalid api URL", e);
        }
        try {
            this.apiKey = loadApiKey(context);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new ApiConfigurationException("Cannot load api key", e);
        }
    }

    private static PublicKey loadApiKey(Context context)
            throws NoSuchAlgorithmException, InvalidKeySpecException {
        KeyFactory keyFactory = KeyFactory.getInstance(API_UPLOAD_ALGORITHM);

        RSAPublicKeySpec keySpec = new RSAPublicKeySpec(
                new BigInteger(context.getResources().getString(
                        R.string.backend_api_key_modulus)),
                new BigInteger(context.getResources().getString(
                        R.string.backend_api_key_exponent)));

        PublicKey publicKey = keyFactory.generatePublic(keySpec);
        return publicKey;
    }

    /**
     * Set up a new HTTPS connection for a following api call.
     * <p/>
     * This method does not yet connect to the server.
     *
     * @return Connection to the rest api endpoint.
     */
    @NonNull
    private HttpsURLConnection prepareConnection() {
        HttpsURLConnection connection;
        try {
            URLConnection urlConnection = apiUrl.openConnection();
            if (urlConnection instanceof HttpsURLConnection) {
                connection = (HttpsURLConnection) urlConnection;
            } else {
                throw new ApiConfigurationException(
                        "communication is only allowed via HTTPS for server verification");
            }
        } catch (IOException e) {
            throw new ApiConfigurationException(
                    "unable to initialize connection for api URL", e);
        }

        connection.setAllowUserInteraction(false);
        connection.setUseCaches(false);
        connection.setConnectTimeout(10000);
        connection.setReadTimeout(10000);
        connection.setRequestProperty("User-Agent", API_USER_AGENT);

        // Prepare API request
        connection.setRequestProperty("Accept", "text/*");
        connection.setRequestProperty("Content-Type", "application/octet-stream");
        connection.setDoOutput(true);
        try {
            connection.setRequestMethod("POST");
        } catch (ProtocolException e) {
            throw new ApiConfigurationException("POST method not supported by HTTP", e);
        }

        return connection;
    }

    private void prepareCrypto() {
        try {
            cipher = Cipher.getInstance(API_UPLOAD_CIPHER);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            throw new ApiConfigurationException("api cipher unusable", e);
        }
        try {
            cipher.init(Cipher.ENCRYPT_MODE, apiKey);
        } catch (InvalidKeyException e) {
            throw new ApiConfigurationException("api key unusable for encryption", e);
        }

        try {
            signature = Signature.getInstance(API_DOWNLOAD_SIGNATURE);
        } catch (NoSuchAlgorithmException e) {
            throw new ApiConfigurationException("api signature unusable", e);
        }
        try {
            signature.initVerify(apiKey);
        } catch (InvalidKeyException e) {
            throw new ApiConfigurationException("api key unusable for signature verification", e);
        }
    }

    private HttpsURLConnection prepareAndOpenConnection() throws IOException {
        IOException connectionError = null;

        for (int retryCount = 0; connectionError == null || retryCount < 5; retryCount++) {
            if (retryCount > 0) {
                Log.i(getClass().getSimpleName(),
                        "Retrying connection automatically ...");
                try {
                    Thread.sleep(500);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new IOException("Reconnect has been interrupted", ie);
                }
            }

            HttpsURLConnection connection = prepareConnection();

            try {
                connection.connect();
                return connection;
            } catch (IOException e) {
                Log.e(getClass().getSimpleName(), "Connection failed", e);
                connectionError = e;
            }

            connection.disconnect();

            /*
             * During the first API call, it happens quite often that the OCSP server
             * responds with 'tryLater' or not at all.
             * We can easily fix that for the user by attempting a new connection.
             */
            if (connectionError instanceof SSLHandshakeException
                    && connectionError.getCause() instanceof CertificateException
                    && connectionError.getCause().getCause() instanceof CertPathValidatorException) {
                // OCSP response error: TRY_LATER or UNKNOWN
                continue;
            }

            // Don't retry for other connection errors
            break;
        }

        // Rethrow connection error after last retry
        throw connectionError;
    }

    public synchronized byte[] performRequest(@NonNull byte[] postData)
            throws CallFailedException {
        prepareCrypto();
        HttpsURLConnection connection;
        try {
            connection = prepareAndOpenConnection();
        } catch (IOException e) {
            throw new ConnectException("Connecting to api failed", e);
        }
        try {
            try (OutputStream os = connection.getOutputStream()) {
                byte[] encryptedPostData = cipher.doFinal(postData);
                os.write(encryptedPostData);
                signature.update(encryptedPostData);
            } catch (IllegalBlockSizeException | BadPaddingException e) {
                throw new ApiConfigurationException("Could not encrypt request data", e);
            } catch (SignatureException e) {
                throw new ApiConfigurationException("Could not verify request data", e);
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
                    throw new IncompatibleClientException(
                            "HTTP request did not succeed, this client should be updated");

                case 429: // Too many requests
                    throw new CallFailedException(
                            "Backend is busy, try again later");

                default:
                    throw new IOException(
                            "HTTP request did not succeed, response code = "
                                    + connection.getResponseCode());
            }

            byte[] responseData = new byte[Math.max(0, connection.getContentLength())];
            if (responseData.length > 0) {
                try (InputStream is = connection.getInputStream()) {
                    if (responseData.length != is.read(responseData)) {
                        throw new IOException(
                                "Unexpected end of content stream");
                    }
                }
            }

            byte[] sigData;
            {
                String sigBase64 = connection.getHeaderField(API_SIGNATURE_HEADER);
                if (sigBase64 == null) {
                    throw new IOException("Missing signature for api response");
                }
                try {
                    sigData = Base64.decode(sigBase64, Base64.DEFAULT);
                } catch (IllegalArgumentException e) {
                    throw new IOException("Illegal format of api response signature", e);
                }
            }

            try {
                signature.update(responseData);
            } catch (SignatureException e) {
                throw new ApiConfigurationException("Unable to verify api response data");
            }

            try {
                if (!signature.verify(sigData)) {
                    throw new IOException("Invalid api response signature");
                }
            } catch (SignatureException e) {
                throw new IOException("Unable to verify api response signature", e);
            }

            return responseData;
        } catch (IOException e) {
            throw new CallFailedException(
                    "Connection error or illegal response during api call", e);
        } finally {
            connection.disconnect();
        }
    }

    /**
     * Exception that is caused by a wrong configuration of the API endpoint and not caused
     * by a runtime problem.
     * <p/>
     * This class is derived from {@link RuntimeException}, because we assume that it only
     * occurs during development and not after having tested the app anymore.
     */
    public static class ApiConfigurationException extends RuntimeException {
        public ApiConfigurationException(String message) {
            super(message);
        }

        public ApiConfigurationException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * Exception that a request was not successful.
     */
    public static class CallFailedException extends Exception {
        public CallFailedException(String message) {
            super(message);
        }

        public CallFailedException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * Exception that it was not possible to connect to the api.
     */
    public static class ConnectException extends CallFailedException {
        public ConnectException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * Exception that the client is not compatible with the rest api endpoint anymore.
     */
    public static class IncompatibleClientException extends CallFailedException {
        public IncompatibleClientException(String message) {
            super(message);
        }
    }
}
