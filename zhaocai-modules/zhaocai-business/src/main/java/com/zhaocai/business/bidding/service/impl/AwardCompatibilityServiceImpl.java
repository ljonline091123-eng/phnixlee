package com.zhaocai.business.bidding.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhaocai.business.bidding.domain.BiddingResult;
import com.zhaocai.business.bidding.domain.model.AwardCandidate;
import com.zhaocai.business.bidding.domain.model.AwardDecision;
import com.zhaocai.business.bidding.domain.model.AwardPublicity;
import com.zhaocai.business.bidding.domain.model.BidSubmission;
import com.zhaocai.business.bidding.mapper.model.AwardCandidateMapper;
import com.zhaocai.business.bidding.mapper.model.AwardDecisionMapper;
import com.zhaocai.business.bidding.mapper.model.AwardPublicityMapper;
import com.zhaocai.business.bidding.mapper.model.BidSubmissionMapper;
import com.zhaocai.business.bidding.service.IAwardCompatibilityService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

@Service
public class AwardCompatibilityServiceImpl implements IAwardCompatibilityService {
    private final AwardDecisionMapper decisionMapper;
    private final AwardCandidateMapper candidateMapper;
    private final AwardPublicityMapper publicityMapper;
    private final BidSubmissionMapper submissionMapper;

    public AwardCompatibilityServiceImpl(AwardDecisionMapper decisionMapper,
                                         AwardCandidateMapper candidateMapper,
                                         AwardPublicityMapper publicityMapper,
                                         BidSubmissionMapper submissionMapper) {
        this.decisionMapper = decisionMapper;
        this.candidateMapper = candidateMapper;
        this.publicityMapper = publicityMapper;
        this.submissionMapper = submissionMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void syncResult(BiddingResult result) {
        if (result == null || result.getNoticeId() == null || result.getVendorId() == null) {
            return;
        }
        BidSubmission submission = submissionMapper.selectOne(new LambdaQueryWrapper<BidSubmission>()
                .eq(BidSubmission::getNoticeId, result.getNoticeId())
                .eq(BidSubmission::getVendorId, result.getVendorId()).last("limit 1"));
        if (submission == null || submission.getCurrentVersionId() == null) {
            return;
        }
        AwardDecision decision = decisionMapper.selectOne(new LambdaQueryWrapper<AwardDecision>()
                .eq(AwardDecision::getNoticeId, result.getNoticeId()).last("limit 1"));
        if (decision == null) {
            decision = new AwardDecision();
            decision.setNoticeId(result.getNoticeId());
            decision.setSchemeId(result.getSchemeId());
            decision.setStatus(AwardDecision.DRAFT);
            decision.setDecisionBasis("由旧 BiddingResult 兼容同步，待复核");
            decisionMapper.insert(decision);
        }
        AwardCandidate candidate = candidateMapper.selectOne(new LambdaQueryWrapper<AwardCandidate>()
                .eq(AwardCandidate::getDecisionId, decision.getId())
                .eq(AwardCandidate::getVendorId, result.getVendorId()).last("limit 1"));
        if (candidate == null) {
            candidate = new AwardCandidate();
            candidate.setDecisionId(decision.getId());
            candidate.setVendorId(result.getVendorId());
            candidate.setSubmissionVersionId(submission.getCurrentVersionId());
            candidate.setRank(result.getRank() == null
                    ? candidateMapper.selectCount(new LambdaQueryWrapper<AwardCandidate>()
                    .eq(AwardCandidate::getDecisionId, decision.getId())).intValue() + 1
                    : result.getRank());
            candidateMapper.insert(candidate);
        }
        candidate.setTotalScore(result.getTotalScore());
        candidate.setRank(result.getRank());
        candidate.setRecommendationReason(Integer.valueOf(1).equals(result.getSureBid()) ? "历史确定中标记录" : "历史候选记录");
        candidateMapper.updateById(candidate);
        if (Integer.valueOf(1).equals(result.getSureBid())) {
            decision.setSelectedCandidateId(candidate.getId());
            decision.setStatus(AwardDecision.DECIDED);
            decision.setDecidedAt(result.getNotifiTime() == null ? new Date() : result.getNotifiTime());
            decisionMapper.updateById(decision);
        }
        AwardPublicity publicity = publicityMapper.selectOne(new LambdaQueryWrapper<AwardPublicity>()
                .eq(AwardPublicity::getDecisionId, decision.getId()).last("limit 1"));
        if (publicity == null) {
            publicity = new AwardPublicity();
            publicity.setDecisionId(decision.getId());
            publicity.setTitle("中标公示-" + result.getNoticeId());
            publicityMapper.insert(publicity);
        }
        publicity.setContent(result.getNotifiContent());
        publicity.setStartTime(result.getPublicityStartTime());
        publicity.setEndTime(result.getPublicityEndTime());
        publicity.setStatus(result.getPublicityEndTime() == null ? 0 : 1);
        publicity.setPublishedAt(result.getNotifiTime());
        publicityMapper.updateById(publicity);
        if (result.getPublicityEndTime() != null && decision.getSelectedCandidateId() != null) {
            decision.setStatus(AwardDecision.PUBLISHED);
            decisionMapper.updateById(decision);
        }
    }
}
