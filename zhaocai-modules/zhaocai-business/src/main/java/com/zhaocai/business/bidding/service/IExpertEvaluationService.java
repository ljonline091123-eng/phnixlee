package com.zhaocai.business.bidding.service;

import com.zhaocai.business.bidding.vo.req.EvalVO;
import com.zhaocai.business.bidding.vo.req.query.EvalTaskPageVO;
import com.zhaocai.business.bidding.vo.res.*;
import com.zhaocai.common.core.bean.PageResult;

import java.util.List;

/**
 * @author ssy
 * @date 2024/5/14 11:34
 */
public interface IExpertEvaluationService {

	/**
	 * 待评标任务列表-分页
	 *
	 * @param pageVO
	 * @return
	 */
	PageResult<EvalTaskPageListVO> todoEvalTaskPage(EvalTaskPageVO pageVO);

	/**
	 * 已完成评标任务列表-分页
	 */
	PageResult<EvalTaskPageListVO> doneEvalTaskPage(EvalTaskPageVO pageVO);

	/**
	 * 根据方案id查询评分模板信息
	 */
	BiddingMarkTemplateDetailVO getMarkTempInfo(Long schemeId);

	/**
	 * 获取专家评分数据
	 */
	ExpertEvalDataVO getExpertEvalData(Long noticeId, Long biddingInfoId);

	/**
	 * 获取专家评分记录
	 */
	List<ExpertEvalRecordVO> getExpertEvalRecord(Long noticeId, Long vendorId);

	/**
	 * 专家评分
	 *
	 * @param evalVO
	 * @return
	 */
	boolean eval(EvalVO evalVO);

	List<BiddingMarkCategoryDetailVO> getEvaluateVisibleDetail(Long schemeId, Long noticeId, Long vendorId, Long expertId);
}
