package com.tausib.jobhunter.web;

import com.tausib.jobhunter.config.JobHunterProperties;
import com.tausib.jobhunter.domain.DiscoveryRun;
import com.tausib.jobhunter.repo.DiscoveryRunRepository;
import com.tausib.jobhunter.service.DiscoveryScheduler;
import com.tausib.jobhunter.service.DiscoveryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/discovery")
public class DiscoveryController {

    private final DiscoveryService discovery;
    private final DiscoveryScheduler scheduler;
    private final DiscoveryRunRepository runs;
    private final JobHunterProperties props;

    public DiscoveryController(DiscoveryService discovery, DiscoveryScheduler scheduler,
                               DiscoveryRunRepository runs, JobHunterProperties props) {
        this.discovery = discovery;
        this.scheduler = scheduler;
        this.runs = runs;
        this.props = props;
    }

    /** Start a run in the background. Check progress with GET /api/discovery/runs. */
    @PostMapping("/run")
    public ResponseEntity<Map<String, Object>> runNow() {
        if (discovery.isRunning()) {
            return ResponseEntity.status(409).body(Map.<String, Object>of("message", "A run is already in progress"));
        }
        scheduler.runNowAsync();
        return ResponseEntity.accepted().body(Map.<String, Object>of("message", "Discovery started. Poll GET /api/discovery/runs"));
    }

    @PostMapping("/pause")
    public Map<String, Object> pause() {
        discovery.pause();
        return status();
    }

    @PostMapping("/resume")
    public Map<String, Object> resume() {
        discovery.resume();
        return status();
    }

    @GetMapping("/status")
    public Map<String, Object> status() {
        var d = props.discovery();
        return Map.<String, Object>of(
                "paused", discovery.isPaused(),
                "running", discovery.isRunning(),
                "schedulerEnabled", d.schedulerEnabled(),
                "cron", d.cron() + " (" + d.zone() + ")",
                "maxApifyItemsPerRun", d.maxItemsPerSearch() * d.maxSearchesPerRun(),
                "apifyTokenConfigured", props.apify().token() != null && !props.apify().token().isBlank());
    }

    @GetMapping("/runs")
    public List<DiscoveryRun> recentRuns() {
        return runs.findTop20ByOrderByStartedAtDesc();
    }
}
