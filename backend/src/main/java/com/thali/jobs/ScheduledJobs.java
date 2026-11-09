package com.thali.jobs;

import java.time.Clock;
import java.time.YearMonth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.thali.billing.BillingService;
import com.thali.booking.NoShowService;
import com.thali.config.AppProperties;

/** Background jobs. Each one is idempotent, so a missed or repeated run is harmless. */
@Component
public class ScheduledJobs {

    private static final Logger log = LoggerFactory.getLogger(ScheduledJobs.class);

    private final NoShowService noShows;
    private final BillingService billing;
    private final AppProperties.Billing cfg;
    private final Clock clock;

    public ScheduledJobs(NoShowService noShows, BillingService billing, AppProperties props, Clock clock) {
        this.noShows = noShows;
        this.billing = billing;
        this.cfg = props.billing();
        this.clock = clock;
    }

    /** Every 5 minutes: booked meals whose serving window ended become NO_SHOW. */
    @Scheduled(initialDelayString = "PT1M", fixedDelayString = "PT5M")
    public void markNoShows() {
        guard("no-show", noShows::markNoShows);
    }

    /** Daily 00:30: unpaid bills past their due date become OVERDUE. */
    @Scheduled(cron = "0 30 0 * * *", zone = "${app.zone}")
    public void markOverdue() {
        guard("overdue", billing::markOverdue);
    }

    /** 1st of every month 02:00: bill the month that just ended. */
    @Scheduled(cron = "0 0 2 1 * *", zone = "${app.zone}")
    public void generateLastMonthBills() {
        if (!cfg.autoGenerate()) return;
        guard("auto-billing", () -> billing.generate(YearMonth.now(clock).minusMonths(1)));
    }

    private void guard(String name, Runnable job) {
        try {
            job.run();
        } catch (RuntimeException e) {
            log.error("Scheduled job '{}' failed", name, e);
        }
    }
}
