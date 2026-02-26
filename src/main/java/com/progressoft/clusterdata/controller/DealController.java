package com.progressoft.clusterdata.controller;

import com.progressoft.clusterdata.dto.DealImportResponse;
import com.progressoft.clusterdata.dto.DealRequest;
import com.progressoft.clusterdata.service.DealService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/deals")
@RequiredArgsConstructor
@Slf4j
@Validated
public class DealController {

    private final DealService dealService;

    @PostMapping
    public ResponseEntity<DealImportResponse> importDeals(@RequestBody List<@Valid DealRequest> deals) {
        log.info("Received request to import {} deals", deals.size());

        DealImportResponse response = dealService.processDeals(deals);

        return ResponseEntity.ok(response);
    }
}
