package com.zhaocai.business.bidding.service.workflow;

import com.zhaocai.business.bidding.domain.model.AwardCandidate;
import com.zhaocai.business.bidding.domain.model.AwardDecision;
import com.zhaocai.business.bidding.domain.model.AwardPublicity;
import com.zhaocai.business.bidding.domain.model.BidSubmission;
import com.zhaocai.business.bidding.domain.model.BidSubmissionVersion;
import com.zhaocai.business.bidding.domain.model.EvaluationAssignment;
import com.zhaocai.business.bidding.domain.model.EvaluationSheet;

import java.util.Date;
import java.util.List;

/**
 * 招投标领域应用服务。
 * 负责跨聚合事务编排，实体内部只负责自身状态不变量。
 */
public interface IBiddingAwardWorkflowService {

    BidSubmission submitBid(BidSubmission submission, BidSubmissionVersion version, Date now);

    EvaluationAssignment completeEvaluation(EvaluationAssignment assignment,
                                             List<EvaluationSheet> sheets,
                                             Date now);

    AwardDecision publishAward(AwardDecision decision,
                               List<AwardCandidate> candidates,
                               AwardPublicity publicity,
                               Long operatorId,
                               Date now);
}
