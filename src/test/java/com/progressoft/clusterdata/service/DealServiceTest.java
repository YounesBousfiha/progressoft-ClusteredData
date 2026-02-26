package com.progressoft.clusterdata.service;


import com.progressoft.clusterdata.dto.DealImportResponse;
import com.progressoft.clusterdata.dto.DealRequest;
import com.progressoft.clusterdata.entity.Deal;
import com.progressoft.clusterdata.repository.DealRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
 class DealServiceTest {

    @Mock
    DealRepository dealRepository;

    @InjectMocks
    DealService dealService;

    @Test
    void shouldHandleNullOrEmptyRequest() {
        DealImportResponse nullResponse = dealService.processDeals(null);
        DealImportResponse emptyResponse = dealService.processDeals(Collections.emptyList());

        assertEquals(0, nullResponse.totalReceived());
        assertEquals(0, emptyResponse.totalReceived());
    }


    @Test
    void shouldProcessCorrectlyWithDuplicatesAndErrors() {
        List<DealRequest> requests = List.of(
                new DealRequest("ID-1", "USD", "MAD", LocalDateTime.now(), BigDecimal.TEN),
                new DealRequest("ID-2", "EUR", "MAD", LocalDateTime.now(), BigDecimal.TEN),
                new DealRequest("ID-3", "GBP", "MAD", LocalDateTime.now(), BigDecimal.TEN)
        );

        when(dealRepository.findExistingDealIds(anyList())).thenReturn(Set.of("ID-2"));

        when(dealRepository.save(any(Deal.class))).thenAnswer(invocation -> {
            Deal deal = invocation.getArgument(0);
            if ("ID-3".equals(deal.getDealUniqueId())) {
                throw new RuntimeException("Simulated DB Error");
            }
            return deal;
        });

        DealImportResponse response = dealService.processDeals(requests);

        assertEquals(3, response.totalReceived());
        assertEquals(1, response.successfulImports());
        assertEquals(2, response.failedOrSkipped());

        verify(dealRepository, times(2)).save(any(Deal.class));
    }

    @Test
    void shouldProcessInBatches() {
        List<DealRequest> requests = IntStream.range(0, 2500)
                .mapToObj(i -> new DealRequest("ID-" + i, "USD", "MAD", LocalDateTime.now(), BigDecimal.TEN))
                .toList();

        dealService.processDeals(requests);

        verify(dealRepository, times(3)).findExistingDealIds(anyList());

        verify(dealRepository, times(2500)).save(any(Deal.class));
    }
}
