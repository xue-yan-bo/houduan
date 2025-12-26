package com.jlm.homework.repository;

import com.jlm.homework.entity.MicroPurchase;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MicroPurchaseRepository extends JpaRepository<MicroPurchase,Long> {
}
