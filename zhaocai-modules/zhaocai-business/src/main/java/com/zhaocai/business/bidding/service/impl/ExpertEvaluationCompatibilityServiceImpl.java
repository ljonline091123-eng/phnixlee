package com.zhaocai.business.bidding.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhaocai.business.bidding.domain.model.BidSubmission;
import com.zhaocai.business.bidding.domain.BiddingEvaluatExpert;
import com.zhaocai.business.bidding.domain.model.EvaluationAssignment;
import com.zhaocai.business.bidding.domain.model.EvaluationSheet;
import com.zhaocai.business.bidding.domain.model.BidSubmissionVersion;
import com.zhaocai.business.bidding.mapper.model.BidSubmissionMapper;
import com.zhaocai.business.bidding.mapper.model.BidSubmissionVersionMapper;
import com.zhaocai.business.bidding.mapper.model.EvaluationAssignmentMapper;
import com.zhaocai.business.bidding.mapper.model.EvaluationSheetMapper;
import com.zhaocai.business.bidding.service.IExpertEvaluationCompatibilityService;
import com.zhaocai.business.expert.domain.ExpertScore;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

@Service
public class ExpertEvaluationCompatibilityServiceImpl implements IExpertEvaluationCompatibilityService {
    private final BidSubmissionMapper submissionMapper;
    private final BidSubmissionVersionMapper versionMapper;
    private final EvaluationAssignmentMapper assignmentMapper;
    private final EvaluationSheetMapper sheetMapper;

    public ExpertEvaluationCompatibilityServiceImpl(BidSubmissionMapper submissionMapper,
                                                    BidSubmissionVersionMapper versionMapper,
                                                    EvaluationAssignmentMapper assignmentMapper,
                                                    EvaluationSheetMapper sheetMapper) {
        this.submissionMapper = submissionMapper;
        this.versionMapper = versionMapper;
        this.assignmentMapper = assignmentMapper;
        this.sheetMapper = sheetMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void syncAssignment(BiddingEvaluatExpert expert) {
        if (expert == null || expert.getNoticeId() == null || expert.getExpertId() == null) {
            return;
        }
        int roleType = expert.getExpertType() == null ? 0 : expert.getExpertType();
        EvaluationAssignment assignment = assignmentMapper.selectOne(new LambdaQueryWrapper<EvaluationAssignment>()
                .eq(EvaluationAssignment::getNoticeId, expert.getNoticeId())
                .eq(EvaluationAssignment::getExpertId, expert.getExpertId())
                .eq(EvaluationAssignment::getRoleType, roleType).last("limit 1"));
        if (assignment == null) {
            assignment = new EvaluationAssignment();
            assignment.setNoticeId(expert.getNoticeId());
            assignment.setExpertId(expert.getExpertId());
            assignment.setRoleType(roleType);
            assignmentMapper.insert(assignment);
        }
        assignment.setDeadline(expert.getDeadline());
        if (Integer.valueOf(2).equals(expert.getIsJoin())) {
            assignment.setStatus(EvaluationAssignment.CANCELLED);
        } else if (Integer.valueOf(1).equals(expert.getEvalStatus())) {
            assignment.setStatus(EvaluationAssignment.COMPLETED);
            assignment.setCompletedAt(expert.getUpdateTime());
        } else {
            assignment.setStatus(EvaluationAssignment.PENDING);
        }
        assignmentMapper.updateById(assignment);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void syncScore(ExpertScore score) {
        if (score == null || score.getNoticeId() == null || score.getExpertId() == null || score.getVendorId() == null) {
            return;
        }
        BidSubmission submission = submissionMapper.selectOne(new LambdaQueryWrapper<BidSubmission>()
                .eq(BidSubmission::getNoticeId, score.getNoticeId())
                .eq(BidSubmission::getVendorId, score.getVendorId()).last("limit 1"));
        if (submission == null || submission.getCurrentVersionId() == null) {
            return;
        }
        BidSubmissionVersion version = versionMapper.selectById(submission.getCurrentVersionId());
        if (version == null) {
            return;
        }
        EvaluationAssignment assignment = assignmentMapper.selectOne(new LambdaQueryWrapper<EvaluationAssignment>()
                .eq(EvaluationAssignment::getNoticeId, score.getNoticeId())
                .eq(EvaluationAssignment::getExpertId, score.getExpertId())
                .last("limit 1"));
        if (assignment == null) {
            assignment = new EvaluationAssignment();
            assignment.setNoticeId(score.getNoticeId());
            assignment.setExpertId(score.getExpertId());
            assignment.setRoleType(0);
            assignment.setStatus(EvaluationAssignment.PENDING);
            assignmentMapper.insert(assignment);
        }
        EvaluationSheet sheet = sheetMapper.selectOne(new LambdaQueryWrapper<EvaluationSheet>()
                .eq(EvaluationSheet::getAssignmentId, assignment.getId())
                .eq(EvaluationSheet::getSubmissionVersionId, version.getId()).last("limit 1"));
        if (sheet == null) {
            sheet = new EvaluationSheet();
            sheet.setAssignmentId(assignment.getId());
            sheet.setSubmissionVersionId(version.getId());
            sheetMapper.insert(sheet);
        }
        sheet.setBusinessScore(score.getBusScore());
        sheet.setTechnicalScore(score.getTechScore());
        sheet.setQuotationScore(score.getQuotation());
        sheet.setOpinion(score.getEvaOpinion());
        Date evaTime = score.getEvaTime() == null ? new Date() : score.getEvaTime();
        if (Integer.valueOf(1).equals(score.getEvalStatus())) {
            sheet.submit(evaTime);
            if (!Integer.valueOf(EvaluationAssignment.COMPLETED).equals(assignment.getStatus())) {
                assignment.setStatus(EvaluationAssignment.COMPLETED);
                assignment.setCompletedAt(evaTime);
                assignmentMapper.updateById(assignment);
            }
        } else {
            sheet.calculateTotalScore();
            sheet.setStatus(0);
        }
        sheetMapper.updateById(sheet);
    }
}
