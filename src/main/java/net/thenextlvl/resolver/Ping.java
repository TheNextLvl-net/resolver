package net.thenextlvl.resolver;

import com.google.common.base.Preconditions;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.thenextlvl.resolver.adapter.UUIDAdapter;
import org.jspecify.annotations.NullMarked;

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

/**
 * The Ping class provides methods to ping a Minecraft server to retrieve its status and resolve its address.
 * It uses the Gson library for JSON parsing and serialization.
 */
@NullMarked
public class Ping {
    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(UUID.class, new UUIDAdapter())
            .create();

    /**
     * Ping a server using specified {@link PingOptions} and retrieve the server ping response.
     *
     * @param options the options containing server address, timeout, and protocol version for pinging the server
     * @return the ping response from the server wrapped in a {@link ServerPing} object
     * @throws IOException if an I/O error occurs during the ping process
     */
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
                var id = PingUtil.readVarInt(input);


                Preconditions.checkState(id != -1, "Server prematurely ended stream.");
                Preconditions.checkState(id == PingUtil.STATUS_REQUEST_PACKET, "Server returned invalid packet.");

                var length = PingUtil.readVarInt(input);
                Preconditions.checkState(length != -1, "Server prematurely ended stream.");
                Preconditions.checkState(length != 0, "Server returned unexpected value.");

                var data = new byte[length];
                input.readFully(data);
                json = new String(data, StandardCharsets.UTF_8);

                output.writeByte(0x09);
                output.writeByte(PingUtil.PING_PACKET);
                output.writeLong(System.currentTimeMillis());

                PingUtil.readVarInt(input);
                id = PingUtil.readVarInt(input);
                Preconditions.checkState(id != -1, "Server prematurely ended stream.");
                Preconditions.checkState(id == PingUtil.PING_PACKET, "Server returned invalid packet.");
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

    /**
     * Resolves the given hostname to an InetSocketAddress using DNS SRV records.
     * If resolution fails, returns an empty Optional.
     *
     * @param hostname the hostname to be resolved
     * @return an Optional containing the resolved InetSocketAddress, or an empty Optional if the resolution fails
     */
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
