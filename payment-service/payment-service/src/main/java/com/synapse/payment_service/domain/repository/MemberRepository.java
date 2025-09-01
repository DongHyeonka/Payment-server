package com.synapse.payment_service.domain.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.synapse.payment_service.domain.entity.Member;

public interface MemberRepository extends JpaRepository<Member, UUID>{
    
}
