package net.thenextlvl.resolver.scanner;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class IPv4Scanner {
    public static void main(String[] args) {
        var startTime = System.currentTimeMillis();

        var numThreads = Runtime.getRuntime().availableProcessors();
        var allIntervals = PublicIpv4Iterator.getAllowedIntervals();

        var totalAddresses = 0L;
        for (var interval : allIntervals) {
            totalAddresses += (interval[1] - interval[0] + 1);
        }
        final var finalTotalAddresses = totalAddresses;

        var threadIntervals = new ArrayList<List<long[]>>();
        for (var i = 0; i < numThreads; i++) {
            threadIntervals.add(new ArrayList<>());
        }

        for (var i = 0; i < allIntervals.size(); i++) {
            threadIntervals.get(i % numThreads).add(allIntervals.get(i));
        }

        var workers = Math.min(numThreads, allIntervals.size());

        try (var executor = Executors.newFixedThreadPool(workers);
             var progressExecutor = Executors.newSingleThreadScheduledExecutor()) {

            System.out.printf("Using %s threads for sequential IPv4 enumeration%n", workers);
            System.out.printf("Total public IPv4 addresses to process: %s%n", totalAddresses);

            var totalCount = new AtomicLong(0);
            var progressTask = progressExecutor.scheduleAtFixedRate(() -> {
                var count = totalCount.get();
                if (count <= 0) return;
                var elapsed = System.currentTimeMillis() - startTime;
                var progress = (count * 100d) / finalTotalAddresses;
                System.out.printf("Progress: %.2f%% - %d/%d addresses processed in %ds%n", progress, count, finalTotalAddresses, elapsed / 1000);
                System.out.printf("Average: %.3fns per address%n", ((double) elapsed / (double) count) * 1000d * 1000d);
            }, 5, 5, TimeUnit.SECONDS);

            var futures = new ArrayList<Future<?>>();
            for (int i = 0; i < workers; i++) {
                final var assignedIntervals = threadIntervals.get(i);

                futures.add(executor.submit(() -> {
                    var localCount = 0;

                    for (var interval : assignedIntervals) {
                        var start = interval[0];
                        var end = interval[1];

                        for (var addr = start; addr <= end; addr++) {
                            var dotted = intToDotted(addr);
                            // todo: process
                            localCount++;

                            if (localCount % 1_000_000 == 0) {
                                totalCount.addAndGet(localCount);
                                localCount = 0;
                            }
                        }
                    }

                    // Add remaining count
                    if (localCount > 0) {
                        totalCount.addAndGet(localCount);
                    }
                }));
            }

            for (var future : futures) {
                try {
                    future.get();
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                }
            }

            progressTask.cancel(false);

            var elapsed = System.currentTimeMillis() - startTime;
            var totalProcessed = totalCount.get();

            System.out.println("\n=== Final Results ===");
            System.out.printf("Total addresses processed: %s%n", totalProcessed);
            if (totalProcessed == totalAddresses) {
                System.out.println("✓ All public IPv4 addresses enumerated successfully");
            } else {
                System.out.printf("Expected addresses: %s%n", totalAddresses);
                System.out.println("⚠ Warning: Processed count doesn't match expected count");
            }
            System.out.printf("Processing time: %.2fs%n", elapsed / 1000d);
            System.out.printf("Average time per address: %.3fns%n", ((double) elapsed / (double) totalProcessed) * 1000d * 1000d);
        }
    }

    private static String intToDotted(long value) {
        var v = value & 0xFFFFFFFFL;
        var a = (int) ((v >> 24) & 0xFF);
        var b = (int) ((v >> 16) & 0xFF);
        var c = (int) ((v >> 8) & 0xFF);
        var d = (int) (v & 0xFF);
        return a + "." + b + "." + c + "." + d;
    }

    private static class PublicIpv4Iterator {
        public static List<long[]> getAllowedIntervals() {
            var reserved = mergeIntervals(buildReservedCidrs()
                    .map(PublicIpv4Iterator::cidrToRange)
                    .collect(Collectors.toList()));
            return buildAllowedFromReserved(reserved);
        }

        private static Stream<String> buildReservedCidrs() {
            return Stream.of(
                    "0.0.0.0/8",        // "This" network
                    "1.1.1.1/32",       // Cloudflare DNS
                    "8.8.8.8/32",       // Google DNS
                    "10.0.0.0/8",       // private
                    "100.64.0.0/10",    // carrier-grade NAT
                    "127.0.0.0/8",      // loopback
                    "169.254.0.0/16",   // link-local
                    "172.16.0.0/12",    // private
                    "192.0.0.0/24",     // IETF protocol assignments / special
                    "192.0.2.0/24",     // TEST-NET-1 (docs)
                    "192.88.99.0/24",   // 6to4 relay
                    "192.168.0.0/16",   // private
                    "198.18.0.0/15",    // benchmark/testing
                    "198.51.100.0/24",  // TEST-NET-2 (docs)
                    "203.0.113.0/24",   // TEST-NET-3 (docs)
                    "224.0.0.0/4",      // multicast
                    "240.0.0.0/4"       // reserved for future use
            );
        }

        /**
         * Convert CIDR string (e.g., "192.0.2.0/24") to [startInclusive, endInclusive] (as unsigned longs)
         */
        private static long[] cidrToRange(String cidr) {
            var parts = cidr.trim().split("/");
            if (parts.length != 2) throw new IllegalArgumentException("Bad CIDR: " + cidr);

            var prefix = Integer.parseInt(parts[1]);
            if (prefix < 0 || prefix > 32) throw new IllegalArgumentException("Bad prefix: " + prefix);

            var mask = (prefix == 0) ? 0L : (0xFFFFFFFFL << (32 - prefix)) & 0xFFFFFFFFL;
            var start = dottedToLong(parts[0]) & mask;
            var end = (start | (~mask & 0xFFFFFFFFL)) & 0xFFFFFFFFL;
            return new long[]{start, end};
        }

        private static long dottedToLong(String dotted) {
            var q = dotted.split("\\.");
            if (q.length != 4) throw new IllegalArgumentException("Bad IPv4: " + dotted);
            var a = Integer.parseInt(q[0]) & 0xFFL;
            var b = Integer.parseInt(q[1]) & 0xFFL;
            var c = Integer.parseInt(q[2]) & 0xFFL;
            var d = Integer.parseInt(q[3]) & 0xFFL;
            return ((a << 24) | (b << 16) | (c << 8) | d) & 0xFFFFFFFFL;
        }

        /**
         * Merge overlapping/adjacent intervals; input intervals may be unsorted.
         */
        private static List<long[]> mergeIntervals(List<long[]> intervals) {
            if (intervals.isEmpty()) return List.of();
            var list = new ArrayList<>(intervals);
            var out = new ArrayList<long[]>();
            var curStart = list.getFirst()[0];
            var curEnd = list.getFirst()[1];
            for (int i = 1; i < list.size(); i++) {
                long s = list.get(i)[0], e = list.get(i)[1];
                if (s <= curEnd + 1) {
                    // overlap or adjacent -> extend
                    curEnd = Math.max(curEnd, e);
                } else {
                    out.add(new long[]{curStart, curEnd});
                    curStart = s;
                    curEnd = e;
                }
            }
            out.add(new long[]{curStart, curEnd});
            return out;
        }

        private static List<long[]> buildAllowedFromReserved(List<long[]> reserved) {
            var allowed = new ArrayList<long[]>();
            var fullStart = 0L;
            var fullEnd = 0xFFFFFFFFL;

            var cursor = fullStart;
            for (var r : reserved) {
                var rs = r[0];
                if (cursor < rs) allowed.add(new long[]{cursor, rs - 1});
                cursor = Math.max(cursor, r[1] + 1);
                if (cursor > fullEnd) break;
            }
            if (cursor <= fullEnd) allowed.add(new long[]{cursor, fullEnd});
            return allowed;
        }
    }
}
