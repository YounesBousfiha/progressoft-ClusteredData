package com.progressoft.clusterdata.service;


import com.progressoft.clusterdata.dto.DealImportResponse;
import com.progressoft.clusterdata.dto.DealRequest;
import com.progressoft.clusterdata.entity.Deal;
import com.progressoft.clusterdata.repository.DealRepository;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@Slf4j
public class DealService {

    private final DealRepository dealRepository;

    private final ExecutorService executor;
    private static final int BATCH_SIZE = 1000;

    public DealService(DealRepository dealRepository) {
        this.dealRepository = dealRepository;
        this.executor = Executors.newFixedThreadPool(10);
    }

    public DealImportResponse processDeals(List<DealRequest> requests) {
        if(null == requests || requests.isEmpty()) return DealImportResponse.builder()
                .totalReceived(0)
                .successfulImports(0)
                .failedOrSkipped(0)
                .build();

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failedCount = new AtomicInteger(0);

        List<CompletableFuture<Void>> futures = new ArrayList<>();

        for(int i = 0; i < requests.size(); i += BATCH_SIZE) {
            int end = Math.min(requests.size(), i + BATCH_SIZE);

            List<DealRequest> chunk = requests.subList(i, end);

            CompletableFuture<Void> future = CompletableFuture.runAsync(
                    () -> processChunk(chunk, successCount, failedCount),
                    executor
            );
            futures.add(future);
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        log.info("Finished processing all {} deals", requests.size());
        return DealImportResponse.builder()
                .totalReceived(requests.size())
                .successfulImports(successCount.get())
                .failedOrSkipped(failedCount.get())
                .build();
    }

    private void processChunk(List<DealRequest> chunk, AtomicInteger successCount, AtomicInteger failedCount) {
        List<String> incomingIds = chunk.stream()
                .map(DealRequest::dealUniqueId)
                .toList();

        Set<String> existingIds = dealRepository.findExistingDealIds(incomingIds);

        chunk.stream()
                .filter(req -> {
                    if(existingIds.contains(req.dealUniqueId())) {
                        log.warn("Duplicate deal ignored: {}", req.dealUniqueId());
                        failedCount.incrementAndGet();
                        return false;
                    }
                    return true;
                })
                .forEach(req -> {
                    try {
                        Deal deal = Deal.builder()
                                .dealUniqueId(req.dealUniqueId())
                                .fromCurrency(req.fromCurrency())
                                .toCurrency(req.toCurrency())
                                .dealTimestamp(req.dealTimestamp())
                                .dealAmount(req.dealAmount())
                                .build();
                        dealRepository.save(deal);
                        successCount.incrementAndGet();
                    } catch (Exception e) {
                        log.error("Failed to save deal: {}", req.dealUniqueId());
                        failedCount.incrementAndGet();
                    }
                });
    }

    @PreDestroy
    public void shutdown() {
        executor.shutdown();
    }
}
