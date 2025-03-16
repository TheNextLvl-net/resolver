package net.thenextlvl.resolver;

import com.velocitypowered.api.network.ProtocolVersion;
import org.jspecify.annotations.NullMarked;

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
@NullMarked
public class PingOptions {
    private final InetSocketAddress address;
    private final ProtocolVersion protocolVersion;
    private final int timeout;

    private PingOptions(InetSocketAddress address, ProtocolVersion protocolVersion, int timeout) {
        this.address = address;
        this.protocolVersion = protocolVersion;
        this.timeout = timeout;
    }

    public InetSocketAddress getAddress() {
        return address;
    }

    public ProtocolVersion getProtocolVersion() {
        return protocolVersion;
    }

    public int getTimeout() {
        return timeout;
    }

    public Builder toBuilder() {
        return new Builder(address)
                .protocolVersion(protocolVersion)
                .timeout(timeout);
    }

    public static Builder builder(InetSocketAddress address) {
        return new Builder(address);
    }

    public static class Builder {
        private InetSocketAddress address;
        private ProtocolVersion protocolVersion = ProtocolVersion.MAXIMUM_VERSION;
        private int timeout = 5000;

        private Builder(InetSocketAddress address) {
            this.address = address;
        }

        public Builder address(InetSocketAddress address) {
            this.address = address;
            return this;
        }

        public Builder protocolVersion(ProtocolVersion protocolVersion) {
            this.protocolVersion = protocolVersion;
            return this;
        }

        public Builder timeout(int timeout) {
            this.timeout = timeout;
            return this;
        }

        public PingOptions build() {
            return new PingOptions(address, protocolVersion, timeout);
        }
    }
}
