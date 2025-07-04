package net.thenextlvl.resolver;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.api.proxy.server.ServerPing;
import core.file.format.TextFile;
import core.io.IO;
import net.thenextlvl.resolver.scanner.AddressResolver;
import net.thenextlvl.resolver.scanner.ServerScanner;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Optional;

public class ResolverTest {
    @Test
    public void failOnNoDiscoveredTestsPropertyDoesntWork() {
    }

    public static void main(String[] args) throws InterruptedException {
        var list = new TextFile(IO.of("src/test/", "servers.txt")).getRoot().stream()
                .map(string -> string.split("#", 2)[0])
                .map(String::strip)
                .toList();

        var servers = new ArrayList<String>();
        for (var server : list) {
            if (server.isBlank()) break;
            servers.add(server);
        }

        var pingOptions = new ArrayList<PingOptions>();
        new AddressResolver(servers).startScan(pingOptions::add);

        new ServerScanner(pingOptions).startScan(ping -> {
            var serverType = ServerType.guess(ping);
            var version = Optional.ofNullable(ping.getVersion())
                    .map(ServerPing.Version::getProtocol)
                    .map(ProtocolVersion::getProtocolVersion)
                    .orElse(ProtocolVersion.UNKNOWN)
                    .getMostRecentSupportedVersion();
            System.out.printf("%s:%s %sms # %s %s%n", ping.getAddress().getAddress(), ping.getAddress().getPort(), ping.getPing(), serverType, version);
        }, (option, e) -> {
            if (option.getAddress().isUnresolved()) {
                System.err.printf("%s cannot be resolved%n", option.getAddress().getHostName());
                return;
            }
            System.err.printf("%s:%s is down or unreachable.%n", option.getAddress().getAddress(), option.getAddress().getPort());
        });
    }
}
