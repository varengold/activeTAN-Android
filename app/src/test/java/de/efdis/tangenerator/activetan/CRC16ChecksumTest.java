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

public class CRC16ChecksumTest {

    @Test
    public void bqrExample() {
        byte[] data = new byte[]{
                0x44, 0x4b, 0x4e, 0x1d, (byte) 0xc8, 0x01, 0x38, 0x32, 0x31, 0x31, 0x32, 0x33, 0x34,
                0x35, 0x4a, 0x30, 0x31, 0x32, 0x33, 0x34, 0x35, 0x36, 0x37, 0x38, 0x39, 0x46, 0x31,
                0x30, 0x30, 0x2c, 0x30, 0x30, 0x02, 0x42, 0x35
        };

        long expectedChecksum = (data[data.length - 2] & 0xff) << 8 | (data[data.length - 1] & 0xff);

        CRC16Checksum crc16 = new CRC16Checksum(0);
        crc16.update(data, 0, data.length - 2);
        long actualChecksum = crc16.getValue();

        TestCase.assertEquals(expectedChecksum, actualChecksum);
    }

}
