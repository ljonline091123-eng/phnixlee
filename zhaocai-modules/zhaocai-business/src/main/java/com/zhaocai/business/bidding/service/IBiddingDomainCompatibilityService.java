package com.zhaocai.business.bidding.service;

import com.zhaocai.business.bidding.domain.BiddingInfo;

/** 旧投标单与规范化投标聚合之间的兼容同步服务。 */
public interface IBiddingDomainCompatibilityService {

    /** 将一条旧投标单幂等同步为投标提交和投标版本。 */
    void syncSubmission(BiddingInfo biddingInfo);
}
