package net.thenextlvl.resolver;

import com.velocitypowered.api.network.ProtocolVersion;
import lombok.Builder;
import lombok.Getter;

import java.net.InetSocketAddress;

/**
 * Represents the options for pinging a server.
 * <p>
 * This class contains the necessary parameters to configure the ping operation, including
 * the server address, timeout duration, and protocol version.
 * <p>
 * Instances of this class are immutable and can be created using the builder pattern.
 * <p>
 * Fields:<br>
 * - address: The {@link InetSocketAddress} of the server to ping.<br>
 * - timeout: The timeout duration in milliseconds for the ping operation. Defaults to 5000 ms.<br>
 * - protocolVersion: The protocol version to use when pinging the server. Defaults to {@link ProtocolVersion#MAXIMUM_VERSION}.
 */
@Getter
@Builder(toBuilder = true)
public class PingOptions {
    private InetSocketAddress address;
    @Builder.Default
    private int timeout = 5000;
    @Builder.Default
    private ProtocolVersion protocolVersion = ProtocolVersion.MAXIMUM_VERSION;
}
