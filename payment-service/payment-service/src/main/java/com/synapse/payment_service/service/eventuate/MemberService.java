package com.synapse.payment_service.service.eventuate;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.synapse.payment_service.domain.entity.Member;
import com.synapse.payment_service.domain.repository.MemberRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class MemberService {
    private final MemberRepository memberRepository;

    @Transactional(propagation = Propagation.MANDATORY) // 상위 트랜잭션이 있을 때만 동작이 되어야 한다.
    public Member createdMember(UUID memberId, String email, String userName) {
        if (memberRepository.existsById(memberId)) {
            return memberRepository.findById(memberId).get();
        }
        
        Member member = Member.builder()
                .memberId(memberId)
                .email(email)
                .userName(userName)
                .build();
        memberRepository.save(member);
        return member;
    }
}
