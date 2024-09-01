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

/**
 * The ServerScanner class is responsible for scanning a list of servers using the provided ping options.
 * It uses an ExecutorService to manage the concurrent execution of ping tasks and a CountDownLatch to
 * keep track of the completed tasks.
 */
public class ServerScanner {
    private final ExecutorService pool = Executors.newCachedThreadPool();
    private final CountDownLatch latch;
    private final List<PingOptions> options;

    /**
     * Constructs a new ServerScanner with the given list of PingOptions.
     * Initializes the CountDownLatch with the size of the option list.
     *
     * @param options the list of PingOptions to be used for the server scan
     */
    public ServerScanner(List<PingOptions> options) {
        this.latch = new CountDownLatch(options.size());
        this.options = options;
    }

    /**
     * Initiates the scanning of servers using the provided list of PingOptions.
     * Each server in the option list is pinged concurrently, with a slight delay to prevent overwhelming the network.
     * The success and exception callbacks are used to handle the results of each ping operation.
     *
     * @param success   a callback function that is invoked with the ServerPing result upon a successful ping
     * @param exception a callback function that is invoked with the PingOptions and IOException if a ping fails
     * @throws InterruptedException if the thread is interrupted while waiting for all tasks to complete
     */
    public void startScan(Consumer<ServerPing> success, BiConsumer<PingOptions, IOException> exception) throws InterruptedException {
        var amount = options.size();
        for (int index = 0; index < amount; index++) {
            if (index % 100 == 0) Thread.sleep(50);
            submitTest(options.get(index), success, exception);
        }
        latch.await();
        pool.shutdown();
    }

    /**
     * Submits a ping task to the thread pool using the specified options and callbacks.
     *
     * @param options   the PingOptions to be used for the ping operation
     * @param consumer  a callback function to be invoked with the ServerPing result upon successful ping
     * @param exception a callback function to be invoked with the PingOptions and IOException if the ping fails
     */
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
