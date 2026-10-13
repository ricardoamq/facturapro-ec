package com.facturapro.customers;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Customer, Long>, JpaSpecificationExecutor<Customer> {

    Optional<Customer> findByIdentification(String identification);

    boolean existsByIdentification(String identification);

    boolean existsByIdentificationAndIdNot(String identification, Long id);
}
