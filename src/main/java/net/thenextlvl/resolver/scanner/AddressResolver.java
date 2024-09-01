package net.thenextlvl.resolver.scanner;

import com.velocitypowered.api.network.ProtocolVersion;
import net.thenextlvl.resolver.Ping;
import net.thenextlvl.resolver.PingOptions;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class AddressResolver {
    private final ExecutorService pool = Executors.newCachedThreadPool();
    private final CountDownLatch latch;
    private final List<String> hostnames;

    public AddressResolver(List<String> hostnames) {
        this.latch = new CountDownLatch(hostnames.size());
        this.hostnames = hostnames;
    }

    public void startScan(Consumer<PingOptions> consumer) throws InterruptedException {
        for (String hostname : hostnames) submitTest(hostname, consumer);
        latch.await();
        pool.shutdown();
    }

    private void submitTest(String string, Consumer<PingOptions> consumer) {
        pool.submit(() -> {
            try {
                var split = string.split(":", 2);
                var hostname = split[0].toLowerCase().strip();

                var address = Ping.resolveAddress(hostname).orElseGet(() -> {
                    var port = split.length > 1 ? Integer.parseInt(split[1].strip()) : 25565;
                    return new InetSocketAddress(hostname, port);
                });

                var pingOptions = PingOptions.builder()
                        .address(address)
                        .protocolVersion(ProtocolVersion.MAXIMUM_VERSION)
                        .timeout(1000)
                        .build();

                consumer.accept(pingOptions);
            } finally {
                latch.countDown();
            }
        });
    }
}
