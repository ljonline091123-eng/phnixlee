package com.zhaocai.business.bidding.service.workflow.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhaocai.business.bidding.domain.model.AwardCandidate;
import com.zhaocai.business.bidding.domain.model.AwardDecision;
import com.zhaocai.business.bidding.domain.model.AwardPublicity;
import com.zhaocai.business.bidding.domain.model.BidSubmission;
import com.zhaocai.business.bidding.domain.model.BidSubmissionVersion;
import com.zhaocai.business.bidding.domain.model.EvaluationAssignment;
import com.zhaocai.business.bidding.domain.model.EvaluationSheet;
import com.zhaocai.business.bidding.mapper.model.AwardCandidateMapper;
import com.zhaocai.business.bidding.mapper.model.AwardDecisionMapper;
import com.zhaocai.business.bidding.mapper.model.AwardPublicityMapper;
import com.zhaocai.business.bidding.mapper.model.BidSubmissionMapper;
import com.zhaocai.business.bidding.mapper.model.BidSubmissionVersionMapper;
import com.zhaocai.business.bidding.mapper.model.EvaluationAssignmentMapper;
import com.zhaocai.business.bidding.mapper.model.EvaluationSheetMapper;
import com.zhaocai.business.bidding.service.workflow.IBiddingAwardWorkflowService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.Date;
import java.util.List;

/** 招投标主链事务编排实现。 */
@Service
public class BiddingAwardWorkflowServiceImpl implements IBiddingAwardWorkflowService {

    private final BidSubmissionMapper submissionMapper;
    private final BidSubmissionVersionMapper versionMapper;
    private final EvaluationAssignmentMapper assignmentMapper;
    private final EvaluationSheetMapper sheetMapper;
    private final AwardDecisionMapper decisionMapper;
    private final AwardCandidateMapper candidateMapper;
    private final AwardPublicityMapper publicityMapper;

    public BiddingAwardWorkflowServiceImpl(BidSubmissionMapper submissionMapper,
                                           BidSubmissionVersionMapper versionMapper,
                                           EvaluationAssignmentMapper assignmentMapper,
                                           EvaluationSheetMapper sheetMapper,
                                           AwardDecisionMapper decisionMapper,
                                           AwardCandidateMapper candidateMapper,
                                           AwardPublicityMapper publicityMapper) {
        this.submissionMapper = submissionMapper;
        this.versionMapper = versionMapper;
        this.assignmentMapper = assignmentMapper;
        this.sheetMapper = sheetMapper;
        this.decisionMapper = decisionMapper;
        this.candidateMapper = candidateMapper;
        this.publicityMapper = publicityMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BidSubmission submitBid(BidSubmission submission, BidSubmissionVersion version, Date now) {
        if (submission == null || version == null) {
            throw new IllegalArgumentException("投标提交和投标版本不能为空");
        }
        if (submission.getStatus() == null) {
            submission.setStatus(BidSubmission.DRAFT);
        }
        if (submission.getId() == null && submissionMapper.insert(submission) != 1) {
            throw new IllegalStateException("投标提交保存失败");
        }
        if (version.getSubmissionId() == null && submission.getId() != null) {
            version.setSubmissionId(submission.getId());
        }
        version.setStatus(1);
        version.setSubmittedAt(now);
        if (versionMapper.insert(version) != 1) {
            throw new IllegalStateException("投标版本保存失败");
        }
        submission.submit(version.getId(), now);
        if (submissionMapper.updateById(submission) != 1) {
            throw new IllegalStateException("投标提交状态更新失败");
        }
        return submission;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public EvaluationAssignment completeEvaluation(EvaluationAssignment assignment,
                                                    List<EvaluationSheet> sheets,
                                                    Date now) {
        if (assignment == null || CollectionUtils.isEmpty(sheets)) {
            throw new IllegalArgumentException("评标任务和评标表不能为空");
        }
        if (assignment.getStatus() == null) {
            assignment.setStatus(EvaluationAssignment.PENDING);
        }
        assignment.start();
        for (EvaluationSheet sheet : sheets) {
            if (sheet.getAssignmentId() == null) {
                sheet.setAssignmentId(assignment.getId());
            }
            sheet.submit(now);
            if (sheetMapper.insert(sheet) != 1) {
                throw new IllegalStateException("评标表保存失败");
            }
        }
        assignment.complete(now);
        if (assignmentMapper.updateById(assignment) != 1) {
            throw new IllegalStateException("评标任务完成状态更新失败");
        }
        return assignment;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AwardDecision publishAward(AwardDecision decision,
                                      List<AwardCandidate> candidates,
                                      AwardPublicity publicity,
                                      Long operatorId,
                                      Date now) {
        if (decision == null || CollectionUtils.isEmpty(candidates) || publicity == null) {
            throw new IllegalArgumentException("定标决策、候选人和公示不能为空");
        }
        if (decision.getStatus() == null) {
            decision.setStatus(AwardDecision.DRAFT);
        }
        if (decision.getId() == null && decisionMapper.insert(decision) != 1) {
            throw new IllegalStateException("定标决策保存失败");
        }
        for (AwardCandidate candidate : candidates) {
            if (candidate.getDecisionId() == null) {
                candidate.setDecisionId(decision.getId());
            }
            if (candidateMapper.insert(candidate) != 1) {
                throw new IllegalStateException("中标候选人保存失败");
            }
        }
        AwardCandidate selected = candidates.stream()
                .filter(candidate -> Integer.valueOf(1).equals(candidate.getRank()))
                .findFirst()
                .orElseGet(() -> candidates.stream()
                        .filter(candidate -> candidate.getRank() != null)
                        .min(java.util.Comparator.comparing(AwardCandidate::getRank))
                        .orElse(candidates.get(0)));
        decision.decide(selected.getId(), operatorId, decision.getDecisionBasis(), now);
        if (decisionMapper.updateById(decision) != 1) {
            throw new IllegalStateException("定标决策保存失败");
        }
        publicity.setDecisionId(decision.getId());
        publicity.publish(now);
        if (publicityMapper.insert(publicity) != 1) {
            throw new IllegalStateException("中标公示保存失败");
        }
        decision.markPublished();
        decisionMapper.updateById(decision);
        return decision;
    }
}
