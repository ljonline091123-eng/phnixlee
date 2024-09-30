package com.zhaocai.business.bidding.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhaocai.business.bidding.domain.BiddingEvaluatExpert;
import com.zhaocai.business.bidding.vo.req.BiddingEvaluatExpertVO;
import com.zhaocai.business.bidding.vo.req.query.BiddingEvaluatExpertQueryVO;
import com.zhaocai.business.bidding.vo.req.query.EvalTaskPageVO;
import com.zhaocai.business.bidding.vo.res.BiddingEvaluatExpertListVO;
import com.zhaocai.business.bidding.vo.res.EvalTaskPageListVO;
import com.zhaocai.common.core.bean.PageResult;

import java.util.List;

/**
 * 评标专家人员信息Service接口
 *
 * @author WH
 * @date 2024-05-24
 */
public interface IBiddingEvaluatExpertService  extends IService<BiddingEvaluatExpert> {

    /**
     * 新增评标专家人员信息
     *
     * @param biddingEvaluatExpertVO 评标专家人员信息
     * @return 结果
     */
    boolean add(BiddingEvaluatExpertVO biddingEvaluatExpertVO);

    /**
     * 查询招标公告答疑列表
     *
     * @param queryVO 招标公告答疑
     * @return
     */
    List<BiddingEvaluatExpertListVO> getList(BiddingEvaluatExpertQueryVO queryVO);

    PageResult<EvalTaskPageListVO> getEvalSchemaPage(EvalTaskPageVO pageVO);

}
