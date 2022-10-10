package org.swdc.filetype.umd;

import java.io.ByteArrayOutputStream;
import java.util.zip.Inflater;

public class UMDUtil {

    /**
     * @param headerByte first four bytes of an umd file
     * @return the header hex string
     */
    public static String computeHeader(byte[] headerByte) {
        if (headerByte.length != 4) {
            return null;
        }
        StringBuilder unsignedHeader = new StringBuilder();
        for (int idx = 0; idx < headerByte.length; idx ++) {
            unsignedHeader.append(Integer.toUnsignedString(headerByte[idx] & 0xFF, 16));
        }
        return unsignedHeader.toString();
    }

    public static long toUint16(byte[] intData) {
        return ((intData[0] & 0xFF) <<  0) |
                ((intData[1] & 0xFF) <<  8);
    }

    public static long toUint16Rev(byte[] intData) {
        return ((intData[1] & 0xFF) <<  0) |
                ((intData[0] & 0xFF) <<  8);
    }

    public static long toUint32(byte[] intData) {
        return ((intData[0] & 0xFF) <<  0) |
                ((intData[1] & 0xFF) <<  8) |
                ((intData[2] & 0xFF) << 16) |
                ((intData[3] & 0xFF) << 24);
    }

    // 解压缩 字节数组
    public static byte[] decompress(byte[] data) {
        byte[] output = new byte[0];
        Inflater inflater = new Inflater();
        inflater.reset();
        inflater.setInput(data);
        ByteArrayOutputStream outputStream  = new ByteArrayOutputStream(data.length);
        try {
            byte[] buf = new byte[1024];
            while (!inflater.finished()) {
                int count  = inflater.inflate(buf);
                outputStream .write(buf, 0, count );
            }
            return outputStream.toByteArray();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

}
