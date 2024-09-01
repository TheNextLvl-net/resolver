package net.thenextlvl.resolver;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.thenextlvl.resolver.adapter.UUIDAdapter;

import javax.naming.Context;
import javax.naming.directory.InitialDirContext;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Hashtable;
import java.util.Optional;
import java.util.UUID;

public class Ping {
    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(UUID.class, new UUIDAdapter())
            .create();

    public static ServerPing ping(PingOptions options) throws IOException {
        String json;
        long ping;

        try (var socket = new Socket()) {

            long start = System.currentTimeMillis();
            socket.connect(options.getAddress(), options.getTimeout());
            socket.setSoTimeout(options.getTimeout());
            ping = System.currentTimeMillis() - start;

            try (var input = new DataInputStream(socket.getInputStream());
                 var output = new DataOutputStream(socket.getOutputStream());
                 var byteArray = new ByteArrayOutputStream();
                 var handshake = new DataOutputStream(byteArray)) {

                handshake.writeByte(PingUtil.HANDSHAKE_PACKET);
                PingUtil.writeVarInt(handshake, options.getProtocolVersion().getProtocol());
                PingUtil.writeVarInt(handshake, options.getAddress().getHostName().length());
                handshake.writeBytes(options.getAddress().getHostName());
                handshake.writeShort(options.getAddress().getPort());
                PingUtil.writeVarInt(handshake, PingUtil.STATUS_HANDSHAKE);

                PingUtil.writeVarInt(output, byteArray.size());
                output.write(byteArray.toByteArray());

                output.writeByte(0x01);
                output.writeByte(PingUtil.STATUS_REQUEST_PACKET);

                PingUtil.readVarInt(input);
                int id = PingUtil.readVarInt(input);


                PingUtil.io(id == -1, "Server prematurely ended stream.");
                PingUtil.io(id != PingUtil.STATUS_REQUEST_PACKET, "Server returned invalid packet.");

                int length = PingUtil.readVarInt(input);
                PingUtil.io(length == -1, "Server prematurely ended stream.");
                PingUtil.io(length == 0, "Server returned unexpected value.");

                byte[] data = new byte[length];
                input.readFully(data);
                json = new String(data, StandardCharsets.UTF_8);

                output.writeByte(0x09);
                output.writeByte(PingUtil.PING_PACKET);
                output.writeLong(System.currentTimeMillis());

                PingUtil.readVarInt(input);
                id = PingUtil.readVarInt(input);
                PingUtil.io(id == -1, "Server prematurely ended stream.");
                PingUtil.io(id != PingUtil.PING_PACKET, "Server returned invalid packet.");
            }
        }

        var jsonObject = JsonParser.parseString(json).getAsJsonObject();
        var descriptionJsonElement = jsonObject.get("description");

        if (descriptionJsonElement.isJsonObject()) {

            var description = jsonObject.get("description").getAsJsonObject();

            if (description.has("extra")) {
                description.addProperty("text", description.get("extra").getAsJsonArray().toString());
                jsonObject.add("description", description);
            }

        } else {

            var description = descriptionJsonElement.getAsString();
            var descriptionObject = new JsonObject();

            descriptionObject.addProperty("text", description);
            jsonObject.add("description", descriptionObject);

        }

        var output = GSON.fromJson(jsonObject, ServerPing.class);

        output.setPing(ping);
        output.setAddress(options.getAddress());

        return output;
    }

    public static Optional<InetSocketAddress> resolveAddress(String hostname) {
        try {
            var query = "_minecraft._tcp." + hostname;
            var table = new Hashtable<>();

            table.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.dns.DnsContextFactory");

            var context = new InitialDirContext(table);
            var attribute = context.getAttributes(query, new String[]{"SRV"}).get("SRV");

            if (attribute == null) return Optional.empty();

            var split = ((String) attribute.get()).split(" ");

            return Optional.of(new InetSocketAddress(
                    split[3].replaceFirst("\\.$", ""),
                    Integer.parseInt(split[2])
            ));

        } catch (Exception ignored) {
            return Optional.empty();
        }
    }
}
