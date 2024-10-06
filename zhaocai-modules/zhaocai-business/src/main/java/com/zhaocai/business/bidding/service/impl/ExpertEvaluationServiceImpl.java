package com.zhaocai.business.bidding.service.impl;

import cn.hutool.core.util.NumberUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.zhaocai.business.bidding.domain.*;
import com.zhaocai.business.bidding.enums.BiddingInfoStatusEnum;
import com.zhaocai.business.bidding.enums.TenderNoticeStatusEnum;
import com.zhaocai.business.bidding.service.*;
import com.zhaocai.business.bidding.vo.req.EvalItemVO;
import com.zhaocai.business.bidding.vo.req.EvalVO;
import com.zhaocai.business.bidding.vo.req.EvalVendorCountVO;
import com.zhaocai.business.bidding.vo.req.query.EvalTaskPageVO;
import com.zhaocai.business.bidding.vo.res.*;
import com.zhaocai.business.common.enums.AttachmentTypeEnum;
import com.zhaocai.business.common.exception.ParamValidateException;
import com.zhaocai.business.common.utils.ValidateUtils;
import com.zhaocai.business.expert.domain.Expert;
import com.zhaocai.business.expert.domain.ExpertScore;
import com.zhaocai.business.expert.service.IExpertScoreService;
import com.zhaocai.business.expert.service.IExpertService;
import com.zhaocai.business.procurement.domain.ProcurementSchemeBidding;
import com.zhaocai.business.procurement.service.IProcurementSchemeBiddingService;
import com.zhaocai.business.procurement.service.IProcurementSchemeService;
import com.zhaocai.business.pub.service.IAttachmentService;
import com.zhaocai.business.pub.vo.res.AttachmentVO;
import com.zhaocai.common.core.bean.PageResult;
import com.zhaocai.common.core.constant.NumberConstant;
import com.zhaocai.common.core.utils.DateUtils;
import com.zhaocai.common.core.utils.bean.BeanCopierUtil;
import com.zhaocai.common.core.web.domain.BaseEntity;
import com.zhaocai.common.security.utils.SecurityUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static java.math.BigDecimal.ROUND_DOWN;

/**
 * @author ssy
 * @date 2024/5/14 11:35
 */
@Slf4j
@Service
public class ExpertEvaluationServiceImpl implements IExpertEvaluationService {

	@Autowired
	private IBiddingEvaluatExpertService biddingEvaluatExpertService;
	@Autowired
	private IProcurementSchemeService procurementSchemeService;
	@Autowired
	private IExpertService expertService;
	@Autowired
	private IBiddingInfoService biddingInfoService;
	@Autowired
	private IProcurementSchemeBiddingService procurementSchemeBiddingService;
	@Autowired
	private IBiddingMarkTemplateService biddingMarkTemplateService;
	@Autowired
	private IBiddingMarkItemService biddingMarkItemService;
	@Autowired
	private IBiddingItemGradeService biddingItemGradeService;
	@Autowired
	private IExpertScoreService expertScoreService;
	@Autowired
	private IAttachmentService attachmentService;
	@Autowired
	private ITenderNoticeService tenderNoticeService;


	@Override
	public PageResult<EvalTaskPageListVO> todoEvalTaskPage(EvalTaskPageVO pageVO) {
		PageResult<EvalTaskPageListVO> pageResult = new PageResult<>();
		Expert expert = getExpert(SecurityUtils.getUserId());
		if (ObjectUtils.isEmpty(expert)){
			return pageResult;
		}

		Long expertId = expert.getId();
		pageVO.setExpertId(expertId);
		//查询未评标的数据
		//pageVO.setEvalStatus(0);
		//查询需要评标的数据
//		pageVO.setIsEval(1);
		pageVO.setNoticeStatus(TenderNoticeStatusEnum.EVALUATION_BID.getState());
		//查询当前登录专家需要评标的方案 -> 查询方案对应的投标供应商
		pageResult = biddingEvaluatExpertService.getEvalSchemaPage(pageVO);
		List<EvalTaskPageListVO> rows = pageResult.getRows();
		for (EvalTaskPageListVO row : rows) {
			List<EvalTaskContentVO> contentVOList = new ArrayList<>();
			//查询当前采购方案-招标公告 下面有多少投标单信息(状态等于1的数据 已投标（对供应商端）|已回标（对采购端） 其余已撤回 已废标 未投标的 数据不显示)
			List<BiddingInfo> biddingInfos = biddingInfoService.list(new LambdaQueryWrapper<BiddingInfo>()
					.eq(BiddingInfo::getNoticeId, row.getNoticeId())
					.eq(BiddingInfo::getSchemeId, row.getSchemeId())
					.eq(BiddingInfo::getBiddingStatus, BiddingInfoStatusEnum.HAVE_BACK.getState())
					.eq(BiddingInfo::getSubmitStatus, 1)
					.isNull(BiddingInfo::getParentId));
			for (BiddingInfo biddingInfo : biddingInfos){
				//查询是否有最新的报价信息
				BiddingInfo newestBiddingInfo = biddingInfoService.getOne(new LambdaQueryWrapper<BiddingInfo>()
						.eq(BiddingInfo::getParentId, biddingInfo.getId())
						.orderByDesc(BiddingInfo::getCreateTime).last("limit 1"));
				if (!ObjectUtils.isEmpty(newestBiddingInfo)){
					biddingInfo = newestBiddingInfo;
				}

				EvalTaskContentVO contentVO = BeanCopierUtil.copyBean(biddingInfo, EvalTaskContentVO.class);
//				contentVO.setTaxPrice(contentVO.getTaxPrice().setScale(2, ROUND_DOWN));
//				contentVO.setNotTaxPrice(contentVO.getNotTaxPrice().setScale(2, ROUND_DOWN));
//				contentVO.setTaxPricePattern(NumberUtil.decimalFormat("#,###.00", contentVO.getTaxPrice()));
//				contentVO.setNotTaxPricePattern(NumberUtil.decimalFormat("#,###.00", contentVO.getNotTaxPrice()));
				contentVO.setBiddingInfoId(biddingInfo.getId());

				//查询投标标书附件
				List<AttachmentVO> attachments = attachmentService.listAttachment(AttachmentTypeEnum.BIDING_DOCUMENT, biddingInfo.getId());
				contentVO.setAttachments(attachments);

				ExpertScore expertScore = expertScoreService.getOne(new LambdaQueryWrapper<ExpertScore>()
						.eq(ExpertScore::getBiddingInfoId, biddingInfo.getId())
						.eq(ExpertScore::getExpertId, expertId)
						.orderByDesc(BaseEntity::getCreateTime).last("limit 1"));
				if (!ObjectUtils.isEmpty(expertScore)){
					//已评标
					contentVO.setIsEval(true);
					contentVO.setBusScore(expertScore.getBusScore());
					contentVO.setTechScore(expertScore.getTechScore());
				}
				contentVOList.add(contentVO);
			}

			row.setEvalTaskContentVOList(contentVOList);
		}
		return pageResult;
	}

	@Override
	public PageResult<EvalTaskPageListVO> doneEvalTaskPage(EvalTaskPageVO pageVO) {
		PageResult<EvalTaskPageListVO> pageResult = new PageResult<>();
		Expert expert = getExpert(SecurityUtils.getUserId());
		if (ObjectUtils.isEmpty(expert)){
			return pageResult;
		}
		Long expertId = expert.getId();
		pageVO.setExpertId(expertId);
		//查询已评标的数据
		//pageVO.setEvalStatus(1);
		//查询不需要评标的数据
//		pageVO.setIsEval(0);
		pageVO.setDoneNoticeStatus(TenderNoticeStatusEnum.EVALUATION_BID.getState());
		//查询当前登录专家需要评标的方案 -> 查询方案对应的投标供应商
		pageResult = biddingEvaluatExpertService.getEvalSchemaPage(pageVO);
		List<EvalTaskPageListVO> rows = pageResult.getRows();
		for (EvalTaskPageListVO row : rows) {
			List<EvalTaskContentVO> contentVOList = new ArrayList<>();
			//查询当前采购方案-招标公告 下面有多少首轮投标单信息(状态等于1的数据 已投标（对供应商端）|已回标（对采购端） 其余已撤回 已废标 未投标的 数据不显示)
			List<BiddingInfo> biddingInfos = biddingInfoService.list(new LambdaQueryWrapper<BiddingInfo>()
					.eq(BiddingInfo::getNoticeId, row.getNoticeId())
					.eq(BiddingInfo::getSchemeId, row.getSchemeId())
					.eq(BiddingInfo::getBiddingStatus, BiddingInfoStatusEnum.HAVE_BACK.getState())
					.eq(BiddingInfo::getSubmitStatus, 1)
					.isNull(BiddingInfo::getParentId));
			for (BiddingInfo biddingInfo : biddingInfos){
				//查询是否有最新的报价信息
				BiddingInfo newestBiddingInfo = biddingInfoService.getOne(new LambdaQueryWrapper<BiddingInfo>()
						.eq(BiddingInfo::getParentId, biddingInfo.getId())
						.orderByDesc(BiddingInfo::getCreateTime).last("limit 1"));
				if (!ObjectUtils.isEmpty(newestBiddingInfo)){
					biddingInfo = newestBiddingInfo;
				}

				EvalTaskContentVO contentVO = BeanCopierUtil.copyBean(biddingInfo, EvalTaskContentVO.class);
//				contentVO.setTaxPrice(contentVO.getTaxPrice().setScale(2, ROUND_DOWN));
//				contentVO.setNotTaxPrice(contentVO.getNotTaxPrice().setScale(2, ROUND_DOWN));
//				contentVO.setTaxPricePattern(NumberUtil.decimalFormat("#,###.00", contentVO.getTaxPrice()));
//				contentVO.setNotTaxPricePattern(NumberUtil.decimalFormat("#,###.00", contentVO.getNotTaxPrice()));
				contentVO.setBiddingInfoId(biddingInfo.getId());

				//查询投标标书附件
				List<AttachmentVO> attachments = attachmentService.listAttachment(AttachmentTypeEnum.BIDING_DOCUMENT, biddingInfo.getId());
				contentVO.setAttachments(attachments);

				ExpertScore expertScore = expertScoreService.getOne(new LambdaQueryWrapper<ExpertScore>()
						.eq(ExpertScore::getBiddingInfoId, biddingInfo.getId())
						.eq(ExpertScore::getExpertId, expertId)
						.orderByDesc(BaseEntity::getCreateTime).last("limit 1"));
				if (!ObjectUtils.isEmpty(expertScore)){
					//已评标
					contentVO.setIsEval(true);
					contentVO.setBusScore(expertScore.getBusScore());
					contentVO.setTechScore(expertScore.getTechScore());
				}
				contentVOList.add(contentVO);
			}
			row.setEvalTaskContentVOList(contentVOList);
		}
		return pageResult;
	}

	@Override
	public BiddingMarkTemplateDetailVO getMarkTempInfo(Long schemeId) {
		//根据采购方案获取采购方案对应的招标信息
		ProcurementSchemeBidding schemeBidding = procurementSchemeBiddingService.getOne(new LambdaQueryWrapper<ProcurementSchemeBidding>()
				.eq(ProcurementSchemeBidding::getSchemeId, schemeId));
		ValidateUtils.isNullException(schemeBidding, "该采购方案对应的招标信息不存在");
		//获取评分模板id
		Long evaluationTemplateId = schemeBidding.getEvaluationTemplateId();
		BiddingMarkTemplateDetailVO templateDetailVO = biddingMarkTemplateService.detail(evaluationTemplateId);
		return templateDetailVO;
	}

	@Override
	public ExpertEvalDataVO getExpertEvalData(Long noticeId, Long biddingInfoId) {
		ExpertEvalDataVO dataVO = new ExpertEvalDataVO();
		//获取专家id
		Long expertId = getExpert(SecurityUtils.getUserId()).getId();
		//查询专家评分数据表（最新一条）
		ExpertScore expertScore = expertScoreService.getOne(new LambdaQueryWrapper<ExpertScore>()
				.eq(ExpertScore::getNoticeId, noticeId)
				.eq(ExpertScore::getBiddingInfoId, biddingInfoId)
				.eq(ExpertScore::getExpertId, expertId)
				.orderByDesc(ExpertScore::getCreateTime).last("limit 1"));
		if (ObjectUtils.isEmpty(expertScore)){
			return dataVO;
		}
		dataVO = BeanCopierUtil.copyBean(expertScore, ExpertEvalDataVO.class);

		List<ExpertEvalDetailDataVO> detailDataVOList = new ArrayList<>();
		//查询评分细项
		List<BiddingItemGrade> itemGrades = biddingItemGradeService.list(new LambdaQueryWrapper<BiddingItemGrade>()
				.eq(BiddingItemGrade::getExpertScoreId, expertScore.getId()));
		for (BiddingItemGrade itemGrade : itemGrades){
			ExpertEvalDetailDataVO detailDataVO = BeanCopierUtil.copyBean(itemGrade, ExpertEvalDetailDataVO.class);
			BiddingMarkItem markItem = biddingMarkItemService.getOne(new LambdaQueryWrapper<BiddingMarkItem>()
					.eq(BiddingMarkItem::getId, itemGrade.getItemId()));
			detailDataVO.setName(markItem.getName());
			detailDataVO.setLowRange(markItem.getLowRange());
			detailDataVO.setHighRange(markItem.getHighRange());
			detailDataVO.setCategoryId(markItem.getCategoryId());
			detailDataVOList.add(detailDataVO);
		}
		dataVO.setDetailDataVOList(detailDataVOList);
		return dataVO;
	}

	@Override
	public List<ExpertEvalRecordVO> getExpertEvalRecord(Long noticeId, Long vendorId) {
		//获取专家id
		Long expertId = getExpert(SecurityUtils.getUserId()).getId();
		//查询专家评分数据
		List<ExpertScore> expertScores = expertScoreService.list(new LambdaQueryWrapper<ExpertScore>()
				.eq(ExpertScore::getNoticeId, noticeId)
				.eq(ExpertScore::getVendorId, vendorId)
				.eq(ExpertScore::getExpertId, expertId)
				.orderByDesc(ExpertScore::getEvaTime));
		List<ExpertEvalRecordVO> recordList = BeanCopierUtil.copyList(expertScores, ExpertEvalRecordVO.class);
		return recordList;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean eval(EvalVO evalVO) {
		//获取当前登录用户专家id
		Long expertId = getExpert(SecurityUtils.getUserId()).getId();

		verifyEvalParam(evalVO);

		//新增专家对供应商投标单评分表
		ExpertScore expertScore = new ExpertScore();
		expertScore.setSchemeId(evalVO.getSchemeId());
		expertScore.setNoticeId(evalVO.getNoticeId());
		expertScore.setEvaOpinion(evalVO.getAdvice());
		expertScore.setVendorId(evalVO.getVendorId());
		expertScore.setBiddingInfoId(evalVO.getBiddingInfoId());
		expertScore.setExpertId(expertId);
		expertScore.setEvaTime(DateUtils.getNowDate());
		expertScoreService.save(expertScore);

		List<BiddingItemGrade> itemGradeList = new ArrayList<>();
		BigDecimal busScore = BigDecimal.ZERO;
		BigDecimal techScore = BigDecimal.ZERO;
		for (List<EvalItemVO> itemDTOS : evalVO.getEvalItemDTOSList()){
			BigDecimal totalScore = BigDecimal.ZERO;
			Integer itemType = 0;
			for (EvalItemVO itemDTO : itemDTOS) {
				BiddingItemGrade itemGrade = new BiddingItemGrade();
				//专家用户id
				itemGrade.setExpertId(expertId);
				itemGrade.setSchemaId(evalVO.getSchemeId());
				itemGrade.setVendorId(evalVO.getVendorId());
				itemGrade.setItemId(itemDTO.getItemId());
				itemGrade.setScore(itemDTO.getScore());
				itemGrade.setExpertScoreId(expertScore.getId());
				itemGradeList.add(itemGrade);

				//获取总分
				if (itemDTO.getScore() != null){
					totalScore = totalScore.add(itemDTO.getScore());
				}
				//设置评分模板项类型
				itemType = itemDTO.getItemType();
			}
			if (itemType == 1) {
				//技术评分
				techScore = totalScore;
			} else if (itemType == 2){
				//商务评分
				busScore = totalScore;
			}
		}
		boolean res = biddingItemGradeService.saveBatch(itemGradeList);

		//更新商务-技术评分
		expertScore.setBusScore(busScore);
		expertScore.setTechScore(techScore);
		expertScoreService.updateById(expertScore);

		EvalVendorCountVO queryDTO = new EvalVendorCountVO();
		queryDTO.setExpertId(expertId);
		queryDTO.setSchemeId(evalVO.getSchemeId());
		queryDTO.setVendorId(evalVO.getVendorId());
		queryDTO.setNoticeId(evalVO.getNoticeId());
		Integer count = expertScoreService.getNotEvalVendorCount(queryDTO);
		//查询该评分供应商是否为最后一个未评标的供应商，如果是则修改该专家对招标项目的评标状态
		if (count == 0){
			biddingEvaluatExpertService.update(new LambdaUpdateWrapper<BiddingEvaluatExpert>()
					.set(BiddingEvaluatExpert::getEvalStatus, NumberConstant.ONE)
					.eq(BiddingEvaluatExpert::getSchemeId, evalVO.getSchemeId())
					.eq(BiddingEvaluatExpert::getExpertId, expertId)
					.eq(BiddingEvaluatExpert::getIsJoin, 1));
		}
		return res;
	}

	private void verifyEvalParam(EvalVO evalVO){
		//判断当前招标公告流程状态
		if (tenderNoticeService.getCountTenderNoticeStatus(
				evalVO.getNoticeId(), TenderNoticeStatusEnum.EVALUATION_BID.getState()) == 0){
			throw new ParamValidateException("投标公告状态已变更，请确认当前招标公告状态");
		}
		//判断公告是否开启专家评标
		long count = tenderNoticeService.count(new LambdaQueryWrapper<TenderNotice>()
				.eq(TenderNotice::getId, evalVO.getNoticeId())
				.eq(TenderNotice::getIsEval, 1));
		if (count == 0){
			throw new ParamValidateException("请开启专家评标");
		}
	}

	private Expert getExpert(Long userId){
		return expertService.getOne(new LambdaQueryWrapper<Expert>()
				.eq(Expert::getUserId, userId));
	}

}
