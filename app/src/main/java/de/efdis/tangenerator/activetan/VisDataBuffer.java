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

import java.io.ByteArrayOutputStream;
import java.security.MessageDigest;
import java.util.Arrays;

public class VisDataBuffer {

    private static final int FIELD_SEPARATOR = 0xe1;
    private static final int START_CODE_SEPARATOR = 0xe0;
    private static final int MAX_DATABLOCK_LENGTH = 12;
    private static final int MAX_HASH_LENGTH = 29;

    private final ByteArrayOutputStream content;

    public VisDataBuffer() {
        content = new ByteArrayOutputStream();
    }

    public void write(byte[] data) {
        content.write(data, 0, data.length);
    }

    public void write(int b) {
        content.write(b);
    }

    public void write(String text) {
        byte[] iso646 = text.getBytes(DKCharset.INSTANCE);
        write(iso646);
    }

    public void write(HHDuc hhduc) {
        int numDataBlocks = 0;

        write(FIELD_SEPARATOR);
        write("Start-Code:");
        numDataBlocks ++;

        write(START_CODE_SEPARATOR);
        write(hhduc.getStartCode());
        numDataBlocks ++;

        if (hhduc.getVisualisationClass() != null) {
            write(FIELD_SEPARATOR);
            write(hhduc.getVisualisationClass().getVisDataLine1());
            numDataBlocks++;

            if (!hhduc.getVisualisationClass().getVisDataLine2().isEmpty()) {
                write(FIELD_SEPARATOR);
                write(hhduc.getVisualisationClass().getVisDataLine2());
                numDataBlocks++;
            }
        }

        for (DataElementType dataElementType : hhduc.getDataElementTypes()) {
            String label = dataElementType.getVisDataLine1();
            String value = hhduc.getDataElement(dataElementType);

            for (int i = 0; i < value.length(); i+= MAX_DATABLOCK_LENGTH) {
                if (value.length() > MAX_DATABLOCK_LENGTH) {
                    while (label.length() < MAX_DATABLOCK_LENGTH - 1) {
                        label += " ";
                    }
                    label = label.substring(0, MAX_DATABLOCK_LENGTH - 1)
                            + (i / MAX_DATABLOCK_LENGTH + 1);
                }

                write(FIELD_SEPARATOR);
                write(label);
                numDataBlocks ++;

                write(FIELD_SEPARATOR);
                write(value.substring(i, Math.min(value.length(), i + MAX_DATABLOCK_LENGTH)));
                numDataBlocks ++;
            }
        }

        if (numDataBlocks < 0x0f) {
            write(0xb0 | numDataBlocks);
        } else {
            write(0xbf);
            write(numDataBlocks);
        }
    }

    public byte[] getHash(MessageDigest algorithm) {
        algorithm.reset();
        byte[] digest = algorithm.digest(content.toByteArray());

        if (digest.length > MAX_HASH_LENGTH) {
            digest = Arrays.copyOf(digest, MAX_HASH_LENGTH);
        }

        return digest;
    }

}
