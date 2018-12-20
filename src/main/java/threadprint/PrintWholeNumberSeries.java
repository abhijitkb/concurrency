package threadprint;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public final class PrintWholeNumberSeries {
    private volatile boolean stop = false;
    private ReentrantLock lock = new ReentrantLock();
    private Condition oddCond = lock.newCondition();
    private Condition evenCond = lock.newCondition();

    private final Thread oddPrinter;
    private final Thread evenPrinter;

    public PrintWholeNumberSeries() {
        this.oddPrinter = new Thread(new PrintSeriesTask(1, 2, oddCond, evenCond), "Odd Printer Thread");
        this.evenPrinter = new Thread(new PrintSeriesTask(0, 2, evenCond, oddCond), "Even Printer Thread");
    }

    public void start() throws InterruptedException {
        this.oddPrinter.start();
        this.evenPrinter.start();

        // Give the task threads some time
        TimeUnit.SECONDS.sleep(1);

        try {
            this.lock.lockInterruptibly();
            this.evenCond.signal();
        } finally {
            this.lock.unlock();
        }
    }

    public void stop() {
        this.stop = true;

        this.oddPrinter.interrupt();
        this.evenPrinter.interrupt();
    }

    private class PrintSeriesTask implements Runnable {
        private final int initNumber;
        private final int incrBy;
        private final Condition listen;
        private final Condition notify;

        public PrintSeriesTask(int initNumber, int incrBy, Condition listen, Condition notify) {
            this.initNumber = initNumber;
            this.incrBy = incrBy;
            this.listen = listen;
            this.notify = notify;
        }

        @Override
        public void run() {
            int number = initNumber;
            while(!stop) {
                try {
                    lock.lockInterruptibly();
                    listen.awaitUninterruptibly();
                    System.out.printf("%d ", number);
                    notify.signal();
                } catch (InterruptedException e) {
                    // wake up to check if the interruption is due to a cancellation request
                } finally {
                    lock.unlock();
                }
                number += incrBy;
            }
        }
    }
}
