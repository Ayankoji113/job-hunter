package com.tausib.jobhunter.domain;

import jakarta.persistence.*;

import java.time.Instant;

/** Audit log: one row per discovery run, so every automated action has a record. */
@Entity
@Table(name = "discovery_run")
public class DiscoveryRun {

    public enum Status { RUNNING, SUCCEEDED, PARTIAL, FAILED, SKIPPED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "trigger_type", length = 20)   // "trigger" is an SQL keyword, so rename the column
    private String trigger;          // "manual" or "scheduled"

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Status status = Status.RUNNING;

    private Instant startedAt = Instant.now();
    private Instant finishedAt;

    private int searchesRun;
    private int itemsFetched;
    private int newJobs;
    private int shortlisted;
    private int duplicates;
    private int filteredOut;

    @Column(columnDefinition = "text")
    private String log;              // per-search notes and errors

    protected DiscoveryRun() {}

    public DiscoveryRun(String trigger) {
        this.trigger = trigger;
    }

    public void appendLog(String line) {
        log = (log == null ? "" : log + "\n") + line;
    }

    public void finish(Status status) {
        this.status = status;
        this.finishedAt = Instant.now();
    }

    public void incSearches() { searchesRun++; }
    public void addFetched(int n) { itemsFetched += n; }
    public void incNew() { newJobs++; }
    public void incShortlisted() { shortlisted++; }
    public void incDuplicates() { duplicates++; }
    public void incFiltered() { filteredOut++; }

    public Long getId() { return id; }
    public String getTrigger() { return trigger; }
    public Status getStatus() { return status; }
    public Instant getStartedAt() { return startedAt; }
    public Instant getFinishedAt() { return finishedAt; }
    public int getSearchesRun() { return searchesRun; }
    public int getItemsFetched() { return itemsFetched; }
    public int getNewJobs() { return newJobs; }
    public int getShortlisted() { return shortlisted; }
    public int getDuplicates() { return duplicates; }
    public int getFilteredOut() { return filteredOut; }
    public String getLog() { return log; }
}
