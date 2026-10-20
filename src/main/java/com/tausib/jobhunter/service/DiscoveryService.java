package com.tausib.jobhunter.service;

import com.tausib.jobhunter.apify.ApifyClient;
import com.tausib.jobhunter.config.JobHunterProperties;
import com.tausib.jobhunter.config.JobHunterProperties.Search;
import com.tausib.jobhunter.config.JobHunterProperties.Source;
import com.tausib.jobhunter.core.*;
import com.tausib.jobhunter.domain.DiscoveryRun;
import com.tausib.jobhunter.domain.JobPosting;
import com.tausib.jobhunter.domain.JobStatus;
import com.tausib.jobhunter.repo.DiscoveryRunRepository;
import com.tausib.jobhunter.repo.JobPostingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * The pipeline:  Apify search -> map -> filter -> de-dup -> score -> save.
 * One bad search never kills the whole run; errors go into the run log.
 */
@Service
public class DiscoveryService {

    private static final Logger log = LoggerFactory.getLogger(DiscoveryService.class);

    private final ApifyClient apify;
    private final JobFilter filter;
    private final MatchScorer scorer;
    private final JobPostingRepository jobs;
    private final DiscoveryRunRepository runs;
    private final JobHunterProperties props;

    private final AtomicBoolean paused = new AtomicBoolean(false);
    private final AtomicBoolean running = new AtomicBoolean(false);

    public DiscoveryService(ApifyClient apify, JobFilter filter, MatchScorer scorer,
                            JobPostingRepository jobs, DiscoveryRunRepository runs,
                            JobHunterProperties props) {
        this.apify = apify;
        this.filter = filter;
        this.scorer = scorer;
        this.jobs = jobs;
        this.runs = runs;
        this.props = props;
    }

    public DiscoveryRun run(String trigger) {
        DiscoveryRun run = new DiscoveryRun(trigger);

        if (paused.get()) {
            run.appendLog("Skipped: discovery is paused.");
            run.finish(DiscoveryRun.Status.SKIPPED);
            return runs.save(run);
        }
        if (!running.compareAndSet(false, true)) {
            run.appendLog("Skipped: another run is already in progress.");
            run.finish(DiscoveryRun.Status.SKIPPED);
            return runs.save(run);
        }
        try {
            String token = props.apify().token();
            if (token == null || token.isBlank()) {
                run.appendLog("FAILED: APIFY_TOKEN is not set. Add it to your environment / .env file.");
                run.finish(DiscoveryRun.Status.FAILED);
                return runs.save(run);
            }
            run = runs.save(run);
            int errors = executeSearches(run);
            run.finish(errors == 0 ? DiscoveryRun.Status.SUCCEEDED
                    : run.getSearchesRun() > errors ? DiscoveryRun.Status.PARTIAL
                    : DiscoveryRun.Status.FAILED);
            log.info("Discovery run {} done: fetched={}, new={}, shortlisted={}, dupes={}, filtered={}",
                    run.getId(), run.getItemsFetched(), run.getNewJobs(), run.getShortlisted(),
                    run.getDuplicates(), run.getFilteredOut());
            return runs.save(run);
        } finally {
            running.set(false);
        }
    }

    private int executeSearches(DiscoveryRun run) {
        var d = props.discovery();
        int budget = d.maxSearchesPerRun();
        int errors = 0;

        for (Source source : props.apify().sources()) {
            if (!source.enabled()) continue;
            List<Search> searches = source.searches() == null || source.searches().isEmpty() ? d.searches() : source.searches();
            for (Search search : searches) {
                if (budget-- <= 0) {
                    run.appendLog("Stopped: max-searches-per-run reached.");
                    return errors;
                }
                if (paused.get()) {
                    run.appendLog("Stopped mid-run: paused by user.");
                    return errors;
                }
                run.incSearches();
                String label = source.actorId() + " | \"" + search.keyword() + "\" @ " + search.location();
                try {
                    String input = InputTemplates.render(source.inputTemplate(),
                            search.keyword(), search.location(), d.maxItemsPerSearch());
                    List<Map<String, Object>> items = apify.runSync(source.actorId(), input);
                    run.addFetched(items.size());
                    int added = processItems(items, source.actorId(), search.keyword(), run);
                    run.appendLog("OK   " + label + " -> " + items.size() + " items, " + added + " new");
                } catch (Exception e) {
                    errors++;
                    run.appendLog("ERR  " + label + " -> " + e.getClass().getSimpleName() + ": " + e.getMessage());
                    log.warn("Search failed: {}", label, e);
                }
                runs.save(run); // progress is visible while the run is still going
                sleepBetweenSearches(d.delaySecondsBetweenSearches());
            }
        }
        return errors;
    }

    private int processItems(List<Map<String, Object>> items, String source, String keyword, DiscoveryRun run) {
        int added = 0;
        int minScore = props.discovery().minScoreToShortlist();
        for (Map<String, Object> item : items) {
            JobListing listing = ApifyItemMapper.map(item, source, keyword);
            if (listing == null) { run.incFiltered(); continue; }

            if (jobs.existsByUrl(listing.url()) || jobs.existsByFingerprint(listing.fingerprint())) {
                run.incDuplicates();
                continue;
            }
            JobFilter.Result verdict = filter.check(listing);
            if (!verdict.keep()) {
                run.incFiltered();
                continue;
            }
            MatchScorer.Result match = scorer.score(listing);
            JobPosting p = toEntity(listing, match);
            p.setStatus(match.score() >= minScore ? JobStatus.SHORTLISTED : JobStatus.NEW);
            jobs.save(p);
            run.incNew();
            if (p.getStatus() == JobStatus.SHORTLISTED) run.incShortlisted();
            added++;
        }
        return added;
    }

    private static JobPosting toEntity(JobListing l, MatchScorer.Result m) {
        JobPosting p = new JobPosting();
        p.setSource(l.source());
        p.setExternalId(l.externalId());
        p.setTitle(truncate(l.title(), 500));
        p.setCompany(truncate(l.company(), 300));
        p.setLocation(truncate(l.location(), 300));
        p.setUrl(truncate(l.url(), 1000));
        p.setApplyUrl(truncate(l.applyUrl(), 1000));
        p.setDescription(l.description());
        p.setPostedAt(truncate(l.postedAt(), 255));
        p.setSearchKeyword(l.searchKeyword());
        p.setFingerprint(l.fingerprint());
        p.setMatchScore(m.score());
        p.setMatchedSkills(truncate(String.join(", ", m.matched()), 1000));
        p.setMissingSkills(truncate(String.join(", ", m.missing()), 1000));
        return p;
    }

    private static String truncate(String s, int max) {
        return s == null || s.length() <= max ? s : s.substring(0, max);
    }

    private static void sleepBetweenSearches(int seconds) {
        if (seconds <= 0) return;
        try {
            Thread.sleep(seconds * 1000L);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // ---- controls ----
    public void pause() { paused.set(true); }
    public void resume() { paused.set(false); }
    public boolean isPaused() { return paused.get(); }
    public boolean isRunning() { return running.get(); }
}
