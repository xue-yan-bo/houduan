package com.jlm.homework.repository;

import com.jlm.homework.entity.MicroPurchase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface MicroPurchaseRepository extends JpaRepository<MicroPurchase,Long> , JpaSpecificationExecutor<MicroPurchase> {
}
