package com.stockguard.order;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface OrderRepository extends JpaRepository<PurchaseOrder,UUID> { @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
 @org.springframework.data.jpa.repository.Query("select o from PurchaseOrder o where o.id=:id") Optional<PurchaseOrder> lockById(UUID id);
 Optional<PurchaseOrder> findByRequestId(UUID requestId); }
