package com.progressoft.clusterdata.repository;


import com.progressoft.clusterdata.entity.Deal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

@Repository
public interface DealRepository extends JpaRepository<Deal, Long> {

    @Query("SELECT d.dealUniqueId FROM Deal d WHERE d.dealUniqueId IN :ids")
    Set<String> findExistingDealIds(@Param("ids") List<String> ids);
}
