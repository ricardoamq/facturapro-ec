package com.facturapro.customers;

import com.facturapro.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "customers")
public class Customer extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "id_type", nullable = false, length = 20)
    private IdType idType = IdType.CEDULA;

    @Column(nullable = false, unique = true, length = 20)
    private String identification;

    @Column(name = "business_name", nullable = false, length = 160)
    private String businessName;

    @Column(length = 255)
    private String address;

    @Column(length = 30)
    private String phone;

    @Column(length = 120)
    private String email;

    @Column(length = 500)
    private String notes;

    @Column(nullable = false)
    private boolean active = true;
}
