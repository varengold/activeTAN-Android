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

package de.efdis.tangenerator.activetan;

import junit.framework.TestCase;

import org.junit.Test;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class TanGeneratorTest {

    private static final Charset ASCII = Charset.forName("US-ASCII");

    @Test
    public void testDecimalizationExample() {
        // Test vectors taken from the example in RFC 4226
        byte[] hmac_result = new byte[] {
                0x1f, (byte) 0x86, (byte) 0x98, 0x69, 0x0e,
                0x02, (byte) 0xca, 0x16, 0x61, (byte) 0x85,
                0x50, (byte) 0xef, 0x7f, 0x19, (byte) 0xda,
                (byte) 0x8e, (byte) 0x94, 0x5b, 0x55, 0x5a
        };

        TestCase.assertEquals(
                872921,
                TanGenerator.decimalization(hmac_result, 6));
    }

    @Test
    public void testDecimalization() throws NoSuchAlgorithmException, InvalidKeyException {
        // Test vectors taken from Appendix D in RFC 4226
        byte[] secret = "12345678901234567890".getBytes(ASCII);

        int[] expectedHOTP = new int[] {
                755224,
                287082,
                359152,
                969429,
                338314,
                254676,
                287922,
                162583,
                399871,
                520489
        };

        Mac hmacSha1 = Mac.getInstance("HmacSHA1");
        hmacSha1.init(new SecretKeySpec(secret, "RAW"));
        ByteBuffer message = ByteBuffer.allocate(8);
        for (int count = 0; count < expectedHOTP.length; count++) {
            message.clear();
            message.putLong(count);
            byte[] hmac = hmacSha1.doFinal(message.array());

            int hotp = TanGenerator.decimalization(hmac, 6);

            TestCase.assertEquals(expectedHOTP[count], hotp);
        }
    }

}