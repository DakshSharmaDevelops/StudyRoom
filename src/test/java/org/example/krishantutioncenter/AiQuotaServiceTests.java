package org.example.krishantutioncenter;

import org.example.krishantutioncenter.config.*;
import org.example.krishantutioncenter.model.*;
import org.example.krishantutioncenter.repository.*;
import org.example.krishantutioncenter.service.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:ai-quota-test;DB_CLOSE_DELAY=-1",
        "app.ai.monthly-request-limit=1"
})
class AiQuotaServiceTests {

    @Autowired
    private AiQuotaService quota;

    @Autowired
    private AiUsageRepository usage;

    @Test
    void concurrentRequestsCannotExceedTheConfiguredMonthlyLimit() throws Exception {
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger reserved = new AtomicInteger();
        AtomicInteger blocked = new AtomicInteger();
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<?> first = executor.submit(() -> reserveAfter(start, reserved, blocked));
            Future<?> second = executor.submit(() -> reserveAfter(start, reserved, blocked));
            start.countDown();
            first.get();
            second.get();
        } finally {
            executor.shutdownNow();
        }
        assertEquals(1, reserved.get());
        assertEquals(1, blocked.get());
        assertEquals(1, usage.findByMonthKey(java.time.YearMonth.now().toString())
                .orElseThrow().getRequestsUsed());
    }

    private void reserveAfter(CountDownLatch start, AtomicInteger reserved, AtomicInteger blocked) {
        try {
            start.await();
            quota.reserve();
            reserved.incrementAndGet();
        } catch (NotesAiService.AiUnavailableException exception) {
            blocked.incrementAndGet();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(exception);
        }
    }
}
