package com.dataquadinc.client;


import com.dataquadinc.dto.PlacementApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(
        name = "candidate-service",
        url = "${candidate.service.url}"
)
public interface PlacementFeignClient {

    @GetMapping("/placement/placements-list")
    PlacementApiResponse getPlacementsByUserId(
            @RequestParam("userId") String userId
    );

    @GetMapping("/placement/placements-list")
    PlacementApiResponse getPlacementsByEmail(
            @RequestParam("email") String email
    );
}