package org.example.service;

import org.example.dao.BatchProgressResponse;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class BatchProgressTrackerTest {

    @Test
    void tracker_initialState_returnsZeroPercentAndRunning() {
        BatchProgressTracker tracker = new BatchProgressTracker("admin_10", 100);
        assertEquals("admin_10", tracker.getExecuteKey());
        assertEquals(100, tracker.getTotal());
        assertEquals(0, tracker.getProcessed());
        assertFalse(tracker.isCompleted());

        BatchProgressResponse response = tracker.toProgressResponse();
        assertEquals("admin_10", response.getExecuteKey());
        assertEquals("RUNNING", response.getStatus());
        assertEquals(0.0, response.getPercent());
    }

    @Test
    void tracker_increments_calculatesCorrectPercent() {
        BatchProgressTracker tracker = new BatchProgressTracker("admin_10", 10);
        tracker.increment();
        tracker.increment();
        tracker.increment();

        assertEquals(3, tracker.getProcessed());

        BatchProgressResponse response = tracker.toProgressResponse();
        assertEquals(30.0, response.getPercent());
        assertEquals("RUNNING", response.getStatus());
    }

    @Test
    void tracker_complete_setsCompletedStatusAndHundredPercent() {
        BatchProgressTracker tracker = new BatchProgressTracker("admin_10", 50);
        tracker.increment();
        org.example.dao.BatchExecutionResponse mockResult = new org.example.dao.BatchExecutionResponse();
        mockResult.setTotalSent(50);
        tracker.complete(mockResult);

        assertTrue(tracker.isCompleted());

        BatchProgressResponse response = tracker.toProgressResponse();
        assertEquals("COMPLETED", response.getStatus());
        assertEquals(100.0, response.getPercent());
        assertNotNull(response.getResult());
        assertEquals(50, response.getResult().getTotalSent());
    }

    @Test
    void tracker_concurrentIncrements_atomicAccuracy() throws Exception {
        int numThreads = 20;
        int incrementsPerThread = 500;
        int totalExpected = numThreads * incrementsPerThread;

        BatchProgressTracker tracker = new BatchProgressTracker("load_test_key", totalExpected);
        ExecutorService pool = Executors.newFixedThreadPool(numThreads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(numThreads);

        for (int t = 0; t < numThreads; t++) {
            pool.submit(() -> {
                try {
                    startLatch.await();
                    for (int i = 0; i < incrementsPerThread; i++) {
                        tracker.increment();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    finishLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        assertTrue(finishLatch.await(5, TimeUnit.SECONDS));
        pool.shutdown();

        assertEquals(totalExpected, tracker.getProcessed());

        tracker.complete();
        BatchProgressResponse res = tracker.toProgressResponse();
        assertEquals(100.0, res.getPercent());
        assertEquals("COMPLETED", res.getStatus());
    }
}
