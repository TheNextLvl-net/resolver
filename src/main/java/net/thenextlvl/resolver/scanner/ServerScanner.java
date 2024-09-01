package net.thenextlvl.resolver.scanner;

import net.thenextlvl.resolver.Ping;
import net.thenextlvl.resolver.PingOptions;
import net.thenextlvl.resolver.ServerPing;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class ServerScanner {
    private final ExecutorService pool = Executors.newCachedThreadPool();
    private final CountDownLatch latch;
    private final List<PingOptions> options;

    public ServerScanner(List<PingOptions> options) {
        this.latch = new CountDownLatch(options.size());
        this.options = options;
    }

    public void startScan(Consumer<ServerPing> success, BiConsumer<PingOptions, IOException> exception) throws InterruptedException {
        var amount = options.size();
        for (int index = 0; index < amount; index++) {
            if (index % 100 == 0) Thread.sleep(50);
            submitTest(options.get(index), success, exception);
        }
        latch.await();
        pool.shutdown();
    }

    private void submitTest(PingOptions options, Consumer<ServerPing> consumer, BiConsumer<PingOptions, IOException> exception) {
        pool.submit(() -> {
            try {
                consumer.accept(Ping.ping(options));
            } catch (IOException e) {
                exception.accept(options, e);
            } finally {
                latch.countDown();
            }
        });
    }
}
