package net.thenextlvl.resolver;

import com.google.gson.annotations.SerializedName;
import com.velocitypowered.api.proxy.server.ServerPing.Players;
import com.velocitypowered.api.proxy.server.ServerPing.Version;
import com.velocitypowered.api.util.ModInfo;
import org.jspecify.annotations.NullMarked;

import java.net.InetSocketAddress;

/**
 * A class that represents the ping response from a server. This includes
 * details such as the server description, players, version, favicon, and mod information.
 * It also includes the address and the ping time of the server.
 * <p>
 * Fields are annotated with @SerializedName to denote the key names in the JSON response.
 * The class is immutable except for the address and ping fields, which can be set within
 * the package.
 * <p>
 * The Description is a record that contains the text description of the server.
 * <p>
 * <a href="http://wiki.vg/Server_List_Ping">Protocol</a>
 */
@NullMarked
public class ServerPing {
    private final @SerializedName("description") Description description;
    private final @SerializedName("players") Players players;
    private final @SerializedName("version") Version version;
    private final @SerializedName("favicon") String favicon;
    private final @SerializedName("modinfo") ModInfo modInfo;

    private InetSocketAddress address;
    private long ping;

    public ServerPing(Description description, Players players, Version version, String favicon, ModInfo modInfo, InetSocketAddress address, long ping) {
        this.description = description;
        this.players = players;
        this.version = version;
        this.favicon = favicon;
        this.modInfo = modInfo;
        this.address = address;
        this.ping = ping;
    }

    public Description getDescription() {
        return description;
    }

    public Players getPlayers() {
        return players;
    }

    public Version getVersion() {
        return version;
    }

    public String getFavicon() {
        return favicon;
    }

    public ModInfo getModInfo() {
        return modInfo;
    }

    public InetSocketAddress getAddress() {
        return address;
    }

    public long getPing() {
        return ping;
    }

    void setAddress(InetSocketAddress address) {
        this.address = address;
    }

    void setPing(long ping) {
        this.ping = ping;
    }

    @Override
    public String toString() {
        return "ServerPing{" +
               "description=" + description +
               ", players=" + players +
               ", version=" + version +
               ", favicon='" + favicon + '\'' +
               ", modInfo=" + modInfo +
               ", address=" + address +
               ", ping=" + ping +
               '}';
    }

    /**
     * The Description record encapsulates the text description of a server.
     * This description is typically used to provide details about the server,
     * and is a part of the ServerPing response.
     * <p>
     * The single field in this record is serialized with the @SerializedName
     * annotation to map the JSON key "text" to the field.
     * <p>
     * Fields:
     * - text: A string containing the text description of the server.
     * <p>
     * The Description record is immutable and ensures that the text field
     * remains constant once it is created.
     */
    public record Description(
            @SerializedName("text") String text
    ) {
    }
}
