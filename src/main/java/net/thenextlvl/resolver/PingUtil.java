package net.thenextlvl.resolver;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class PingUtil {
    public static final byte HANDSHAKE_PACKET = 0x00;
    public static final byte STATUS_REQUEST_PACKET = 0x00;
    public static final byte PING_PACKET = 0x01;
    public static final int STATUS_HANDSHAKE = 1;

    public static void io(final boolean b, final String m) throws IOException {
        if (b) throw new IOException(m);
    }

    public static int readVarInt(DataInputStream in) throws IOException {
        int i = 0;
        int j = 0;
        while (true) {
            int k = in.readByte();
            i |= (k & 0x7F) << j++ * 7;
            if (j > 5) throw new RuntimeException("VarInt too big");
            if ((k & 0x80) != 128) break;
        }
        return i;
    }

    public static void writeVarInt(DataOutputStream out, int paramInt) throws IOException {
        while (true) {
            if ((paramInt & 0xFFFFFF80) == 0) {
                out.writeByte(paramInt);
                return;
            }
            out.writeByte(paramInt & 0x7F | 0x80);
            paramInt >>>= 7;
        }
    }
}
