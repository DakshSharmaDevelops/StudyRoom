package org.example.krishantutioncenter.service;

import org.example.krishantutioncenter.config.*;
import org.example.krishantutioncenter.model.*;
import org.example.krishantutioncenter.repository.*;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.YearMonth;

@Service
public class AiQuotaService implements ApplicationRunner {

    private final AiQuotaLockRepository locks;
    private final AiUsageRepository usage;
    private final AiFeatureProperties properties;

    public AiQuotaService(AiQuotaLockRepository locks, AiUsageRepository usage, AiFeatureProperties properties) {
        this.locks = locks;
        this.usage = usage;
        this.properties = properties;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        locks.findById(1).orElseGet(() -> locks.save(new AiQuotaLock(1)));
    }

    @Transactional
    public void reserve() {
        locks.findAndLock(1).orElseThrow(() -> new IllegalStateException("AI quota lock is not initialized."));
        String month = YearMonth.now().toString();
        AiUsage monthlyUsage = usage.findByMonthKey(month)
                .orElseGet(() -> usage.saveAndFlush(new AiUsage(month)));
        if (properties.getMonthlyRequestLimit() < 1
                || monthlyUsage.getRequestsUsed() >= properties.getMonthlyRequestLimit()) {
            throw new NotesAiService.AiUnavailableException(
                    "The monthly free-tier limit has been reached. AI will be available again next month.");
        }
        monthlyUsage.increment();
        usage.saveAndFlush(monthlyUsage);
    }
}
