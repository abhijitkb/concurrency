package threadprint;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Prints the series 1 2 3 1 2 3 1 2 3 ... indefinitely
 * <pre>
 *     Has three tasks:
 *      Task 1 prints 1 1 1 1 ... indefinitely
 *      Task 2 prints 2 2 2 2 2 ... indefinitely
 *      Task 3 prints 3 3 3 3 3 ... indefinitely
 * </pre>
 */
public final class PrintSeriesSeqentially {
    private final ReentrantLock lock = new ReentrantLock();
    private volatile boolean stop = false;

    private List<Condition> conditions = new ArrayList<>();
    private List<Thread> threads = new ArrayList<>();

    public PrintSeriesSeqentially(final int totalThreads) {
        // Initialise the condition variables
        for (int i = 0; i < totalThreads; i++) {
            conditions.add(lock.newCondition());
        }

        // Initialise the threads
        for (int i = 0; i < totalThreads; i++) {
            threads.add(
                    new Thread(
                            new PrintSeriesTask(
                                    i + 1, // number to print
                                    conditions.get(i), // listen condition
                                    conditions.get((i + 1) % totalThreads) // signal condition >> the very next thread should be awakened
                                    )
                    )
            );
        }
    }

    public void start() throws InterruptedException {
        threads.forEach(Thread::start);

        // Give the task threads some time
        TimeUnit.SECONDS.sleep(1);

        // Signal the first thread to start
        try {
            this.lock.lockInterruptibly();
            conditions.get(0).signal();
        } finally {
            this.lock.unlock();
        }
    }

    public void stop() {
        this.stop = true;
        // trigger cancellation policy
        threads.forEach(Thread::interrupt);

        threads.forEach(thread -> {
            try {
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
    }

    private final class PrintSeriesTask implements Runnable {
        private final int numberToPrint;
        private final Condition listen;
        private final Condition notify;

        public PrintSeriesTask(int numberToPrint, final Condition listen, final Condition notify) {
            this.numberToPrint = numberToPrint;
            this.listen = listen;
            this.notify = notify;
        }

        @Override
        public void run() {
            while(!stop) {
                try {
                    lock.lockInterruptibly();
                    // wait till its condition is signalled
                    this.listen.awaitUninterruptibly();
                    System.out.printf("%d ", this.numberToPrint);
                    // signal the next condition
                    this.notify.signal();
                } catch (InterruptedException e) {
                    // just wake up to see if the task has been cancelled!
                } finally {
                    lock.unlock();
                }
            }
        }
    }
}
