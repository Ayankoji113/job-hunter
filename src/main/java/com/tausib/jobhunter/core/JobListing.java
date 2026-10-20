package com.tausib.jobhunter.core;

/**
 * One job posting, normalised from whatever shape the Apify actor returned.
 * Plain record with no framework dependencies so it can be unit-tested anywhere.
 */
public record JobListing(
        String source,        // which actor produced it, e.g. "harvestapi/linkedin-job-search"
        String externalId,    // id from the source site, may be null
        String title,
        String company,
        String location,
        String url,           // normalised URL (used for de-duplication)
        String applyUrl,      // direct apply link if the actor gave one, else null
        String description,
        String postedAt,      // kept as text: actors return many date formats
        String searchKeyword  // the search that found it
) {
    /** Second de-dup key: same role at same company/location posted on two URLs. */
    public String fingerprint() {
        return Fingerprints.of(company, title, location);
    }
}
