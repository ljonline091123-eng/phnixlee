package com.zhaocai.business.bidding.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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
import com.zhaocai.common.core.web.bean.ResultData;
import com.zhaocai.common.security.annotation.RequiresPermissions;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.Min;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 规范化招采领域查询接口，供新前端和旧接口适配层逐步切换使用。 */
@RestController
@RequestMapping("/bidding-domain")
@Api(value = "规范化招采领域", tags = "规范化招采领域")
@Validated
public class BiddingDomainController {

    private final BidSubmissionMapper submissionMapper;
    private final BidSubmissionVersionMapper versionMapper;
    private final EvaluationAssignmentMapper assignmentMapper;
    private final EvaluationSheetMapper sheetMapper;
    private final AwardDecisionMapper decisionMapper;
    private final AwardCandidateMapper candidateMapper;
    private final AwardPublicityMapper publicityMapper;

    public BiddingDomainController(BidSubmissionMapper submissionMapper,
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

    @GetMapping("/submissions")
    @ApiOperation("按公告查询规范化投标提交")
    @RequiresPermissions("bidding:domain:query")
    public ResultData<List<BidSubmission>> submissions(@RequestParam @Min(1) Long noticeId,
                                                       @RequestParam(required = false) @Min(1) Long vendorId) {
        LambdaQueryWrapper<BidSubmission> query = new LambdaQueryWrapper<BidSubmission>()
                .eq(BidSubmission::getNoticeId, noticeId)
                .orderByDesc(BidSubmission::getSubmittedAt);
        if (vendorId != null) {
            query.eq(BidSubmission::getVendorId, vendorId);
        }
        return ResultData.data(submissionMapper.selectList(query));
    }

    @GetMapping("/submissions/{submissionId}/versions")
    @ApiOperation("查询投标版本链")
    @RequiresPermissions("bidding:domain:query")
    public ResultData<List<BidSubmissionVersion>> versions(@PathVariable @Min(1) Long submissionId) {
        return ResultData.data(versionMapper.selectList(new LambdaQueryWrapper<BidSubmissionVersion>()
                .eq(BidSubmissionVersion::getSubmissionId, submissionId)
                .orderByAsc(BidSubmissionVersion::getVersionNo)));
    }

    @GetMapping("/evaluations")
    @ApiOperation("按公告查询评标任务及评标表")
    @RequiresPermissions("bidding:domain:query")
    public ResultData<Map<String, Object>> evaluations(@RequestParam @Min(1) Long noticeId) {
        List<EvaluationAssignment> assignments = assignmentMapper.selectList(new LambdaQueryWrapper<EvaluationAssignment>()
                .eq(EvaluationAssignment::getNoticeId, noticeId)
                .orderByAsc(EvaluationAssignment::getId));
        List<Long> assignmentIds = assignments.stream().map(EvaluationAssignment::getId).collect(java.util.stream.Collectors.toList());
        List<EvaluationSheet> sheets = assignmentIds.isEmpty() ? java.util.Collections.emptyList() : sheetMapper.selectList(
                new LambdaQueryWrapper<EvaluationSheet>().in(EvaluationSheet::getAssignmentId, assignmentIds));
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("assignments", assignments);
        result.put("sheets", sheets);
        return ResultData.data(result);
    }

    @GetMapping("/awards/{noticeId}")
    @ApiOperation("查询定标决策、中标候选人和公示")
    @RequiresPermissions("bidding:domain:query")
    public ResultData<Map<String, Object>> award(@PathVariable @Min(1) Long noticeId) {
        AwardDecision decision = decisionMapper.selectOne(new LambdaQueryWrapper<AwardDecision>()
                .eq(AwardDecision::getNoticeId, noticeId).last("limit 1"));
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("decision", decision);
        if (decision == null) {
            result.put("candidates", java.util.Collections.emptyList());
            result.put("publicity", null);
            return ResultData.data(result);
        }
        result.put("candidates", candidateMapper.selectList(new LambdaQueryWrapper<AwardCandidate>()
                .eq(AwardCandidate::getDecisionId, decision.getId())
                .orderByAsc(AwardCandidate::getRank)));
        result.put("publicity", publicityMapper.selectOne(new LambdaQueryWrapper<AwardPublicity>()
                .eq(AwardPublicity::getDecisionId, decision.getId()).last("limit 1")));
        return ResultData.data(result);
    }
}
