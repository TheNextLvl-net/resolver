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

/**
 * The AddressResolver class is responsible for resolving and pinging a list of hostnames asynchronously.
 * It uses an ExecutorService to manage the asynchronous tasks and a CountDownLatch to ensure that all
 * tasks are completed before shutting down the thread pool.
 */
public class AddressResolver {
    private final ExecutorService pool = Executors.newCachedThreadPool();
    private final CountDownLatch latch;
    private final List<String> hostnames;

    /**
     * Constructs an AddressResolver instance.
     *
     * @param hostnames The list of hostnames to resolve and ping.
     */
    public AddressResolver(List<String> hostnames) {
        this.latch = new CountDownLatch(hostnames.size());
        this.hostnames = hostnames;
    }

    /**
     * Starts the scanning process for the list of hostnames.
     * <p>
     * This method initiates asynchronous tasks to ping each hostname in the list. It waits
     * for all tasks to complete before shutting down the thread pool.
     *
     * @param consumer A consumer that processes the {@link PingOptions} for each hostname.
     * @throws InterruptedException If the current thread is interrupted while waiting.
     */
    public void startScan(Consumer<PingOptions> consumer) throws InterruptedException {
        hostnames.forEach(hostname -> submitTest(hostname, consumer));
        latch.await();
        pool.shutdown();
    }

    /**
     * Submits a task to resolve the given hostname string and generate PingOptions,
     * then passes the PingOptions to the provided Consumer.
     * This method utilizes threads from an ExecutorService for asynchronous execution.
     *
     * @param string The hostname string to be resolved, optionally including a port separated by a colon.
     * @param consumer A Consumer that processes the generated PingOptions.
     */
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
