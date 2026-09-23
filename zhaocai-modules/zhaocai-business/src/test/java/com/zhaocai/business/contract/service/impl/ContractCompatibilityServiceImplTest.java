package com.zhaocai.business.contract.service.impl;

import com.zhaocai.business.agreement.domain.Agreement;
import com.zhaocai.business.common.enums.AgreementStateEnum;
import com.zhaocai.business.bidding.mapper.model.AwardCandidateMapper;
import com.zhaocai.business.bidding.mapper.model.AwardDecisionMapper;
import com.zhaocai.business.bidding.domain.model.AwardCandidate;
import com.zhaocai.business.bidding.domain.model.AwardDecision;
import com.zhaocai.business.contract.domain.model.ProcurementContract;
import com.zhaocai.business.contract.mapper.ContractPartyMapper;
import com.zhaocai.business.contract.mapper.ContractSignTaskMapper;
import com.zhaocai.business.contract.mapper.ProcurementContractMapper;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ContractCompatibilityServiceImplTest {

    @Test
    public void newContractShouldBePopulatedBeforeInsert() {
        ProcurementContractMapper contractMapper = mock(ProcurementContractMapper.class);
        ContractPartyMapper partyMapper = mock(ContractPartyMapper.class);
        ContractSignTaskMapper signTaskMapper = mock(ContractSignTaskMapper.class);
        AwardDecisionMapper awardDecisionMapper = mock(AwardDecisionMapper.class);
        AwardCandidateMapper awardCandidateMapper = mock(AwardCandidateMapper.class);
        when(contractMapper.selectOne(any())).thenReturn(null);

        Agreement agreement = new Agreement();
        agreement.setId(101L);
        agreement.setAgreementCode("HT-001");
        agreement.setAgreementState(AgreementStateEnum.DRAFT.getState());

        new ContractCompatibilityServiceImpl(contractMapper, partyMapper, signTaskMapper,
                awardDecisionMapper, awardCandidateMapper)
                .syncContract(agreement);

        ArgumentCaptor<ProcurementContract> captor = ArgumentCaptor.forClass(ProcurementContract.class);
        verify(contractMapper).insert(captor.capture());
        verify(contractMapper, never()).updateById(any());
        assertEquals("HT-001", captor.getValue().getContractNo());
        assertEquals("历史合同-101", captor.getValue().getContractName());
        assertEquals(Integer.valueOf(ProcurementContract.DRAFT), captor.getValue().getStatus());
    }

    @Test
    public void contractShouldReferenceAwardSelectedForVendor() {
        ProcurementContractMapper contractMapper = mock(ProcurementContractMapper.class);
        ContractPartyMapper partyMapper = mock(ContractPartyMapper.class);
        ContractSignTaskMapper signTaskMapper = mock(ContractSignTaskMapper.class);
        AwardDecisionMapper awardDecisionMapper = mock(AwardDecisionMapper.class);
        AwardCandidateMapper awardCandidateMapper = mock(AwardCandidateMapper.class);
        when(contractMapper.selectOne(any())).thenReturn(null);
        AwardDecision decision = new AwardDecision();
        decision.setId(301L);
        decision.setSelectedCandidateId(401L);
        AwardCandidate candidate = new AwardCandidate();
        candidate.setId(401L);
        candidate.setVendorId(201L);
        when(awardDecisionMapper.selectList(any())).thenReturn(Collections.singletonList(decision));
        when(awardCandidateMapper.selectById(401L)).thenReturn(candidate);

        Agreement agreement = new Agreement();
        agreement.setId(101L);
        agreement.setSchemeId(102L);
        agreement.setVendorId(201L);
        agreement.setAgreementName("采购合同");
        agreement.setAgreementState(AgreementStateEnum.DRAFT.getState());

        new ContractCompatibilityServiceImpl(contractMapper, partyMapper, signTaskMapper,
                awardDecisionMapper, awardCandidateMapper).syncContract(agreement);

        ArgumentCaptor<ProcurementContract> captor = ArgumentCaptor.forClass(ProcurementContract.class);
        verify(contractMapper).insert(captor.capture());
        assertEquals(Long.valueOf(301L), captor.getValue().getAwardDecisionId());
    }
}
