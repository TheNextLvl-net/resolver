package net.thenextlvl.resolver;

import com.velocitypowered.api.network.ProtocolVersion;
import lombok.Builder;
import lombok.Getter;

import java.net.InetSocketAddress;

@Getter
@Builder(toBuilder = true)
public class PingOptions {
    private InetSocketAddress address;
    @Builder.Default
    private int timeout = 5000;
    @Builder.Default
    private ProtocolVersion protocolVersion = ProtocolVersion.MAXIMUM_VERSION;
}
