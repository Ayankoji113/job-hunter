package com.tausib.jobhunter.core;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** Pure unit tests — no Spring, no database, no network. Run with: mvn test */
class CoreLogicTest {

    private static JobListing listing(String title, String desc) {
        return new JobListing("s", null, title, "C", "Pune", "https://x.com/" + title.hashCode(), null, desc, null, "k");
    }

    private static JobListing at(String location) {
        return new JobListing("s", null, "Java Developer Intern", "C", location, "https://x.com/" + location, null, "", null, "k");
    }

    @Test
    void mapsInternshalaItem() {
        Map<String, Object> item = new HashMap<>();
        item.put("title", "Java Development Internship");
        item.put("company", "Acme Labs");
        item.put("location", "Work from home");
        item.put("isWorkFromHome", true);
        item.put("canonicalUrl", "https://internshala.com/internship/detail/java-123");
        item.put("applyUrl", "https://internshala.com/internship/detail/java-123");
        item.put("description", "About the internship");
        item.put("skills", java.util.List.of("Java", "Spring Boot"));
        item.put("jobKey", "3301227");

        JobListing j = ApifyItemMapper.map(item, "internshala", "java");
        assertNotNull(j);
        assertEquals("Work from home", j.location());            // already says home: no "(Remote)" added
        assertTrue(j.description().contains("Skills: Java, Spring Boot"));
        assertTrue(new JobFilter(1, java.util.List.of("pune", "remote", "work from home")).check(j).keep());
    }

    @Test
    void mapsNaukriItemWithNestedListsAndLinkedInRemoteFlag() {
        Map<String, Object> item = new HashMap<>();
        item.put("title", "Java Developer Fresher");
        item.put("company", Map.of("name", "Zensar"));
        item.put("locations", java.util.List.of(Map.of("label", "Pune")));
        item.put("url", "https://www.naukri.com/job-listings-java-1");
        item.put("description", Map.of("full", "<p>Core Java, Spring Boot</p>"));
        item.put("skills", Map.of("preferred", java.util.List.of(), "other", java.util.List.of("hibernate")));
        item.put("createdDateText", "2026-10-14 18:28:31");

        JobListing n = ApifyItemMapper.map(item, "naukri", "k");
        assertEquals("Zensar", n.company());
        assertEquals("Pune", n.location());
        assertTrue(n.description().startsWith("Core Java, Spring Boot"));
        assertTrue(n.description().contains("hibernate"));
        assertEquals("2026-10-14 18:28:31", n.postedAt());

        Map<String, Object> li = new HashMap<>(Map.of("title", "Java Intern", "linkedinUrl", "https://linkedin.com/jobs/view/5",
                "location", Map.of("linkedinText", "India"), "workplaceType", "remote"));
        assertEquals("India (Remote)", ApifyItemMapper.map(li, "li", "k").location());
    }

    @Test
    void scoreIsCappedWhenJobHasNoJavaOrSpring() {
        MatchScorer scorer = new MatchScorer(MatchScorer.defaultProfile());
        assertTrue(scorer.score(listing("Website Developer", "HTML, CSS, JavaScript, Git")).score() <= 50);
        assertTrue(scorer.score(listing("Java Intern", "Java, REST APIs")).score() >= 60);
    }

    @Test
    void locationFilterKeepsOnlyWantedCities() {
        JobFilter pune = new JobFilter(1, java.util.List.of("Pune"));
        assertTrue(pune.check(at("Pune City, Maharashtra, India")).keep());
        assertTrue(pune.check(at("Pune District, Maharashtra, India")).keep());
        assertFalse(pune.check(at("India")).keep());
        assertFalse(pune.check(at("Greater Bengaluru Area")).keep());
        assertFalse(pune.check(at(null)).keep());
        assertTrue(new JobFilter(1).check(at("India")).keep());   // no list = any location
    }

    @Test
    void normalisesLinkedInUrls() {
        assertEquals("https://linkedin.com/jobs/view/123456",
                Fingerprints.normalizeUrl("https://in.linkedin.com/jobs/view/123456/?trk=abc#x"));
        assertEquals("https://linkedin.com/jobs/view/123456",
                Fingerprints.normalizeUrl("https://www.LinkedIn.com/jobs/view/123456"));
    }

    @Test
    void fingerprintIgnoresCaseAndPunctuation() {
        assertEquals(Fingerprints.of("Acme Pvt. Ltd", "Java Intern", "Pune"),
                Fingerprints.of("acme pvt ltd", "JAVA intern ", "pune"));
    }

    @Test
    void mapsNestedActorOutput() {
        Map<String, Object> item = new HashMap<>();
        item.put("title", "Backend Developer Intern");
        item.put("company", Map.of("name", "NxtPe"));
        item.put("location", Map.of("linkedinText", "Pune, Maharashtra, India"));
        item.put("linkedinUrl", "https://in.linkedin.com/jobs/view/999/?trk=x");
        item.put("descriptionText", "<p>Java, Spring Boot</p>");
        item.put("id", 999);

        JobListing j = ApifyItemMapper.map(item, "src", "java intern");
        assertNotNull(j);
        assertEquals("NxtPe", j.company());
        assertTrue(j.location().startsWith("Pune"));
        assertEquals("https://linkedin.com/jobs/view/999", j.url());
        assertFalse(j.description().contains("<p>"));
        assertEquals("999", j.externalId());
    }

    @Test
    void mapsFlatActorOutputAndRejectsItemsWithoutUrl() {
        JobListing f = ApifyItemMapper.map(
                Map.of("jobTitle", "Java Developer", "companyName", "Iris", "jobUrl", "https://x.com/j/1"), "s", "k");
        assertNotNull(f);
        assertEquals("Iris", f.company());
        assertNull(ApifyItemMapper.map(Map.of("title", "x"), "s", "k"));
    }

    @Test
    void filterKeepsFreshersAndDropsSeniorOrScam() {
        JobFilter filter = new JobFilter(1);
        assertTrue(filter.check(listing("Java Developer Intern", "3 months, 0-1 year experience")).keep());
        assertTrue(filter.check(listing("Software Engineer", "Spring Boot, REST, SQL")).keep());
        assertFalse(filter.check(listing("Senior Java Developer", "5+ years experience")).keep());
        assertFalse(filter.check(listing("Java Developer", "Minimum 3+ years of experience")).keep());
        assertFalse(filter.check(listing("Software Engineer", "2-4 years of experience required")).keep());
        assertFalse(filter.check(listing("Java Intern", "Pay a registration fee of 2000")).keep());
    }

    @Test
    void filterMatchesWholeWordsOnly() {
        JobFilter filter = new JobFilter(1);
        // "international"/"internal" are not "intern"; bare "graduate" is not entry-level
        assertFalse(filter.check(listing("Java Developer", "5+ years of experience. Work with international clients.")).keep());
        assertFalse(filter.check(listing("Backend Engineer", "Minimum 4 years experience building internal tools.")).keep());
        assertFalse(filter.check(listing("Software Engineer", "3+ years experience. Bachelor's degree, graduate preferred.")).keep());
        // "Leading" is not "lead"
        assertTrue(filter.check(listing("Java Developer (Leading fintech)", "freshers welcome")).keep());
        assertFalse(filter.check(listing("Team Lead Java", "")).keep());
        assertFalse(filter.check(listing("Sr. Java Developer", "")).keep());
        assertTrue(filter.check(listing("Software Engineer Intern", "5 years of company history")).keep());
    }

    @Test
    void scorerRewardsMatchingStackAndListsGaps() {
        MatchScorer scorer = new MatchScorer(MatchScorer.defaultProfile());
        MatchScorer.Result r = scorer.score(listing("Backend Developer Intern",
                "Java, Spring Boot, REST APIs, PostgreSQL, Git. Freshers welcome. Kafka nice to have."));
        assertTrue(r.score() >= 80, "score was " + r.score());
        assertTrue(r.matched().contains("Spring Boot"));
        assertTrue(r.missing().contains("Kafka"));
        assertFalse(r.matched().contains("JavaScript"));

        assertTrue(scorer.score(listing("Python Developer", "Django, Flask, AWS, Kubernetes")).score() < 30);
        assertFalse(scorer.score(listing("Dev", "Join the rest of the team")).matched().contains("REST APIs"));
    }

    @Test
    void inputTemplateEscapesAndFillsPlaceholders() {
        String out = InputTemplates.render(
                "{\"keyword\": \"{keyword}\", \"location\": \"{location}\", \"limit\": {limit}}",
                "Java \"Intern\"", "Pune, India", 25);
        assertEquals("{\"keyword\": \"Java \\\"Intern\\\"\", \"location\": \"Pune, India\", \"limit\": 25}", out);
    }
}
