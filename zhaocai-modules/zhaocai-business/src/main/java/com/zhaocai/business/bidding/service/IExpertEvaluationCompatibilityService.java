package com.zhaocai.business.bidding.service;

import com.zhaocai.business.expert.domain.ExpertScore;
import com.zhaocai.business.bidding.domain.BiddingEvaluatExpert;

/** 旧专家评分向规范化评标任务、评标表的兼容同步。 */
public interface IExpertEvaluationCompatibilityService {
    void syncAssignment(BiddingEvaluatExpert expert);

    void syncScore(ExpertScore score);
}
