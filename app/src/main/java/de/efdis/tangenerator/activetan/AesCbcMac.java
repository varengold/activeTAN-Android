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

import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Security;
import java.security.spec.AlgorithmParameterSpec;
import java.util.Arrays;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.Mac;
import javax.crypto.MacSpi;
import javax.crypto.spec.IvParameterSpec;

/**
 * Implementation of RFC 4493
 */
public final class AesCbcMac extends MacSpi {
    public static final String ALGORITHM = "AESMAC";

    public static final class Provider extends java.security.Provider {
        public static final String NAME = "AesCbcMac.Provider";

        public Provider() {
            super(NAME, 1.0,
                    "Implementing RFC 4493");

            put("Mac." + ALGORITHM, AesCbcMac.class.getName());
        }
    }

    public static Mac getInstance() {
        try {
            return Mac.getInstance(ALGORITHM, Provider.NAME);
        } catch (NoSuchAlgorithmException | NoSuchProviderException e) {
            Security.addProvider(new Provider());
            return getInstance();
        }
    }

    private Cipher blockCipher;

    private Key key;
    private byte[] subKey1;
    private byte[] subKey2;

    private ByteBuffer inputBuffer = ByteBuffer.allocate(1024);

    @Override
    protected void engineInit(Key key, AlgorithmParameterSpec params) throws InvalidKeyException, InvalidAlgorithmParameterException {
        if (key == null) {
            throw new InvalidKeyException("No key provided");
        }

        try {
            blockCipher = Cipher.getInstance("AES/CBC/NoPadding");
        } catch (GeneralSecurityException e) {
            throw new InvalidKeyException(
                    "Cannot initialize AES cipher", e);
        }

        this.key = key;
        try {
            engineReset();
        } catch (RuntimeException e) {
            throw new InvalidKeyException(
                    "Cannot initialize AES cipher with key provided", e);
        }

        // AES-Encryption of 128-bit zeros
        byte[] l;
        try {
            l = blockCipher.doFinal(new byte[16]);
        } catch (IllegalBlockSizeException | BadPaddingException e) {
            throw new InvalidAlgorithmParameterException(
                    "Block size of AES is always 128 bit", e);
        }

        // First sub key for final step
        subKey1 = l.clone();
        leftShift(subKey1);
        if ((l[0] & 0x80) != 0) {
            subKey1[subKey1.length - 1] ^= 0x87;
        }

        // Second sub key for final step
        subKey2 = subKey1.clone();
        leftShift(subKey2);
        if ((subKey1[0] & 0x80) != 0) {
            subKey2[subKey2.length - 1] ^= 0x87;
        }

        engineReset();
    }

    /**
     * Left shift the binary data by 1 bit.
     * <p/>
     * The most significant bit of <code>data[0]</code> will be lost.
     * A new bit <code>false</code> is added as least significant bit of
     * <code>data[data.length - 1]</code>.
     *
     * @param data
     *      The binary data, which is modified in place.
     */
    private static void leftShift(byte[] data) {
        int carry = 0;
        for (int i = data.length - 1; i >= 0; i--) {
            int shiftedByte = ((data[i] & 0xff) << 1) | carry;
            carry = (shiftedByte & 0x100) >> 8;
            shiftedByte = shiftedByte & 0xff;
            data[i] = (byte) shiftedByte;
        }
        // final carry (= most significant bit) is discarded
    }

    @Override
    protected void engineReset() {
        inputBuffer.clear();

        // Without reinitialization, the blockCipher would
        // create an exception "IV has already been used."
        // after the first use of doFinal.
        try {
            blockCipher.init(Cipher.ENCRYPT_MODE, key,
                    new IvParameterSpec(new byte[blockCipher.getBlockSize()]));
        } catch (GeneralSecurityException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected int engineGetMacLength() {
        return blockCipher.getOutputSize(blockCipher.getBlockSize());
    }

    @Override
    protected void engineUpdate(byte input) {
        inputBuffer.put(input);
        processBuffer();
    }

    @Override
    protected void engineUpdate(ByteBuffer input) {
        while (inputBuffer.remaining() < input.remaining()) {
            // prevent buffer overflow
            byte[] partialData = new byte[inputBuffer.remaining()];
            input.get(partialData);
            inputBuffer.put(partialData);
            processBuffer();
        }

        inputBuffer.put(input);
        processBuffer();
    }

    @Override
    protected void engineUpdate(byte[] input, int offset, int len) {
        while (inputBuffer.remaining() < len) {
            // prevent buffer overflow
            byte[] partialData = Arrays.copyOfRange(input, offset, inputBuffer.remaining());
            offset += partialData.length;
            len -= partialData.length;
            inputBuffer.put(partialData);
            processBuffer();
        }

        inputBuffer.put(input, offset, len);
        processBuffer();
    }

    private void processBuffer() {
        if (inputBuffer.position() <= blockCipher.getBlockSize()) {
            // keep the last block for processing in #engineDoFinal.
            return;
        }

        inputBuffer.flip();

        byte[] block = new byte[blockCipher.getBlockSize()];
        while (inputBuffer.remaining() > blockCipher.getBlockSize()) {
            inputBuffer.get(block);
            blockCipher.update(block);
        }

        inputBuffer.compact();
    }

    @Override
    protected byte[] engineDoFinal() {
        // Choose sub key for final block
        byte[] subKey;
        if (inputBuffer.position() == blockCipher.getBlockSize()) {
            subKey = subKey1;
        } else {
            subKey = subKey2;

            // Padding of final block
            if (inputBuffer.position() < blockCipher.getBlockSize()) {
                inputBuffer.put((byte) 0x80);
                while (inputBuffer.position() < blockCipher.getBlockSize()) {
                    inputBuffer.put((byte) 0x00);
                }
            }

            assert inputBuffer.position() == blockCipher.getBlockSize();
        }

        byte[] finalBlock = new byte[blockCipher.getBlockSize()];
        inputBuffer.flip();
        inputBuffer.get(finalBlock);
        inputBuffer.compact();

        for (int i = 0; i < finalBlock.length; i++) {
            finalBlock[i] ^= subKey[i];
        }

        try {
            return blockCipher.doFinal(finalBlock);
        } catch (IllegalBlockSizeException | BadPaddingException e) {
            // Should not happen, b/c only complete blocks are processed
            throw new RuntimeException(e);
        }
    }
}
