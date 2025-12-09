package net.thenextlvl.resolver;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.api.proxy.server.ServerPing;
import net.thenextlvl.resolver.scanner.AddressResolver;
import net.thenextlvl.resolver.scanner.PortScanner;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Optional;

public class PortScannerTest {
    private static final int scanAmount = 10000;

    public static void main(String[] args) throws InterruptedException, IOException {
        var list = Files.readAllLines(Path.of("src/test/servers.txt"))
                .stream()
                .map(string -> string.split("#", 2)[0]) // remove comments
                .map(String::strip) // strip whitespaces
                .toList();

        var servers = new ArrayList<String>();
        for (var server : list) {
            if (server.isBlank()) break;
            servers.add(server);
        }

        var options = new ArrayList<PingOptions>();
        new AddressResolver(servers).startScan(options::add);


        for (var option : options) {
            var now = System.currentTimeMillis();

            System.out.printf("Start scanning %s ports on %s%n", scanAmount, option.getAddress().getHostName());

            new PortScanner(option, scanAmount).startScan(open -> {
                var type = ServerType.guess(open);
                var version = Optional.ofNullable(open.getVersion())
                        .map(ServerPing.Version::getProtocol)
                        .map(ProtocolVersion::getProtocolVersion)
                        .orElse(ProtocolVersion.UNKNOWN)
                        .getMostRecentSupportedVersion();
                System.out.printf("%s:%s %sms # %s %s%n",
                        open.getAddress().getAddress().getHostAddress(),
                        open.getAddress().getPort(),
                        open.getPing(),
                        type,
                        version
                );
            });

            var time = (System.currentTimeMillis() - now) / 1000d;
            System.out.printf("Finished scanning %s ports in (%ss)%n", scanAmount, time);
            System.out.println();
        }
    }
}
