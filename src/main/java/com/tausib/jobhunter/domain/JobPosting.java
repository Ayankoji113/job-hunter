package com.tausib.jobhunter.domain;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "job_posting", indexes = {
        @Index(name = "idx_job_fingerprint", columnList = "fingerprint"),
        @Index(name = "idx_job_status", columnList = "status")
})
public class JobPosting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String source;
    private String externalId;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(length = 300)
    private String company;

    @Column(length = 300)
    private String location;

    /** Normalised URL — primary de-dup key. */
    @Column(nullable = false, unique = true, length = 1000)
    private String url;

    @Column(length = 1000)
    private String applyUrl;

    @Column(columnDefinition = "text")
    private String description;

    private String postedAt;
    private String searchKeyword;

    /** company|title|location hash — secondary de-dup key. */
    @Column(length = 64)
    private String fingerprint;

    private int matchScore;

    @Column(length = 1000)
    private String matchedSkills;

    @Column(length = 1000)
    private String missingSkills;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private JobStatus status = JobStatus.NEW;

    @Column(length = 2000)
    private String notes;

    private Instant discoveredAt;
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        discoveredAt = Instant.now();
        updatedAt = discoveredAt;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    // ---- getters / setters ----
    public Long getId() { return id; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public String getExternalId() { return externalId; }
    public void setExternalId(String externalId) { this.externalId = externalId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getCompany() { return company; }
    public void setCompany(String company) { this.company = company; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public String getApplyUrl() { return applyUrl; }
    public void setApplyUrl(String applyUrl) { this.applyUrl = applyUrl; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getPostedAt() { return postedAt; }
    public void setPostedAt(String postedAt) { this.postedAt = postedAt; }
    public String getSearchKeyword() { return searchKeyword; }
    public void setSearchKeyword(String searchKeyword) { this.searchKeyword = searchKeyword; }
    public String getFingerprint() { return fingerprint; }
    public void setFingerprint(String fingerprint) { this.fingerprint = fingerprint; }
    public int getMatchScore() { return matchScore; }
    public void setMatchScore(int matchScore) { this.matchScore = matchScore; }
    public String getMatchedSkills() { return matchedSkills; }
    public void setMatchedSkills(String matchedSkills) { this.matchedSkills = matchedSkills; }
    public String getMissingSkills() { return missingSkills; }
    public void setMissingSkills(String missingSkills) { this.missingSkills = missingSkills; }
    public JobStatus getStatus() { return status; }
    public void setStatus(JobStatus status) { this.status = status; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public Instant getDiscoveredAt() { return discoveredAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
