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

public enum FieldEncoding {
    BCD, ASCII;


    public static long bcdDecode(byte[] data) throws NumberFormatException {
        long result = 0;

        for (int i = 0; i < data.length; i++) {
            int firstNibble = (data[i] & 0xf0) >> 4;
            int secondNibble = (data[i] & 0x0f);

            if (firstNibble > 9) {
                throw new NumberFormatException(
                        "Illegal value in first half-byte of BCD coded number");
            } else {
                result = result * 10 + firstNibble;
            }

            if (secondNibble > 9) {
                if (secondNibble == 0xf && i == data.length - 1) {
                    // end of number
                    break;
                }

                throw new NumberFormatException(
                        "Illegal value in second half-byte of BCD coded number");
            } else {
                result = result * 10 + secondNibble;
            }
        }

        return result;
    }

    public static byte[] bcdEncode(String number) {
        int[] digits;
        if (number.length() % 2 == 0) {
            digits = new int[number.length()];
        } else {
            digits = new int[number.length() + 1];
            digits[digits.length - 1] = 0xf;
        }

        for (int i = 0; i < number.length(); i++) {
            int digit = number.charAt(i) - '0';
            digits[i] = digit;
        }

        byte[] result = new byte[digits.length / 2];
        for (int i = 0; i < result.length; i++) {
            int firstNibble = digits[2 * i];
            int secondNibble = digits[2 * i + 1];
            result[i] = (byte) ((firstNibble << 4) | secondNibble);
        }

        return result;
    }
}
