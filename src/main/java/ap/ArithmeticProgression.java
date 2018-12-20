package ap;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

/**
 * Calculates the sum of all the arithmetic terms using multiple threads
 */
public final class ArithmeticProgression {
    /**
     * Calculates the summation of all the AP terms supplied using Executors
     * @param firstTerm     First term of AP
     * @param totalTerms    Total number of terms in AP
     * @param difference    Difference between two consecutive AP terms
     * @param threads       Number of threads to be used for calculation of sum
     * @return  The sum of all the AP terms
     * @throws CancellationException    if thread is interrupted or any task throws an exception
     * @throws IllegalStateException    if sanity check fails on the parameters passed
     */
    public static long computeSum(final int firstTerm, final int totalTerms, final int difference, final int threads) throws CancellationException {
        sanityCheck(totalTerms, threads);
        ExecutorService service = Executors.newCachedThreadPool();
        List<Future<Long>> futures = new ArrayList<>();
        try {
            CompletionService<Long> completionService = new ExecutorCompletionService<>(service);

            final int slices = totalTerms / threads;
            int termsToProcess;
            int effectiveFirstTerm = firstTerm;
            for (int i = 1; i <= threads; i++) {
                termsToProcess = (i == threads) ? totalTerms - (i - 1) * slices : slices;
                futures.add(completionService.submit(new SummationTask(effectiveFirstTerm, difference, termsToProcess)));
                effectiveFirstTerm += (termsToProcess * difference);
            }

            Long totalSum = 0L;
            while (!futures.isEmpty()) {
                if (Thread.currentThread().isInterrupted()) {
                    throw new InterruptedException();
                }

                Future<Long> result = completionService.take();
                futures.remove(result);

                totalSum += result.get();
            }

            return totalSum;
        } catch (InterruptedException | ExecutionException e) {
            // cancel any pending/running tasks
            futures.forEach(future -> future.cancel(true));
            throw new CancellationException(e.getMessage());
        } finally {
            service.shutdown();
        }
    }

    /**
     * Checks sanity of arguments for a given AP
     * @param totalTerms    Total number of terms for the AP series
     * @param threads       Number of threads to be used for calculating AP sum
     * @throws IllegalStateException If sanity check fails
     */
    private static void sanityCheck(final int totalTerms, final int threads) {
        if((totalTerms < 0) || (threads > totalTerms))
            throw new IllegalStateException(String.format("Total Terms: %d, Threads: %d; total terms must be greater than 0 AND total terms needs to be at least as great as total number of terms", totalTerms, threads));
    }

    /**
     * Implements Summation of AP terms for the given series
     */
    private static class SummationTask implements Callable<Long> {
        private final int a;
        private final int d;
        private final int n;

        SummationTask(int a, int d, int n) {
            this.a = a;
            this.d = d;
            this.n = n;
        }

        public Long call() throws Exception {
            long sum = a;

            for(int i = 2; i <= n; ++i) {
                sum += (a + (i - 1) * d);
                if(Thread.currentThread().isInterrupted()) {
                    throw new InterruptedException();
                }
            }

            return sum;
        }
    }

}
