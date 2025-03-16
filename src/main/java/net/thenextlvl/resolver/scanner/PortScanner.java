package net.thenextlvl.resolver.scanner;

import net.thenextlvl.resolver.Ping;
import net.thenextlvl.resolver.PingOptions;
import net.thenextlvl.resolver.ServerPing;
import org.jspecify.annotations.NullMarked;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * The PortScanner class is responsible for scanning a range of ports on a given server to determine their status.
 * It utilizes a thread pool
 * to perform the scanning concurrently and a latch to synchronize the completion of all tasks.
 */
@NullMarked
public class PortScanner {
    private final ExecutorService pool = Executors.newCachedThreadPool();
    private final CountDownLatch latch;
    private final PingOptions pingOptions;
    private final int amount;

    /**
     * Constructs a PortScanner instance with the given ping options and the number of ports to scan.
     *
     * @param pingOptions the options to configure the ping operation, including the server address, timeout, and protocol version
     * @param amount      the number of ports to scan
     */
    public PortScanner(PingOptions pingOptions, int amount) {
        this.latch = new CountDownLatch(amount);
        this.pingOptions = pingOptions;
        this.amount = amount;
    }

    /**
     * Starts scanning a range of ports on a given server concurrently using a thread pool.
     * The results of each scan are passed to the specified consumer.
     *
     * @param consumer a {@link Consumer} to handle the {@link ServerPing} response for each scanned port
     * @throws InterruptedException if the current thread is interrupted while waiting
     */
    public void startScan(Consumer<ServerPing> consumer) throws InterruptedException {
        for (var amount = 0; amount < this.amount; amount++) {
            if (amount % 100 == 0) Thread.sleep(50);
            submitTest(pingOptions.getAddress().getPort() + amount, consumer);
        }
        latch.await();
        pool.shutdown();
    }

    /**
     * Submits a port scanning task to the thread pool.
     * The task pings a specific port on the server and passes the result to the provided consumer.
     *
     * @param port     the port number to scan on the server
     * @param consumer a {@link Consumer} to handle the {@link ServerPing} response for the scanned port
     */
    private void submitTest(int port, Consumer<ServerPing> consumer) {
        pool.submit(() -> {
            try {
                consumer.accept(Ping.ping(pingOptions.toBuilder()
                        .address(new InetSocketAddress(pingOptions.getAddress().getAddress(), port))
                        .build()));
            } catch (IOException ignored) {
            } finally {
                latch.countDown();
            }
        });
    }
}
