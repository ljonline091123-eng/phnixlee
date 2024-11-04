package com.zhaocai.business.bidding.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhaocai.business.bidding.domain.BiddingInfo;
import com.zhaocai.business.bidding.domain.BiddingResult;
import com.zhaocai.business.bidding.domain.TenderNotice;
import com.zhaocai.business.bidding.enums.TenderNoticeStatusEnum;
import com.zhaocai.business.bidding.mapper.BiddingResultMapper;
import com.zhaocai.business.bidding.service.IBiddingInfoService;
import com.zhaocai.business.bidding.service.IBiddingResultService;
import com.zhaocai.business.bidding.service.ITenderNoticeService;
import com.zhaocai.business.bidding.vo.req.CalibrationEntranceVO;
import com.zhaocai.business.bidding.vo.req.CalibrationReleaseVO;
import com.zhaocai.business.bidding.vo.req.CalibrationVO;
import com.zhaocai.business.bidding.vo.req.ResultReleasVO;
import com.zhaocai.business.bidding.vo.res.*;
import com.zhaocai.business.common.enums.*;
import com.zhaocai.business.common.exception.ParamValidateException;
import com.zhaocai.business.manager.http.dto.req.*;
import com.zhaocai.business.manager.http.dto.res.BpmAuditResponseDTO;
import com.zhaocai.business.manager.http.dto.res.BpmInitializeResponseDTO;
import com.zhaocai.business.manager.http.dto.res.BpmListProcessLogResponseDTO;
import com.zhaocai.business.manager.http.dto.res.BpmLoadTaskDefResponseDTO;
import com.zhaocai.business.process.service.IBPMProcessService;
import com.zhaocai.business.process.service.IPBMOverrideService;
import com.zhaocai.business.procurement.domain.ProcurementScheme;
import com.zhaocai.business.procurement.service.IProcurementSchemeService;
import com.zhaocai.business.procurement.vo.res.ProcurementSchemeBiddingVendorVO;
import com.zhaocai.business.pub.service.IAttachmentService;
import com.zhaocai.common.core.constant.NumberConstant;
import com.zhaocai.common.core.utils.DateUtils;
import com.zhaocai.common.core.utils.bean.BeanCopierUtil;
import com.zhaocai.common.core.web.bean.ResultData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 投标结果信息Service业务层处理
 *
 * @author WH
 * @date 2024-05-24
 */
@Service
public class BiddingResultServiceImpl extends ServiceImpl<BiddingResultMapper,BiddingResult> implements IBiddingResultService {

    @Lazy
    @Autowired
    private ITenderNoticeService tenderNoticeService;

    @Lazy
    @Autowired
    private IBiddingInfoService biddingInfoService;

    @Autowired
    private IProcurementSchemeService procurementSchemeService;

    @Autowired
    private IBPMProcessService processService;

    @Autowired
    private IAttachmentService attachmentService;

    @Override
    public BiddingResultDetailVO detail(Long id) {
        BiddingResult biddingResult = this.getById(id);
        BiddingResultDetailVO vo = BeanCopierUtil.copyBean(biddingResult, BiddingResultDetailVO.class);

        ProcurementScheme scheme = procurementSchemeService.getOne(new LambdaQueryWrapper<ProcurementScheme>()
                .eq(ProcurementScheme::getId, vo.getSchemeId()));
        if (!ObjectUtils.isEmpty(scheme)){
            vo.setProcurementSchemeName(scheme.getProcurementSchemeName());
            vo.setProcurementSchemeCode(scheme.getProcurementSchemeCode());
        }

        //获取投标单数据
        BiddingInfo biddingInfo = biddingInfoService.getById(biddingResult.getBiddingInfoId());
        vo.setTaxPrice(biddingInfo.getTaxPrice());
        vo.setNotTaxPrice(biddingInfo.getNotTaxPrice());
        vo.setBidTime(biddingInfo.getCreateTime());

        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean calibration(CalibrationEntranceVO entranceVO) {
        List<CalibrationVO> calibrationVOList = entranceVO.getCalibrationVOList();
        String detailUrl = entranceVO.getDetailUrl();
        //首先校验招标公告数据状态
        if (CollectionUtils.isEmpty(calibrationVOList)){
            throw new ParamValidateException("无定标数据");
        }
        Long noticeId = calibrationVOList.get(0).getNoticeId();
        TenderNotice tenderNotice = tenderNoticeService.getTenderNotice(noticeId);
        if (!TenderNoticeStatusEnum.CALI_REPORT.getState().equals(tenderNotice.getNoticeStatus())){
            throw new ParamValidateException("当前数据状态不能定标");
        }

        //保存定标结果数据
        List<BiddingResult> results = new ArrayList<>();
        for (CalibrationVO calibrationVO : calibrationVOList) {
            BiddingResult result = BeanCopierUtil.copyBean(calibrationVO, BiddingResult.class);
            results.add(result);
        }
        boolean res = this.saveBatch(results, results.size());

        //保存定标文件附件
        attachmentService.addAttachment(entranceVO.getCalibrationDocAttachList(), AttachmentTypeEnum.CALIBRATION_DOCUMENT,
                tenderNotice.getId());

        TenderNoticeSchemeInfoVO detailVO = tenderNoticeService.getTenderNoticeSchemeInfo(noticeId);
        Integer nextNoticeStatus = tenderNoticeService.nextTenderNoticeStatus(detailVO.getSchemeType(), detailVO.getNoticeStatus());
//        tenderNoticeService.updateStatus(noticeId, nextNoticeStatus); //审批流监听器去处理状态更新

        //调用第三方审批信息，审批通过之后才能流转到下一环节
        //接入底层逻辑平台流程
        Map<String,Object> paramMap = new HashMap<>();
        paramMap.put("businessId", noticeId);
        paramMap.put("detailUrl", detailUrl);
        paramMap.put("projectCode", detailVO.getProjectCode());
        paramMap.put("businessTitle", "招标管理-定标环节审批");
        paramMap.put("businessContent",
                String.format(ApproveFlowPromptTemplateEnum.BID_CALIBRATION.getDesc(),detailVO.getProcurementSchemeName()));
        paramMap.put("operateComment", entranceVO.getOperateComment());
        UserObj userObj = UserObj.builder().businessType(ProcessKeyEnum.ZHAOCAI_TENDER_CALIBRATE.name()).
                businessId(noticeId.toString()).noticeId(noticeId).schemeId(tenderNotice.getSchemeId())
                .toDoType(ToDoTypeEnum.EXAMINE.name()).build();
        paramMap.put("userObj", JSON.toJSONString(userObj));
        processService.startProcessInstance(ProcessKeyEnum.ZHAOCAI_TENDER_CALIBRATE.getIdentifying(), paramMap);

        //处理额外环节逻辑
        if (TenderNoticeStatusEnum.RESULT_RELEASE.getState().equals(nextNoticeStatus) &&
                (!detailVO.getSchemeType().equals(NumberConstant.ONE))){
            //如果下一阶段是‘结果发布’，并且采购方案类型为（邀请，询价，单一来源）那就直接处理‘中标公示’阶段数据
            this.updateBiddingResultStatus(noticeId, null);
        }

        return res;
    }

    @Override
    public List<BiddingResultListVO> getBiddingResult(Long noticeId) {
        //校验参数
        verifyData(noticeId, TenderNoticeStatusEnum.WINNING_BID.getState());

        List<BiddingResult> results = this.list(new LambdaQueryWrapper<BiddingResult>()
                .eq(BiddingResult::getNoticeId, noticeId)
                .orderByAsc(BiddingResult::getRank));
        List<BiddingResultListVO> resultVOList = BeanCopierUtil.copyList(results, BiddingResultListVO.class);



        return resultVOList;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean calibrationRelease(CalibrationReleaseVO calibrationReleaseVO) {
        //更新公示期
        boolean res = this.update(new LambdaUpdateWrapper<BiddingResult>()
                .set(BiddingResult::getPublicityStartTime, calibrationReleaseVO.getPublicityStartTime())
                .set(BiddingResult::getPublicityEndTime, calibrationReleaseVO.getPublicityEndTime())
                .eq(BiddingResult::getNoticeId, calibrationReleaseVO.getNoticeId()));

        //todo su 暂时这个现在演示不做时间限制，点击发布到下一步  张贵荣06-21
        /** --------------------------------------------------------- */
        this.updateBiddingResultStatus(calibrationReleaseVO.getNoticeId(), TenderNoticeStatusEnum.WINNING_BID.getState());
        Set<Long> noticeIdSet = new HashSet<>();
        noticeIdSet.add(calibrationReleaseVO.getNoticeId());

        for (Long noticeId : noticeIdSet) {
            //更新招标公告状态
            TenderNoticeSchemeInfoVO detailVO = tenderNoticeService.getTenderNoticeSchemeInfo(noticeId);
            Integer nextNoticeStatus = tenderNoticeService.nextTenderNoticeStatus(detailVO.getSchemeType(), detailVO.getNoticeStatus());
            tenderNoticeService.updateStatus(noticeId, nextNoticeStatus);
        }
        /** --------------------------------------------------------- */

        return res;
    }

    private void updateBiddingResultStatus(Long noticeId, Integer noticeStatus){
        //先查招标公告状态为 ‘中标公示’ 的投标结果信息数据
        List<NoticeBiddingResultListVO> noticeResultList = tenderNoticeService.findNoticeBiddingResultList(
                noticeId, noticeStatus, null);
        for (NoticeBiddingResultListVO vo : noticeResultList) {
            //如果当前数据去人中标，修改此投标结果为中标状态
            if (NumberConstant.ONE == vo.getSureBid()){
                this.update(new LambdaUpdateWrapper<BiddingResult>()
                        .set(BiddingResult::getBidResult, 1)
                        .eq(BiddingResult::getId, vo.getBiddingResultId()));
            } else {
                this.update(new LambdaUpdateWrapper<BiddingResult>()
                        .set(BiddingResult::getBidResult, 0)
                        .eq(BiddingResult::getId, vo.getBiddingResultId()));
            }

            //如果当前数据排名第一，修改此投标结果为中标状态
            /*if (vo.getRank() == 1){
                this.update(new LambdaUpdateWrapper<BiddingResult>()
                        .set(BiddingResult::getBidResult, 1)
                        .eq(BiddingResult::getId, vo.getBiddingResultId()));
            } else {
                this.update(new LambdaUpdateWrapper<BiddingResult>()
                        .set(BiddingResult::getBidResult, 0)
                        .eq(BiddingResult::getId, vo.getBiddingResultId()));
            }*/
        }


    }

    @Override
    public List<WinningBidResultVO> getWinningBidResult(Long noticeId) {
        //校验参数
        verifyData(noticeId, TenderNoticeStatusEnum.RESULT_RELEASE.getState());

        List<BiddingResult> results = this.list(new LambdaQueryWrapper<BiddingResult>()
                .eq(BiddingResult::getNoticeId, noticeId)
                .orderByAsc(BiddingResult::getRank));
        List<WinningBidResultVO> resultVOList = new ArrayList<>();
        Set<Long> schemeIdSet = new HashSet<>();
        for (BiddingResult result : results) {
            WinningBidResultVO resultVO = BeanCopierUtil.copyBean(result, WinningBidResultVO.class);

            //获取投标单数据
            BiddingInfo biddingInfo = biddingInfoService.getById(result.getBiddingInfoId());
            resultVO.setTaxPrice(biddingInfo.getTaxPrice());
            resultVO.setNotTaxPrice(biddingInfo.getNotTaxPrice());
            resultVO.setBidTime(biddingInfo.getCreateTime());

            resultVO.setBidResultText(resultVO.getBidResult() == 1 ? "中标" : "未中标");
            resultVOList.add(resultVO);

            if (!schemeIdSet.contains(result.getSchemeId())){
                ProcurementScheme scheme = procurementSchemeService.getOne(new LambdaQueryWrapper<ProcurementScheme>()
                        .eq(ProcurementScheme::getId, result.getSchemeId()));
                resultVO.setProcurementSchemeName(scheme.getProcurementSchemeName());
                schemeIdSet.add(result.getSchemeId());
            }

        }
        return resultVOList;

    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean winningBidResultRelease(ResultReleasVO resultReleasVO) {
        //更新 中标通知书内容，是否发送通知书，通知书发布时间
        boolean res = this.update(new LambdaUpdateWrapper<BiddingResult>()
                .set(BiddingResult::getSendNotified, NumberConstant.ONE)
                .set(BiddingResult::getNotifiContent, resultReleasVO.getNotifiContent())
                .set(BiddingResult::getNotifiTime, DateUtils.getNowDate())
                .eq(BiddingResult::getBidResult, NumberConstant.ONE)
                .eq(BiddingResult::getNoticeId, resultReleasVO.getNoticeId()));

        //点击发布进入到投标完成环节（结束）
        Integer nextNoticeStatus = findNextTenderNoticeStatus(resultReleasVO.getNoticeId());
        tenderNoticeService.updateStatus(resultReleasVO.getNoticeId(), nextNoticeStatus);
        return res;
    }

    @Override
    public List<BidResultVO> getBidResult(Long noticeId) {
        List<BiddingResult> results = this.list(new LambdaQueryWrapper<BiddingResult>()
                .eq(BiddingResult::getNoticeId, noticeId)
                .eq(BiddingResult::getBidResult, NumberConstant.ONE));
        List<BidResultVO> resultVOList = new ArrayList<>();
        for (BiddingResult result : results) {
            BidResultVO resultVO = BeanCopierUtil.copyBean(result, BidResultVO.class);
            resultVO.setBidResultText(resultVO.getBidResult() == 1 ? "中标" : "未中标");
            resultVOList.add(resultVO);
        }
        return resultVOList;
    }

    @Override
    public List<ProcurementSchemeBiddingVendorVO> listBiddingVendorBySchemeId(Long schemeId) {
        List<BiddingResult> biddingResults = baseMapper.selectBiddingVendorBySchemeId(schemeId);

        return biddingResults.stream()
                .map(x -> new ProcurementSchemeBiddingVendorVO(x.getVendorId(),x.getVendorName(),x.getBidResult())).collect(Collectors.toList());
    }

    private void verifyData(Long noticeId, Integer noticeState){
        //验证状态
        TenderNotice tenderNotice = tenderNoticeService.getById(noticeId);
        if (tenderNotice.getNoticeStatus().compareTo(noticeState) < 0){
            throw new ParamValidateException("当前公告状态无法查询相关数据");
        }
    }

    @Override
    public void processStart(Map<String, Object> variables) {
        String processId = variables.get("processId").toString();
        String businessId = variables.get("businessId").toString();
        Object flagObj = variables.get("completedFlag");
        Integer nextNoticeStatus = null;
        if (!ObjectUtils.isEmpty(flagObj) && ProcessStateEnum.COMPLETED.getDesc().equals(flagObj.toString())){
            //如果流程状态为已完成，则直接更新状态
            nextNoticeStatus = findNextTenderNoticeStatus(Long.valueOf(businessId));
        }
        tenderNoticeService.update(new LambdaUpdateWrapper<TenderNotice>()
                .set(null != nextNoticeStatus, TenderNotice::getNoticeStatus, nextNoticeStatus)
                .set(TenderNotice::getWfProcessId, processId)
                .eq(TenderNotice::getId, Long.valueOf(businessId)));
    }

    @Override
    public void processAuditPass(Map<String, Object> variables) {
        String businessId = variables.get("businessId").toString();
        Integer nextNoticeStatus = findNextTenderNoticeStatus(Long.valueOf(businessId));
        tenderNoticeService.updateStatus(Long.valueOf(businessId), nextNoticeStatus);
    }

    @Override
    public void processAuditFreedom(Map<String, Object> variables) {

    }

    @Override
    public void processAuditReject(Map<String, Object> variables) {

    }

    @Override
    public ResultData<BpmInitializeResponseDTO> initialize(BpmInitializeRequestDTO requestDTO) {
        return null;
    }

    @Override
    public ResultData<List<BpmListProcessLogResponseDTO>> listProcessLog(BpmListProcessLogRequestDTO requestDTO) {
        return null;
    }

    @Override
    public String audit(String processKey, Map<String, Object> variables) {
        return null;
    }

    @Override
    public ResultData<List<BpmLoadTaskDefResponseDTO>> loadTaskDef(BpmLoadTaskDefRequestDTO requestDTO) {
        return null;
    }


    private Integer findNextTenderNoticeStatus(Long noticeId){
        TenderNoticeSchemeInfoVO detailVO = tenderNoticeService.getTenderNoticeSchemeInfo(noticeId);
        return tenderNoticeService.nextTenderNoticeStatus(detailVO.getSchemeType(), detailVO.getNoticeStatus());
    }

}
