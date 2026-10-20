package com.tausib.jobhunter.web;

import com.tausib.jobhunter.domain.JobPosting;
import com.tausib.jobhunter.domain.JobStatus;
import com.tausib.jobhunter.repo.JobPostingRepository;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

@RestController
@RequestMapping("/api/jobs")
public class JobController {

    private final JobPostingRepository jobs;

    public JobController(JobPostingRepository jobs) {
        this.jobs = jobs;
    }

    /** GET /api/jobs?status=SHORTLISTED&minScore=60&q=spring  (all params optional) */
    @GetMapping
    public List<JobPosting> list(@RequestParam(required = false) JobStatus status,
                                 @RequestParam(defaultValue = "0") int minScore,
                                 @RequestParam(defaultValue = "") String q) {
        return filtered(status, minScore, q);
    }

    public record UpdateRequest(JobStatus status, String notes) {}

    /** PATCH /api/jobs/12  body: {"status":"APPLIED","notes":"applied via careers page"} */
    @PatchMapping("/{id}")
    public JobPosting update(@PathVariable Long id, @RequestBody UpdateRequest req) {
        JobPosting job = jobs.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No job with id " + id));
        if (req.status() != null) job.setStatus(req.status());
        if (req.notes() != null) job.setNotes(req.notes());
        return jobs.save(job);
    }

    /** GET /api/jobs/export.csv?status=SHORTLISTED — opens straight in Excel. */
    @GetMapping(value = "/export.csv", produces = "text/csv")
    public ResponseEntity<String> exportCsv(@RequestParam(required = false) JobStatus status,
                                            @RequestParam(defaultValue = "0") int minScore) {
        StringBuilder csv = new StringBuilder("﻿"); // BOM so Excel reads UTF-8 (₹ etc.)
        csv.append("id,score,status,title,company,location,posted,url,apply_url,matched_skills,missing_skills,notes\n");
        for (JobPosting j : filtered(status, minScore, "")) {
            csv.append(j.getId()).append(',')
               .append(j.getMatchScore()).append(',')
               .append(j.getStatus()).append(',')
               .append(cell(j.getTitle())).append(',')
               .append(cell(j.getCompany())).append(',')
               .append(cell(j.getLocation())).append(',')
               .append(cell(j.getPostedAt())).append(',')
               .append(cell(j.getUrl())).append(',')
               .append(cell(j.getApplyUrl())).append(',')
               .append(cell(j.getMatchedSkills())).append(',')
               .append(cell(j.getMissingSkills())).append(',')
               .append(cell(j.getNotes())).append('\n');
        }
        String filename = "jobs-" + LocalDate.now() + ".csv";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(csv.toString());
    }

    private List<JobPosting> filtered(JobStatus status, int minScore, String q) {
        String needle = q.toLowerCase(Locale.ROOT);
        return jobs.findAllByOrderByMatchScoreDescDiscoveredAtDesc().stream()
                .filter(j -> status == null || j.getStatus() == status)
                .filter(j -> j.getMatchScore() >= minScore)
                .filter(j -> needle.isEmpty()
                        || contains(j.getTitle(), needle) || contains(j.getCompany(), needle))
                .toList();
    }

    private static boolean contains(String s, String needle) {
        return s != null && s.toLowerCase(Locale.ROOT).contains(needle);
    }

    private static String cell(String s) {
        if (s == null) return "";
        return "\"" + s.replace("\"", "\"\"") + "\"";
    }
}
