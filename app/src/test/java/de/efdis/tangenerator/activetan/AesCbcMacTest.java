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

package de.efdis.tangenerator.activetan;

import junit.framework.TestCase;

import org.junit.Test;

import java.security.GeneralSecurityException;
import java.security.Key;
import java.util.Arrays;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class AesCbcMacTest {

    // Test vectors from RFC 4493
    private static final byte[] RFC_TEST_KEY = new byte[]{
            0x2b, 0x7e, 0x15, 0x16, 0x28, (byte) 0xae, (byte) 0xd2, (byte) 0xa6,
            (byte) 0xab, (byte) 0xf7, 0x15, (byte) 0x88, 0x09, (byte) 0xcf, 0x4f, 0x3c
    };
    private static final byte[] RFC_TEST_MESSAGE = new byte[]{
            0x6b, (byte) 0xc1, (byte) 0xbe, (byte) 0xe2, 0x2e, 0x40, (byte) 0x9f, (byte) 0x96,
            (byte) 0xe9, 0x3d, 0x7e, 0x11, 0x73, (byte) 0x93, 0x17, 0x2a,
            (byte) 0xae, 0x2d, (byte) 0x8a, 0x57, 0x1e, 0x03, (byte) 0xac, (byte) 0x9c,
            (byte) 0x9e, (byte) 0xb7, 0x6f, (byte) 0xac, 0x45, (byte) 0xaf, (byte) 0x8e, 0x51,
            0x30, (byte) 0xc8, 0x1c, 0x46, (byte) 0xa3, 0x5c, (byte) 0xe4, 0x11,
            (byte) 0xe5, (byte) 0xfb, (byte) 0xc1, 0x19, 0x1a, 0x0a, 0x52, (byte) 0xef,
            (byte) 0xf6, (byte) 0x9f, 0x24, 0x45, (byte) 0xdf, 0x4f, (byte) 0x9b, 0x17,
            (byte) 0xad, 0x2b, 0x41, 0x7b, (byte) 0xe6, 0x6c, 0x37, 0x10
    };


    private byte[] computeMac(int messageLength) throws GeneralSecurityException {
        Mac algorithm = AesCbcMac.getInstance();
        Key key =  new SecretKeySpec(RFC_TEST_KEY, "AES");
        algorithm.init(key);
        byte[] message = Arrays.copyOf(RFC_TEST_MESSAGE, messageLength);
        algorithm.update(message);
        byte[] mac = algorithm.doFinal();
        return mac;
    }

    @Test
    public void checkEmptyMessage() throws GeneralSecurityException {
        byte[] expectedMac = new byte[] {
                (byte) 0xbb, 0x1d, 0x69, 0x29,
                (byte) 0xe9, 0x59, 0x37, 0x28,
                0x7f, (byte) 0xa3, 0x7d, 0x12,
                (byte) 0x9b, 0x75, 0x67, 0x46
        };
        byte[] actualMac = computeMac(0);

        TestCase.assertTrue(Arrays.equals(expectedMac, actualMac));
    }

    @Test
    public void checkMessageLength16() throws GeneralSecurityException {
        byte[] expectedMac = new byte[] {
                0x07, 0x0a, 0x16, (byte) 0xb4,
                0x6b, 0x4d, 0x41, 0x44,
                (byte) 0xf7, (byte) 0x9b, (byte) 0xdd, (byte) 0x9d,
                (byte) 0xd0, 0x4a, 0x28, 0x7c
        };
        byte[] actualMac = computeMac(16);

        TestCase.assertTrue(Arrays.equals(expectedMac, actualMac));
    }

    @Test
    public void checkMessageLength40() throws GeneralSecurityException {
        byte[] expectedMac = new byte[] {
                (byte) 0xdf, (byte) 0xa6, 0x67, 0x47,
                (byte) 0xde, (byte) 0x9a, (byte) 0xe6, 0x30,
                0x30, (byte) 0xca, 0x32, 0x61,
                0x14, (byte) 0x97, (byte) 0xc8, 0x27
        };
        byte[] actualMac = computeMac(40);

        TestCase.assertTrue(Arrays.equals(expectedMac, actualMac));
    }

    @Test
    public void checkMessageLength64() throws GeneralSecurityException {
        byte[] expectedMac = new byte[] {
                0x51, (byte) 0xf0, (byte) 0xbe, (byte) 0xbf,
                0x7e, 0x3b, (byte) 0x9d, (byte) 0x92,
                (byte) 0xfc, 0x49, 0x74, 0x17,
                0x79, 0x36, 0x3c, (byte) 0xfe
        };
        byte[] actualMac = computeMac(64);

        TestCase.assertTrue(Arrays.equals(expectedMac, actualMac));
    }

}
