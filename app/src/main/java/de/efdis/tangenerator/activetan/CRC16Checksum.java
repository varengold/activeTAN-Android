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

import java.util.zip.Checksum;

public class CRC16Checksum implements Checksum {
    /** XOR lookup table */
    private static short[] table;

    /** Polynomial x^16 + x^15 + x^2 + 1 with LSB */
    private static final int DIVISOR = 0xa001;

    private final int initialValue;

    private int crc;

    public CRC16Checksum(int initialValue) {
        this.initialValue = initialValue;
        initializeLookupTable();
        reset();
    }

    private static synchronized void initializeLookupTable() {
        if (table == null) {
            table = new short[256];
            for (int idx = 0; idx < table.length; idx++) {
                int value = idx;
                for (int bit = 0; bit < 8; bit++) {
                    if ((value & 1) != 0)
                        value = (value >> 1) ^ DIVISOR;
                    else
                        value = (value >> 1);
                }
                table[idx] = (short) value;
            }
        }
    }

    @Override
    public void reset() {
        crc = initialValue;
    }

    @Override
    public void update(int b) {
        crc = (crc >> 8) ^ (table[(crc ^ b) & 0xff] & 0xffff);
    }

    @Override
    public void update(byte[] b, int off, int len) {
        int start = Math.max(0, off);
        int end = Math.min(b.length, off + len);
        for (int idx = start; idx < end; idx++) {
            update(b[idx] & 0xff);
        }
    }

    @Override
    public long getValue() {
        return crc;
    }
}
