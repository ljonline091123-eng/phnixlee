package com.zhaocai.business.bidding.controller;

import com.zhaocai.business.bidding.service.IExpertEvaluationService;
import com.zhaocai.business.bidding.vo.req.EvalVO;
import com.zhaocai.business.bidding.vo.req.query.EvalTaskPageVO;
import com.zhaocai.business.bidding.vo.res.BiddingMarkTemplateDetailVO;
import com.zhaocai.business.bidding.vo.res.EvalTaskPageListVO;
import com.zhaocai.business.bidding.vo.res.ExpertEvalDataVO;
import com.zhaocai.business.bidding.vo.res.ExpertEvalRecordVO;
import com.zhaocai.business.common.base.BladeController;
import com.zhaocai.common.core.bean.PageResult;
import com.zhaocai.common.core.web.bean.ResultData;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

/**
 * @author ssy
 * @date 2024/5/14 11:32
 */
@RestController
@AllArgsConstructor
@RequestMapping("expertEvaluation")
@Api(value = "专家评标", tags = "专家端评标接口")
public class ExpertEvaluationController extends BladeController {

	@Autowired
	private IExpertEvaluationService expertEvaluationService;

	/**
	 * 待评标任务列表-分页
	 */
	@PostMapping("/todoEvalTaskPage")
	@ApiOperation(value = "待评标任务列表-分页", notes = "传入pageDTO")
	public ResultData<PageResult<EvalTaskPageListVO>> todoEvalTaskPage(@RequestBody EvalTaskPageVO pageVO) {
		return ResultData.data(expertEvaluationService.todoEvalTaskPage(pageVO));
	}

	/**
	 * 已完成评标任务列表-分页
	 */
	@PostMapping("/doneEvalTaskPage")
	@ApiOperation(value = "已完成评标任务列表-分页", notes = "传入pageDTO")
	public ResultData<PageResult<EvalTaskPageListVO>> doneEvalTaskPage(@RequestBody EvalTaskPageVO pageVO) {
		return ResultData.data(expertEvaluationService.doneEvalTaskPage(pageVO));
	}

	/**
	 * 根据方案id查询评分模板信息
	 */
	@PostMapping("/getMarkTempInfo")
	@ApiOperation(value = "根据方案id查询评分模板信息", notes = "传入pageDTO")
	public ResultData<BiddingMarkTemplateDetailVO> getMarkTempInfo(@RequestParam Long schemeId) {
		return ResultData.data(expertEvaluationService.getMarkTempInfo(schemeId));
	}

	/**
	 * 获取专家评分数据
	 */
	@PostMapping("/getExpertEvalData")
	@ApiOperation(value = "获取专家评分数据", notes = "传入pageDTO")
	public ResultData<ExpertEvalDataVO> getExpertEvalData(@RequestParam Long noticeId, @RequestParam Long biddingInfoId) {
		return ResultData.data(expertEvaluationService.getExpertEvalData(noticeId, biddingInfoId));
	}

	/**
	 * 获取专家评分记录
	 */
	@PostMapping("/getExpertEvalRecord")
	@ApiOperation(value = "获取专家评分记录", notes = "传入pageDTO")
	public ResultData<List<ExpertEvalRecordVO>> getExpertEvalRecord(@RequestParam Long noticeId, @RequestParam Long vendorId) {
		return ResultData.data(expertEvaluationService.getExpertEvalRecord(noticeId, vendorId));
	}

	/**
	 * 专家评分
	 */
	@PostMapping("/eval")
	@ApiOperation(value = "专家评分", notes = "传入evalVO")
	public ResultData eval(@Valid @RequestBody EvalVO evalVO) {
		return ResultData.status(expertEvaluationService.eval(evalVO));
	}

}
