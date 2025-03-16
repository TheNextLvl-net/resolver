package net.thenextlvl.resolver;

import com.velocitypowered.api.network.ProtocolVersion;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;
import java.util.Optional;

/**
 * Represents the type of server including its brand, an optional modification type,
 * and a flag indicating if it is a proxy server.
 *
 * @param brand   the brand name of the server
 * @param modType the type of modification can be null if not applicable
 * @param proxy   indicates if the server is a proxy server
 */
@NullMarked
public record ServerType(String brand, @Nullable String modType, boolean proxy) {
    /**
     * An array of predefined server brand names.
     */
    private static final String[] brands = new String[]{
            "CraftBukkit", "Paper", "Pufferfish", "Purpur", "Spigot", "Tuinity",
            "Forge"
    };

    /**
     * An array of predefined proxy server names.
     */
    private static final String[] proxies = new String[]{
            "BungeeCord", "FlameCord", "Travertine", "Velocity", "Waterfall", "XCord"
    };

    /**
     * Matches the provided version string against a predefined list of server brands.
     *
     * @param version the server version string to match against known brands
     * @return an Optional containing the matched brand if found, otherwise an empty Optional
     */
    public static Optional<String> matchBrand(String version) {
        return Arrays.stream(brands)
                .filter(brand -> version.toLowerCase().contains(brand.toLowerCase()))
                .findAny();
    }

    /**
     * Matches the provided version string against a predefined list of proxy names.
     *
     * @param version the server version string to match against known proxy names
     * @return an Optional containing the matched proxy name if found, otherwise an empty Optional
     */
    public static Optional<String> matchProxy(String version) {
        return Arrays.stream(proxies)
                .filter(proxy -> version.toLowerCase().contains(proxy.toLowerCase()))
                .findAny();
    }

    /**
     * Matches the provided version string against a predefined list of protocol versions
     * to determine if it corresponds to a "Vanilla" server.
     *
     * @param version the server version string to match
     * @return an Optional containing "Vanilla" if the version matches any supported protocol version, otherwise an empty Optional
     */
    public static Optional<String> matchVanilla(String version) {
        return Optional.ofNullable(Arrays.stream(ProtocolVersion.values())
                .map(ProtocolVersion::getVersionsSupportedBy)
                .anyMatch(strings -> strings.contains(version))
                ? "Vanilla" : null);
    }

    /**
     * Attempts to guess the type of server based on the provided {@link ServerPing}.
     *
     * @param ping the ServerPing object containing server details
     * @return an Optional containing the guessed ServerType if identified, otherwise an empty Optional
     */
    public static ServerType guess(ServerPing ping) {
        var version = Optional.ofNullable(ping.getVersion())
                .map(com.velocitypowered.api.proxy.server.ServerPing.Version::getName);
        var brand = version.flatMap(ServerType::matchBrand);
        var proxy = version.flatMap(ServerType::matchProxy);
        var modType = ping.getModInfo() != null ? ping.getModInfo().getType() : null;
        var type = brand.orElse(proxy.orElse(version
                .flatMap(ServerType::matchVanilla)
                .orElse(version.orElse("unknown"))));
        return new ServerType(type, modType, proxy.isPresent());
    }

    /**
     * Determines if the server is modded based on the presence of a mod type.
     *
     * @return true if the server has a mod type, false otherwise
     */
    public boolean isModded() {
        return modType != null;
    }

    @Override
    public String toString() {
        return brand + (proxy ? " (proxy)" : "") + (isModded() ? " (mod " + modType + ")" : "");
    }
}
