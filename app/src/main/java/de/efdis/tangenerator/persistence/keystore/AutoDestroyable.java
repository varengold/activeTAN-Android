package de.efdis.tangenerator.persistence.keystore;

import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;

import javax.crypto.SecretKey;
import javax.security.auth.DestroyFailedException;
import javax.security.auth.Destroyable;

/** Utility class to clear or unreference sensitive key information from memory */
public class AutoDestroyable<K extends SecretKey> implements AutoCloseable, Destroyable {

    private static final String TAG = AutoDestroyable.class.getSimpleName();

    private K keyMaterial;

    private boolean destroyed;

    public AutoDestroyable(@NonNull K keyMaterial) {
        this.keyMaterial = keyMaterial;
    }

    public K getKeyMaterial() {
        if (destroyed) {
            throw new IllegalStateException("Usage after destruction of key material");
        } else {
            return keyMaterial;
        }
    }

    @Override
    public boolean isDestroyed() {
        if (destroyed) {
            return true;
        }

        // Up to and including API 25 SecretKey did not implement the Destroyable interface
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return keyMaterial.isDestroyed();
        } else {
            return false;
        }
    }

    @Override
    public void destroy() {
        if (destroyed) {
            return;
        }

        // Up to and including API 25 SecretKey did not implement the Destroyable interface
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                keyMaterial.destroy();
            } catch (DestroyFailedException e) {
                // The SecretKey implementation of the Android keystore does not override the
                // default implementation of the destroy() method.
                // AFAIK, only password protected keystores have a destroy() implementation, which
                // erases the password from memory.
                // Thus, it is not possible to unreference or destroy the key material and we can
                // ignore the exception.
                Log.d(TAG, "Could not destroy key material after usage", e);
            }
        }

        // Unreference the key material in this wrapper to prevent further usage w/ getKeyMaterial()
        keyMaterial = null;
        destroyed = true;
    }

    @Override
    public void close() {
        destroy();
    }

}
