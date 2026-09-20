package com.stockguard.inventory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface WarehouseRepository extends JpaRepository<Warehouse,UUID> { Optional<Warehouse> findByCode(String code); }
