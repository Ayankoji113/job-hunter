package com.tausib.jobhunter.repo;

import com.tausib.jobhunter.domain.DiscoveryRun;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DiscoveryRunRepository extends JpaRepository<DiscoveryRun, Long> {

    List<DiscoveryRun> findTop20ByOrderByStartedAtDesc();
}
