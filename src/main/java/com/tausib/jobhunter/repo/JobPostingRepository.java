package com.tausib.jobhunter.repo;

import com.tausib.jobhunter.domain.JobPosting;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JobPostingRepository extends JpaRepository<JobPosting, Long> {

    boolean existsByUrl(String url);

    boolean existsByFingerprint(String fingerprint);

    List<JobPosting> findAllByOrderByMatchScoreDescDiscoveredAtDesc();
}
