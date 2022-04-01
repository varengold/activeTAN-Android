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

public class XorChecksum implements Checksum {
    private int sum;

    public XorChecksum() {
        reset();
    }

    @Override
    public void reset() {
        sum = 0;
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
    public void update(int b) {
        sum ^= b;
    }

    @Override
    public long getValue() {
        int firstNibble = (sum & 0xf0) >> 4;
        int secondNibble = sum & 0x0f;
        return (firstNibble ^ secondNibble);
    }
}
