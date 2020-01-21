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
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;

public class DKCharset extends Charset {

    public static final DKCharset INSTANCE = new DKCharset();

    public DKCharset() {
        super("DK", new String[0]);
    }

    @Override
    public boolean contains(Charset cs) {
        return false;
    }

    @Override
    public CharsetDecoder newDecoder() {
        return new Decoder(this);
    }

    @Override
    public CharsetEncoder newEncoder() {
        return new Encoder(this);
    }

    public static class Encoder extends CharsetEncoder {
        public Encoder(Charset charset) {
            super(charset, 1.f, 1.f, new byte[]{'?'});
        }

        @Override
        public boolean isLegalReplacement(byte[] repl) {
            for (byte b : repl) {
                if ((b & 0xff) < 0x20 || 0x7f <= (b&0xff)) {
                    return false;
                }
            }
            return repl.length != 0;
        }

        @Override
        protected CoderResult encodeLoop(CharBuffer in, ByteBuffer out) {
            while (in.hasRemaining()) {
                if (!out.hasRemaining()) {
                    return CoderResult.OVERFLOW;
                }

                char c = in.get();
                switch (c) {
                    case '#':
                        out.put((byte) 0x23);
                        continue;
                    case '€':
                        out.put((byte) 0x24);
                        continue;
                    case '@':
                        out.put((byte) 0x40);
                        continue;
                    case 'Ä':
                        out.put((byte) 0x5b);
                        continue;
                    case 'Ö':
                        out.put((byte) 0x5c);
                        continue;
                    case 'Ü':
                        out.put((byte) 0x5d);
                        continue;
                    case '£':
                        out.put((byte) 0x5e);
                        continue;
                    case '`':
                        out.put((byte) 0x60);
                        continue;
                    case 'ä':
                        out.put((byte) 0x7b);
                        continue;
                    case 'ö':
                        out.put((byte) 0x7c);
                        continue;
                    case 'ü':
                        out.put((byte) 0x7d);
                        continue;
                    case 'ß':
                        out.put((byte) 0x7e);
                        continue;
                }

                int utf16 = (int) c;
                if (0x20 <= utf16 && utf16 <= 0x7f) {
                    out.put((byte) utf16);
                } else {
                    out.put(replacement());
                }
            }
            return CoderResult.UNDERFLOW;
        }
    }

    public static class Decoder extends CharsetDecoder {
        public Decoder(Charset charset) {
            super(charset, 1.f, 1.f);
            replaceWith("?");
        }

        @Override
        protected CoderResult decodeLoop(ByteBuffer in, CharBuffer out) {
            while (in.hasRemaining()) {
                if (!out.hasRemaining()) {
                    return CoderResult.OVERFLOW;
                }

                int b = in.get() & 0xff;
                switch (b) {
                    case 0x23:
                        out.put('#');
                        continue;
                    case 0x24:
                        out.put('€');
                        continue;
                    case 0x40:
                        out.put('@');
                        continue;
                    case 0x5b:
                        out.put('Ä');
                        continue;
                    case 0x5c:
                        out.put('Ö');
                        continue;
                    case 0x5d:
                        out.put('Ü');
                        continue;
                    case 0x5e:
                        out.put('£');
                        continue;
                    case 0x60:
                        out.put('`');
                        continue;
                    case 0x7b:
                        out.put('ä');
                        continue;
                    case 0x7c:
                        out.put('ö');
                        continue;
                    case 0x7d:
                        out.put('ü');
                        continue;
                    case 0x7e:
                        out.put('ß');
                        continue;
                }

                if (0x20 <= b && b < 0x7f) {
                    char ascii = (char) b;
                    out.put(ascii);
                } else {
                    out.put(replacement());
                }
            }
            return CoderResult.UNDERFLOW;
        }
    }
}
