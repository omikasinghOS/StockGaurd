package com.stockguard.inventory;
import jakarta.persistence.*;
import java.util.UUID;
@Entity @Table(name="warehouses")
public class Warehouse { @Id public UUID id; public String code; public String name; public String city; protected Warehouse() {} }
