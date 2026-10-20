package com.tausib.jobhunter.service;

import com.tausib.jobhunter.config.JobHunterProperties;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Runs discovery on a cron (weekdays 9 AM IST by default) — only when
 * jobhunter.discovery.scheduler-enabled=true. Manual runs go through {@link #runNowAsync()}.
 */
@Component
public class DiscoveryScheduler {

    private final DiscoveryService discovery;
    private final JobHunterProperties props;

    public DiscoveryScheduler(DiscoveryService discovery, JobHunterProperties props) {
        this.discovery = discovery;
        this.props = props;
    }

    @Scheduled(cron = "${jobhunter.discovery.cron}", zone = "${jobhunter.discovery.zone}")
    public void scheduledRun() {
        if (props.discovery().schedulerEnabled()) {
            discovery.run("scheduled");
        }
    }

    /** Background thread so the HTTP request returns immediately (a run can take minutes). */
    @Async
    public void runNowAsync() {
        discovery.run("manual");
    }
}
