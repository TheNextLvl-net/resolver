package net.thenextlvl.resolver.scanner;

import net.thenextlvl.resolver.Ping;
import net.thenextlvl.resolver.PingOptions;
import net.thenextlvl.resolver.ServerPing;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class PortScanner {
    private final ExecutorService pool = Executors.newCachedThreadPool();
    private final CountDownLatch latch;
    private final PingOptions pingOptions;
    private final int amount;

    public PortScanner(PingOptions pingOptions, int amount) {
        this.latch = new CountDownLatch(amount);
        this.pingOptions = pingOptions;
        this.amount = amount;
    }

    public void startScan(Consumer<ServerPing> consumer) throws InterruptedException {
        for (var amount = 0; amount < this.amount; amount++) {
            if (amount % 100 == 0) Thread.sleep(50);
            submitTest(pingOptions.getAddress().getPort() + amount, consumer);
        }
        latch.await();
        pool.shutdown();
    }

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
