package de.hts.web;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Kleiner QR-Code-Erzeuger (Byte-Modus, Fehlerkorrektur M, Versionen 1–10) für die Adressen der Handy-Seiten – ohne
 * fremde Bibliothek. Aufbau nach ISO/IEC 18004 (Reed-Solomon über GF(256), Masken mit Strafpunkten).
 */
public final class QrCode {

    // Fehlerkorrektur-Level M: Codewörter je Block und Zahl der Blöcke für Version 1–10
    private static final int[] ECC_PER_BLOCK = {-1, 10, 16, 26, 18, 24, 16, 18, 22, 22, 26};
    private static final int[] BLOCKS = {-1, 1, 1, 1, 2, 2, 4, 4, 4, 5, 5};
    private static final int FORMAT_LEVEL_M = 0;

    private final int size;
    private final boolean[][] modules;     // [y][x], true = dunkel
    private final boolean[][] function;

    private QrCode(int version) {
        size = version * 4 + 17;
        modules = new boolean[size][size];
        function = new boolean[size][size];
    }

    public int size() {
        return size;
    }

    /** {@code true} = dunkles Modul. */
    public boolean get(int x, int y) {
        return x >= 0 && y >= 0 && x < size && y < size && modules[y][x];
    }

    /** QR-Code für einen Text (UTF-8); höchstens Version 10 (bis ca. 210 Bytes). */
    public static QrCode encode(String text) {
        byte[] data = text.getBytes(StandardCharsets.UTF_8);
        int version = 1;
        for (; version <= 10; version++) {
            int cap = dataCodewords(version) * 8;
            int need = 4 + (version <= 9 ? 8 : 16) + data.length * 8;
            if (need <= cap) break;
        }
        if (version > 10) throw new IllegalArgumentException("Text zu lang für den QR-Code: " + text.length() + " Zeichen");
        // Bitfolge: Modus „Byte“, Länge, Daten, Abschluss, Füllbytes
        List<Boolean> bits = new ArrayList<>();
        append(bits, 0b0100, 4);
        append(bits, data.length, version <= 9 ? 8 : 16);
        for (byte b : data) append(bits, b & 0xFF, 8);
        int capBits = dataCodewords(version) * 8;
        append(bits, 0, Math.min(4, capBits - bits.size()));
        append(bits, 0, (8 - bits.size() % 8) % 8);
        for (int pad = 0xEC; bits.size() < capBits; pad ^= 0xEC ^ 0x11) append(bits, pad, 8);
        byte[] codewords = new byte[bits.size() / 8];
        for (int i = 0; i < bits.size(); i++) if (bits.get(i)) codewords[i >>> 3] |= (byte) (1 << (7 - (i & 7)));

        QrCode qr = new QrCode(version);
        qr.drawFunctionPatterns(version);
        qr.drawCodewords(addEccAndInterleave(codewords, version));
        int best = 0;
        long bestPenalty = Long.MAX_VALUE;
        for (int mask = 0; mask < 8; mask++) {
            qr.applyMask(mask);
            qr.drawFormatBits(mask);
            long p = qr.penalty();
            if (p < bestPenalty) {
                bestPenalty = p;
                best = mask;
            }
            qr.applyMask(mask);       // wieder entfernen (XOR)
        }
        qr.applyMask(best);
        qr.drawFormatBits(best);
        return qr;
    }

    private static void append(List<Boolean> bits, int value, int len) {
        for (int i = len - 1; i >= 0; i--) bits.add(((value >>> i) & 1) != 0);
    }

    private static int rawDataModules(int ver) {
        int result = (16 * ver + 128) * ver + 64;
        if (ver >= 2) {
            int numAlign = ver / 7 + 2;
            result -= (25 * numAlign - 10) * numAlign - 55;
            if (ver >= 7) result -= 36;
        }
        return result;
    }

    private static int dataCodewords(int ver) {
        return rawDataModules(ver) / 8 - ECC_PER_BLOCK[ver] * BLOCKS[ver];
    }

    // --- Funktionsmuster --------------------------------------------------------------

    private void setFunction(int x, int y, boolean dark) {
        modules[y][x] = dark;
        function[y][x] = true;
    }

    private void drawFunctionPatterns(int version) {
        for (int i = 0; i < size; i++) {
            setFunction(6, i, i % 2 == 0);
            setFunction(i, 6, i % 2 == 0);
        }
        drawFinder(3, 3);
        drawFinder(size - 4, 3);
        drawFinder(3, size - 4);
        int[] align = alignmentPositions(version);
        int n = align.length;
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                if ((i == 0 && j == 0) || (i == 0 && j == n - 1) || (i == n - 1 && j == 0)) continue;
                for (int dy = -2; dy <= 2; dy++) {
                    for (int dx = -2; dx <= 2; dx++) {
                        setFunction(align[i] + dx, align[j] + dy, Math.max(Math.abs(dx), Math.abs(dy)) != 1);
                    }
                }
            }
        }
        drawFormatBits(0);           // Platz reservieren
        if (version >= 7) {
            int rem = version;
            for (int i = 0; i < 12; i++) rem = (rem << 1) ^ ((rem >>> 11) * 0x1F25);
            int bits = version << 12 | rem;
            for (int i = 0; i < 18; i++) {
                boolean bit = ((bits >>> i) & 1) != 0;
                int a = size - 11 + i % 3, b = i / 3;
                setFunction(a, b, bit);
                setFunction(b, a, bit);
            }
        }
    }

    private void drawFinder(int x, int y) {
        for (int dy = -4; dy <= 4; dy++) {
            for (int dx = -4; dx <= 4; dx++) {
                int dist = Math.max(Math.abs(dx), Math.abs(dy));
                int xx = x + dx, yy = y + dy;
                if (xx >= 0 && xx < size && yy >= 0 && yy < size) setFunction(xx, yy, dist != 2 && dist != 4);
            }
        }
    }

    private static int[] alignmentPositions(int ver) {
        if (ver == 1) return new int[0];
        int numAlign = ver / 7 + 2;
        int step = (ver * 4 + numAlign * 2 + 1) / (numAlign * 2 - 2) * 2;
        int[] result = new int[numAlign];
        result[0] = 6;
        for (int i = numAlign - 1, pos = ver * 4 + 10; i >= 1; i--, pos -= step) result[i] = pos;
        return result;
    }

    private void drawFormatBits(int mask) {
        int data = FORMAT_LEVEL_M << 3 | mask;
        int rem = data;
        for (int i = 0; i < 10; i++) rem = (rem << 1) ^ ((rem >>> 9) * 0x537);
        int bits = (data << 10 | rem) ^ 0x5412;
        for (int i = 0; i <= 5; i++) setFunction(8, i, bit(bits, i));
        setFunction(8, 7, bit(bits, 6));
        setFunction(8, 8, bit(bits, 7));
        setFunction(7, 8, bit(bits, 8));
        for (int i = 9; i < 15; i++) setFunction(14 - i, 8, bit(bits, i));
        for (int i = 0; i < 8; i++) setFunction(size - 1 - i, 8, bit(bits, i));
        for (int i = 8; i < 15; i++) setFunction(8, size - 15 + i, bit(bits, i));
        setFunction(8, size - 8, true);
    }

    private static boolean bit(int x, int i) {
        return ((x >>> i) & 1) != 0;
    }

    // --- Daten ----------------------------------------------------------------------------

    private static byte[] addEccAndInterleave(byte[] data, int ver) {
        int numBlocks = BLOCKS[ver], blockEccLen = ECC_PER_BLOCK[ver];
        int rawCodewords = rawDataModules(ver) / 8;
        int numShortBlocks = numBlocks - rawCodewords % numBlocks;
        int shortBlockLen = rawCodewords / numBlocks;
        byte[][] blocks = new byte[numBlocks][];
        byte[] divisor = rsDivisor(blockEccLen);
        for (int i = 0, k = 0; i < numBlocks; i++) {
            int datLen = shortBlockLen - blockEccLen + (i < numShortBlocks ? 0 : 1);
            byte[] dat = java.util.Arrays.copyOfRange(data, k, k + datLen);
            k += datLen;
            byte[] ecc = rsRemainder(dat, divisor);
            byte[] block = new byte[shortBlockLen + 1];
            System.arraycopy(dat, 0, block, 0, datLen);
            System.arraycopy(ecc, 0, block, block.length - blockEccLen, blockEccLen);
            blocks[i] = block;
        }
        byte[] result = new byte[rawCodewords];
        int n = 0;
        for (int i = 0; i < blocks[0].length; i++) {
            for (int j = 0; j < numBlocks; j++) {
                if (i != shortBlockLen - blockEccLen || j >= numShortBlocks) result[n++] = blocks[j][i];
            }
        }
        return result;
    }

    private static byte[] rsDivisor(int degree) {
        byte[] result = new byte[degree];
        result[degree - 1] = 1;
        int root = 1;
        for (int i = 0; i < degree; i++) {
            for (int j = 0; j < degree; j++) {
                result[j] = (byte) mul(result[j] & 0xFF, root);
                if (j + 1 < degree) result[j] ^= result[j + 1];
            }
            root = mul(root, 0x02);
        }
        return result;
    }

    private static byte[] rsRemainder(byte[] data, byte[] divisor) {
        byte[] result = new byte[divisor.length];
        for (byte b : data) {
            int factor = (b ^ result[0]) & 0xFF;
            System.arraycopy(result, 1, result, 0, result.length - 1);
            result[result.length - 1] = 0;
            for (int i = 0; i < result.length; i++) result[i] ^= (byte) mul(divisor[i] & 0xFF, factor);
        }
        return result;
    }

    private static int mul(int x, int y) {
        int z = 0;
        for (int i = 7; i >= 0; i--) {
            z = (z << 1) ^ ((z >>> 7) * 0x11D);
            z ^= ((y >>> i) & 1) * x;
        }
        return z & 0xFF;
    }

    private void drawCodewords(byte[] data) {
        int i = 0;
        for (int right = size - 1; right >= 1; right -= 2) {
            if (right == 6) right = 5;
            for (int vert = 0; vert < size; vert++) {
                for (int j = 0; j < 2; j++) {
                    int x = right - j;
                    boolean upward = ((right + 1) & 2) == 0;
                    int y = upward ? size - 1 - vert : vert;
                    if (!function[y][x] && i < data.length * 8) {
                        modules[y][x] = bit(data[i >>> 3], 7 - (i & 7));
                        i++;
                    }
                }
            }
        }
    }

    private void applyMask(int mask) {
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                if (function[y][x]) continue;
                boolean invert = switch (mask) {
                    case 0 -> (x + y) % 2 == 0;
                    case 1 -> y % 2 == 0;
                    case 2 -> x % 3 == 0;
                    case 3 -> (x + y) % 3 == 0;
                    case 4 -> (x / 3 + y / 2) % 2 == 0;
                    case 5 -> x * y % 2 + x * y % 3 == 0;
                    case 6 -> (x * y % 2 + x * y % 3) % 2 == 0;
                    default -> ((x + y) % 2 + x * y % 3) % 2 == 0;
                };
                if (invert) modules[y][x] = !modules[y][x];
            }
        }
    }

    /** Vereinfachte Strafpunkte (gleichfarbige Reihen, 2×2-Blöcke, Hell/Dunkel-Verhältnis). */
    private long penalty() {
        long p = 0;
        for (int y = 0; y < size; y++) {
            int runX = 1, runY = 1;
            for (int x = 1; x < size; x++) {
                if (modules[y][x] == modules[y][x - 1]) runX++;
                else runX = 1;
                if (runX == 5) p += 3;
                else if (runX > 5) p++;
                if (modules[x][y] == modules[x - 1][y]) runY++;
                else runY = 1;
                if (runY == 5) p += 3;
                else if (runY > 5) p++;
            }
        }
        int dark = 0;
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                if (modules[y][x]) dark++;
                if (x + 1 < size && y + 1 < size && modules[y][x] == modules[y][x + 1]
                        && modules[y][x] == modules[y + 1][x] && modules[y][x] == modules[y + 1][x + 1]) p += 3;
            }
        }
        int total = size * size;
        p += (Math.abs(dark * 20 - total * 10) + total - 1) / total * 10L;
        return p;
    }
}
