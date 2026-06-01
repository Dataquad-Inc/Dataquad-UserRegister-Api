package com.dataquadinc.client;

import com.dataquadinc.dto.BdmEmployeeDTO;
import com.dataquadinc.dto.CandidateStatsResponse;
import com.dataquadinc.dto.Coordinator_DTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@FeignClient(
        name = "requirements-service",
        url = "${requirements.service.url}"
)
public interface RequirementFeignClient {

    @GetMapping("/requirements/bdmlist")
    List<BdmEmployeeDTO> getBdmList();

    @GetMapping("/requirements/stats")
    CandidateStatsResponse getStats();

    @GetMapping("/requirements/coordinatorstats")
    List<Coordinator_DTO> getCoordinatorStats();
}
