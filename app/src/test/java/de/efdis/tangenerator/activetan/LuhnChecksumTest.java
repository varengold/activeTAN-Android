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

public class LuhnChecksumTest {
    @Test
    public void oneDatablockExample() {
        // Example data from
        // Specifications of the Secorder 3G version 1.2,
        // Secoder Application chipTAN version Identifier 4,
        // page 128
        byte[] data = new byte[] {
            0x01,
            0x20, (byte)0x82, (byte)0x90, 0x19, (byte)0x98,
            0x49, 0x45, 0x39, 0x39, 0x42, 0x4f,
            0x46, 0x49
        };

        LuhnChecksum luhn = new LuhnChecksum();
        luhn.update(data, 0, data.length);

        TestCase.assertEquals(2, luhn.getValue());
    }

    @Test
    public void twoDatablocksExample() throws HHDuc.UnsupportedDataFormatException {
        // Example data from
        // HandHeld-Device (HHD) zur TAN-Erzeugung,
        // HHD-Erweiterung für optische Schnittstellen Version V 1.5.1,
        // page 31
        byte[] data = new byte[] {
                0x01,
                0x38, 0x32, 0x31, 0x31, 0x32, 0x33, 0x34, 0x35,
                0x30, 0x31, 0x32, 0x33, 0x34, 0x35, 0x36, 0x37, 0x38, 0x39,
                0x31, 0x30, 0x30, 0x2c, 0x30, 0x30,
        };

        LuhnChecksum luhn = new LuhnChecksum();
        luhn.update(data, 0, data.length);

        TestCase.assertEquals(0, luhn.getValue());
    }
}
