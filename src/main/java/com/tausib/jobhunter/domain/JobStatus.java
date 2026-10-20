package com.tausib.jobhunter.domain;

public enum JobStatus {
    NEW,            // found, score below shortlist threshold
    SHORTLISTED,    // score >= threshold, worth a look
    APPLIED,
    INTERVIEW,
    OFFER,
    REJECTED,       // company said no
    SKIPPED,        // you decided not to apply
    CLOSED          // posting expired
}
