package com.compudelivery.orders.identity;

import org.springframework.data.jpa.repository.JpaRepository;

public interface OperatorRepository extends JpaRepository<Operator, String> {
}
