package com.zhaocai.business.expert.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhaocai.business.bidding.vo.req.EvalVendorCountVO;
import com.zhaocai.business.expert.domain.ExpertScore;

/**
 * 专家评分Service接口
 *
 * @author WH
 * @date 2024-05-24
 */
public interface IExpertScoreService  extends IService<ExpertScore> {

    /**
     * 获取未评标的供应商条数
     *
     * @param queryDTO
     * @return
     */
    Integer getNotEvalVendorCount(EvalVendorCountVO queryDTO);
}
