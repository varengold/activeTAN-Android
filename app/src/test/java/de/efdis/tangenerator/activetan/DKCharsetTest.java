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

import java.util.Arrays;

public class DKCharsetTest {

    @Test
    public void encodeNumbers() {
        TestCase.assertTrue(Arrays.equals(new byte[] {0x31, 0x30, 0x30, 0x2c, 0x30, 0x30},
                "100,00".getBytes(DKCharset.INSTANCE)));
    }

    @Test
    public void decodeNumbers() {
        TestCase.assertEquals("100,00",
                new String(new byte[] {0x31, 0x30, 0x30, 0x2c, 0x30, 0x30}, DKCharset.INSTANCE));
    }

    @Test
    public void encodeDecodeAllValidCharacters() {
        String validCharacters = " !\"#€%&'()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZÄÖÜ£_`abcdefghijklmnopqrstuvwxyzäöüß";
        TestCase.assertEquals(validCharacters,
                new String(validCharacters.getBytes(DKCharset.INSTANCE), DKCharset.INSTANCE));
    }

    @Test
    public void invalidCharacters() {
        TestCase.assertEquals("?",
                new String("´".getBytes(DKCharset.INSTANCE), DKCharset.INSTANCE));
    }

    @Test
    public void invalidEncoding() {
        TestCase.assertEquals("?",
                new String(new byte[]{0x00}, DKCharset.INSTANCE));
    }

}
