package net.thenextlvl.resolver;

import com.google.gson.annotations.SerializedName;
import com.velocitypowered.api.proxy.server.ServerPing.Players;
import com.velocitypowered.api.proxy.server.ServerPing.Version;
import com.velocitypowered.api.util.ModInfo;
import core.annotation.FieldsAreNullableByDefault;
import core.annotation.TypesAreNullableByDefault;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

import java.net.InetSocketAddress;

/**
 * <a href="http://wiki.vg/Server_List_Ping">Protocol</a>
 */
@Getter
@AllArgsConstructor
@TypesAreNullableByDefault
@FieldsAreNullableByDefault
@Setter(value = AccessLevel.PACKAGE)
public class ServerPing {
    private final @SerializedName("description") Description description;
    private final @SerializedName("players") Players players;
    private final @SerializedName("version") Version version;
    private final @SerializedName("favicon") String favicon;
    private final @SerializedName("modinfo") ModInfo modInfo;

    private @NotNull InetSocketAddress address;
    private long ping;

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

    public record Description(
            @SerializedName("text") String text
    ) {
    }
}
