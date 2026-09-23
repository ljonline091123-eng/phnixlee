package com.zhaocai.business.contract.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhaocai.business.agreement.domain.Agreement;
import com.zhaocai.business.bidding.domain.model.AwardCandidate;
import com.zhaocai.business.bidding.domain.model.AwardDecision;
import com.zhaocai.business.bidding.mapper.model.AwardCandidateMapper;
import com.zhaocai.business.bidding.mapper.model.AwardDecisionMapper;
import com.zhaocai.business.common.enums.AgreementStateEnum;
import com.zhaocai.business.contract.domain.model.ContractParty;
import com.zhaocai.business.contract.domain.model.ContractSignTask;
import com.zhaocai.business.contract.domain.model.ProcurementContract;
import com.zhaocai.business.contract.mapper.ContractPartyMapper;
import com.zhaocai.business.contract.mapper.ContractSignTaskMapper;
import com.zhaocai.business.contract.mapper.ProcurementContractMapper;
import com.zhaocai.business.contract.service.IContractCompatibilityService;
import com.zhaocai.common.signature.common.enums.QiYueSuoPrivateCallBackTypeEnum;
import com.zhaocai.common.signature.dto.callback.qysp.SignCallBackData;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ContractCompatibilityServiceImpl implements IContractCompatibilityService {
    private final ProcurementContractMapper contractMapper;
    private final ContractPartyMapper partyMapper;
    private final ContractSignTaskMapper signTaskMapper;
    private final AwardDecisionMapper awardDecisionMapper;
    private final AwardCandidateMapper awardCandidateMapper;

    public ContractCompatibilityServiceImpl(ProcurementContractMapper contractMapper,
                                            ContractPartyMapper partyMapper,
                                            ContractSignTaskMapper signTaskMapper,
                                            AwardDecisionMapper awardDecisionMapper,
                                            AwardCandidateMapper awardCandidateMapper) {
        this.contractMapper = contractMapper;
        this.partyMapper = partyMapper;
        this.signTaskMapper = signTaskMapper;
        this.awardDecisionMapper = awardDecisionMapper;
        this.awardCandidateMapper = awardCandidateMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void syncContract(Agreement agreement) {
        if (agreement == null || agreement.getId() == null) {
            return;
        }
        ProcurementContract contract = contractMapper.selectOne(new LambdaQueryWrapper<ProcurementContract>()
                .eq(ProcurementContract::getLegacyAgreementId, agreement.getId()).last("limit 1"));
        boolean newContract = contract == null;
        if (newContract) {
            contract = new ProcurementContract();
            contract.setLegacyAgreementId(agreement.getId());
        }
        contract.setSchemeId(agreement.getSchemeId());
        contract.setVendorId(agreement.getVendorId());
        contract.setAwardDecisionId(findAwardDecisionId(agreement.getSchemeId(), agreement.getVendorId()));
        contract.setContractNo(agreement.getAgreementCode());
        contract.setContractName(contractName(agreement));
        contract.setTotalAmount(agreement.getTotalAmountIncTax());
        contract.setSignedAt(agreement.getAgreementSignDate());
        contract.setEffectiveAt(agreement.getAgreementEffectiveDate());
        contract.setStatus(mapStatus(agreement.getAgreementState()));
        if (newContract) {
            contractMapper.insert(contract);
        } else {
            contractMapper.updateById(contract);
        }

        syncParty(contract.getId(), 1, agreement.getPartyAOrgId(), agreement.getPartyAName());
        syncParty(contract.getId(), 2, String.valueOf(agreement.getVendorId()), agreement.getPartyBName());
        if (Integer.valueOf(ProcurementContract.PENDING_SIGN).equals(contract.getStatus())) {
            getOrCreateSignTask(contract.getId());
        }
    }

    private String contractName(Agreement agreement) {
        String name = agreement.getAgreementName();
        return name == null || name.trim().isEmpty() ? "历史合同-" + agreement.getId() : name;
    }

    private Long findAwardDecisionId(Long schemeId, Long vendorId) {
        if (schemeId == null || vendorId == null) {
            return null;
        }
        java.util.List<AwardDecision> decisions = awardDecisionMapper.selectList(
                new LambdaQueryWrapper<AwardDecision>()
                        .eq(AwardDecision::getSchemeId, schemeId)
                        .isNotNull(AwardDecision::getSelectedCandidateId));
        for (AwardDecision decision : decisions) {
            AwardCandidate selected = awardCandidateMapper.selectById(decision.getSelectedCandidateId());
            if (selected != null && vendorId.equals(selected.getVendorId())) {
                return decision.getId();
            }
        }
        return null;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void syncSignCallback(Long legacyAgreementId, SignCallBackData callback) {
        ProcurementContract contract = contractMapper.selectOne(new LambdaQueryWrapper<ProcurementContract>()
                .eq(ProcurementContract::getLegacyAgreementId, legacyAgreementId).last("limit 1"));
        if (contract == null) {
            return;
        }
        ContractSignTask task = getOrCreateSignTask(contract.getId());
        task.setExternalContractId(String.valueOf(callback.getContractId()));
        task.setLastCallbackEventId(callback.getCallbackType() + ":" + callback.getCallbackTime() + ":" + callback.getContractId());
        if (QiYueSuoPrivateCallBackTypeEnum.CONTRACT_COMPLETE.equalsType(callback.getCallbackType())) {
            task.setStatus(ContractSignTask.COMPLETED);
            contract.setStatus(ProcurementContract.SIGNED);
            contractMapper.updateById(contract);
        } else {
            task.setStatus(ContractSignTask.PROCESSING);
        }
        signTaskMapper.updateById(task);
    }

    private int mapStatus(Integer state) {
        if (AgreementStateEnum.SIGN_SUCCESS.equalsState(state)) {
            return ProcurementContract.SIGNED;
        }
        if (AgreementStateEnum.CANCELLATION.equalsState(state) || AgreementStateEnum.CANCEL_SIGN.equalsState(state)) {
            return ProcurementContract.CANCELLED;
        }
        if (AgreementStateEnum.PUSH_SIGNATURE_PLATFORM.equalsState(state)
                || AgreementStateEnum.PARTY_A_TO_SIGN.equalsState(state)
                || AgreementStateEnum.PARTY_B_TO_SIGN.equalsState(state)) {
            return ProcurementContract.PENDING_SIGN;
        }
        return ProcurementContract.DRAFT;
    }

    private void syncParty(Long contractId, int partyType, String partyId, String partyName) {
        if (partyId == null || "null".equals(partyId)) {
            return;
        }
        ContractParty party = partyMapper.selectOne(new LambdaQueryWrapper<ContractParty>()
                .eq(ContractParty::getContractId, contractId)
                .eq(ContractParty::getPartyType, partyType)
                .eq(ContractParty::getPartyId, partyId).last("limit 1"));
        if (party == null) {
            party = new ContractParty();
            party.setContractId(contractId);
            party.setPartyType(partyType);
            party.setPartyId(partyId);
            party.setNeedSign(1);
            partyMapper.insert(party);
        }
        party.setPartyName(partyName);
        partyMapper.updateById(party);
    }

    private ContractSignTask getOrCreateSignTask(Long contractId) {
        ContractSignTask task = signTaskMapper.selectOne(new LambdaQueryWrapper<ContractSignTask>()
                .eq(ContractSignTask::getContractId, contractId)
                .eq(ContractSignTask::getPartyId, 0L).last("limit 1"));
        if (task == null) {
            task = new ContractSignTask();
            task.setContractId(contractId);
            task.setPartyId(0L);
            task.setPlatform("qiyuesuo-private");
            task.setStatus(ContractSignTask.PENDING);
            signTaskMapper.insert(task);
        }
        return task;
    }
}
