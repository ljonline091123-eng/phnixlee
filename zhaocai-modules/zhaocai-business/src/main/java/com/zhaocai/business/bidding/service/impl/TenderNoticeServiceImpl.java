package com.zhaocai.business.bidding.service.impl;

import cn.hutool.extra.spring.SpringUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhaocai.business.agreement.domain.Agreement;
import com.zhaocai.business.agreement.service.IAgreementService;
import com.zhaocai.business.bidding.domain.*;
import com.zhaocai.business.bidding.enums.TenderNoticeStatusEnum;
import com.zhaocai.business.bidding.mapper.TenderNoticeMapper;
import com.zhaocai.business.bidding.service.*;
import com.zhaocai.business.bidding.vo.req.TenderNoticeVO;
import com.zhaocai.business.bidding.vo.req.query.*;
import com.zhaocai.business.bidding.vo.res.*;
import com.zhaocai.business.common.cache.DictBizCache;
import com.zhaocai.business.common.enums.AgreementStateEnum;
import com.zhaocai.business.common.enums.AttachmentTypeEnum;
import com.zhaocai.business.common.enums.DictBizEnum;
import com.zhaocai.business.common.enums.VendorStateEnum;
import com.zhaocai.business.common.exception.ParamValidateException;
import com.zhaocai.business.common.sms.SmsSenderUtil;
import com.zhaocai.business.manager.http.service.PerformanceEvaluationService;
import com.zhaocai.business.procurement.domain.ProcurementScheme;
import com.zhaocai.business.procurement.service.IProcurementSchemeService;
import com.zhaocai.business.procurement.vo.res.MinProjectDataVO;
import com.zhaocai.business.pub.service.IAttachmentService;
import com.zhaocai.business.pub.vo.res.AttachmentVO;
import com.zhaocai.business.vendor.domain.Vendor;
import com.zhaocai.business.vendor.service.IVendorContactService;
import com.zhaocai.business.vendor.service.IVendorService;
import com.zhaocai.business.vendor.vo.req.VendorManagementListQueryDataVO;
import com.zhaocai.business.vendor.vo.res.VendorMainContactVO;
import com.zhaocai.business.vendor.vo.res.VendorManagementListDataVO;
import com.zhaocai.common.core.bean.PageResult;
import com.zhaocai.common.core.constant.NumberConstant;
import com.zhaocai.common.core.utils.DateUtils;
import com.zhaocai.common.core.utils.bean.BeanCopierUtil;
import com.zhaocai.common.security.utils.SecurityUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 招标公告Service业务层处理
 *
 * @author WH
 * @date 2024-05-24
 */
@Service
public class TenderNoticeServiceImpl extends ServiceImpl<TenderNoticeMapper,TenderNotice> implements ITenderNoticeService {

    @Autowired
    private ITenderNoticeRangeService tenderNoticeRangeService;
    @Autowired
    private ITenderNoticeChangeRecordService tenderNoticeChangeRecordService;
    @Autowired
    private IAttachmentService attachmentService;
    @Autowired
    private IBiddingResultService biddingResultService;
    @Autowired
    private IVendorContactService vendorContactService;
    @Autowired
    private IVendorService vendorService;
    @Autowired
    private IProcurementSchemeService procurementSchemeService;
    @Lazy
    @Autowired
    private IBiddingOpenPeopleService biddingOpenPeopleService;

    @Autowired
    private PerformanceEvaluationService performanceEvaluationService;

    @Autowired
    private IAgreementService agreementService;

    @Autowired
    private SmsSenderUtil smsSenderUtil = SpringUtil.getBean(SmsSenderUtil.class);

    @Override
    public PageResult<TenderNoticeListVO> page(TenderNoticeQueryVO queryDTO) {
        IPage<TenderNoticeListVO> iPage = baseMapper.page(queryDTO.toMybatisPage(), queryDTO);
        return new PageResult<>(iPage);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED,rollbackFor = Exception.class)
    public boolean add(TenderNoticeVO tenderNoticeVO) {
        verifyParam(tenderNoticeVO);
        TenderNotice tenderNotice = BeanCopierUtil.copyBean(tenderNoticeVO, TenderNotice.class);

        List<Long> vendorIds = tenderNoticeVO.getVendorIds();
        if (tenderNoticeVO.getSchemeType() != null && 1 == tenderNoticeVO.getSchemeType()){
            //如果是公开招标，那就获取到查询供应商范围的条件，由程序来获取条件内的供应商信息
            VendorManagementListQueryDataVO vendorQueryParam = new VendorManagementListQueryDataVO();
            if (!ObjectUtils.isEmpty(tenderNoticeVO.getVendorQueryParam())){
                vendorQueryParam = tenderNoticeVO.getVendorQueryParam();
            }
            List<VendorManagementListDataVO> vendorList = vendorService.getListVendor(vendorQueryParam);
            if (!CollectionUtils.isEmpty(vendorList)){
                vendorIds = vendorList.stream().map(VendorManagementListDataVO::getId).collect(Collectors.toList());
            }
        }
        //是否设置供应商范围表
        boolean flag = false;
        if (CollectionUtils.isEmpty(vendorIds)){
            tenderNotice.setVendorRange(NumberConstant.ZERO);
        } else {
            tenderNotice.setVendorRange(NumberConstant.ONE);
            flag = true;
        }

        tenderNotice.setNoticeStatus(TenderNoticeStatusEnum.TENDER_ISSUE.getState());
        //保存招标公告信息
        boolean res = this.save(tenderNotice);

        //保存招标文件附件
        attachmentService.addAttachment(tenderNoticeVO.getBiddingDocAttachList(), AttachmentTypeEnum.BIDING_NOTICE_DOC,
                tenderNotice.getId());

        //保存供应商范围表
        if (flag){
            //设置范围类型（设置供应商范围|推荐供应商）
            Integer rangeType = NumberConstant.ZERO;
            if (tenderNoticeVO.getSchemeType() != null && 1 != tenderNoticeVO.getSchemeType()){
                rangeType = NumberConstant.ONE;
            }
            List<TenderNoticeRange> ranges = new ArrayList<>();
            List<String> phoneList = new ArrayList<>();
            for (Long vendorId : vendorIds) {
                TenderNoticeRange range = new TenderNoticeRange();
                range.setNoticeId(tenderNotice.getId());
                range.setVendorId(vendorId);
                range.setType(rangeType);
                ranges.add(range);

                //获取供应商的手机号
                VendorMainContactVO contactVO = vendorContactService.getMainContact(vendorId);
                if (!ObjectUtils.isEmpty(contactVO)){
                    phoneList.add(contactVO.getContactPhone());
                }
            }
            tenderNoticeRangeService.saveBatch(ranges, ranges.size());

            //todo su 2027/07/29 张贵荣 先把这个功能关闭吧，发短信有点费钱
/*
            LinkedHashMap<String, String> varParam = new LinkedHashMap<>();
            varParam.put("unit_name", "湖南建投");
            varParam.put("name", "");
            varParam.put("Task", "");
            varParam.put("time", DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD_HH_MM, tenderNoticeVO.getApplyTime()));
            smsSenderUtil.sendMessage(SmsTemplateEnum.TENDER_NOTICE.getCode(), phoneList, varParam);
*/

//             smsSenderUtil.sendMessage("湖南建投",
//                    DictBizCache.getValue(DictBizEnum.PROCUREMENT_TYPE, String.valueOf(tenderNoticeVO.getSchemeType())),
//                    DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD_HH_MM, tenderNoticeVO.getApplyTime()),
//                    phoneList);
        }
        return res;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean aNewAdd(TenderNoticeVO tenderNoticeVO) {
        verifyNewAddParam(tenderNoticeVO);
        //删除旧招标公告数据
        this.remove(new LambdaUpdateWrapper<TenderNotice>()
                .eq(TenderNotice::getSchemeId, tenderNoticeVO.getSchemeId()));
        return this.add(tenderNoticeVO);
    }

    private void verifyNewAddParam(TenderNoticeVO tenderNoticeVO){
        TenderNotice tenderNotice = this.getOne(new LambdaQueryWrapper<TenderNotice>()
                .eq(TenderNotice::getSchemeId, tenderNoticeVO.getSchemeId()));
        if (ObjectUtils.isEmpty(tenderNotice)){
            throw new ParamValidateException("未查询到旧招标数据，不能重新招标");
        }
        if (!TenderNoticeStatusEnum.ABANDON_BID.getState().equals(tenderNotice.getNoticeStatus())){
            throw new ParamValidateException("只允许废标状态的数据重新招标");
        }
    }

    @Override
    public boolean handleTenderNoticeIssueStatus() {
        Date nowDate = DateUtils.getNowDate();
        //1.查询当前时间之前，状态为已发布的招标公告
        List<TenderNotice> tenderNotices = this.list(new LambdaQueryWrapper<TenderNotice>()
                .eq(TenderNotice::getNoticeStatus, TenderNoticeStatusEnum.TENDER_ISSUE.getState())
                .le(TenderNotice::getApplyTime, nowDate));
        //2.按照投标截止时间更改状态
        if (!CollectionUtils.isEmpty(tenderNotices)){
            for (TenderNotice tenderNotice : tenderNotices) {
                TenderNoticeSchemeInfoVO detailVO = this.getTenderNoticeSchemeInfo(tenderNotice.getId());
                if (!ObjectUtils.isEmpty(detailVO)){
                    Integer nextNoticeStatus = this.nextTenderNoticeStatus(detailVO.getSchemeType(), tenderNotice.getNoticeStatus());

                    TenderNoticeChangeRecord changeRecord = getNoticeTimeChange(tenderNotice.getId());
                    if (!ObjectUtils.isEmpty(changeRecord)){
                        //招标截止时间 的更改记录
                        Date bidEndTime = DateUtils.strToDate(changeRecord.getUpdateAfter(), DateUtils.YYYY_MM_DD_HH_MM);
                        if (nowDate.after(bidEndTime)){
                            tenderNotice.setNoticeStatus(nextNoticeStatus);
                        }
                    } else {
                        tenderNotice.setNoticeStatus(nextNoticeStatus);
                    }
                }
            }
            this.updateBatchById(tenderNotices);
        }
        return true;
    }

    @Override
    public boolean handleTenderNoticePublicityStatus() {
        //先查招标公告状态为 ‘中标公示’ 的投标结果信息数据
        List<NoticeBiddingResultListVO> noticeResultList = baseMapper.findNoticeBiddingResultList(null,
                TenderNoticeStatusEnum.WINNING_BID.getState(),
                DateUtils.strToDate(DateUtils.getDate(), DateUtils.YYYY_MM_DD));
        Set<Long> noticeIdSet = new HashSet<>();
        for (NoticeBiddingResultListVO vo : noticeResultList) {
            //如果当前数据排名第一，修改此投标结果为中标状态
            if (vo.getRank() == 1){
                biddingResultService.update(new LambdaUpdateWrapper<BiddingResult>()
                        .set(BiddingResult::getBidResult, 1)
                        .eq(BiddingResult::getId, vo.getBiddingResultId()));
            } else {
                biddingResultService.update(new LambdaUpdateWrapper<BiddingResult>()
                        .set(BiddingResult::getBidResult, 0)
                        .eq(BiddingResult::getId, vo.getBiddingResultId()));
            }

            noticeIdSet.add(vo.getNoticeId());
        }

        for (Long noticeId : noticeIdSet) {
            TenderNoticeSchemeInfoVO detailVO = this.getTenderNoticeSchemeInfo(noticeId);
            Integer nextNoticeStatus = this.nextTenderNoticeStatus(detailVO.getSchemeType(), detailVO.getNoticeStatus());
            //更新招标公告状态
            this.updateStatus(noticeId, nextNoticeStatus);
        }

        return true;
    }

    @Override
    public List<NoticeBiddingResultListVO> findNoticeBiddingResultList(Long noticeId, Integer noticeStatus, Date date){
        return baseMapper.findNoticeBiddingResultList(
                noticeId, noticeStatus, date);
    }

    @Override
    public TenderNoticeDetailVO getInfo(Long schemeId, Long noticeId) {
        TenderNoticeDetailVO vo = new TenderNoticeDetailVO();
        TenderNotice tenderNotice = this.getOne(new LambdaQueryWrapper<TenderNotice>()
                .eq(null != noticeId, TenderNotice::getId, noticeId)
                .eq(TenderNotice::getSchemeId, schemeId));
        if (ObjectUtils.isEmpty(tenderNotice)){
            return vo;
        }

        vo.setTenderNotice(tenderNotice);
        vo.setNoticeStatusText(TenderNoticeStatusEnum.getValueByCode(vo.getTenderNotice().getNoticeStatus()));

        TenderNoticeChangeRecord changeRecord = tenderNoticeChangeRecordService.getOne(
                new LambdaQueryWrapper<TenderNoticeChangeRecord>()
                .eq(TenderNoticeChangeRecord::getNoticeId, tenderNotice.getId())
                .eq(TenderNoticeChangeRecord::getType, NumberConstant.ONE)
                .orderByDesc(TenderNoticeChangeRecord::getCreateTime).last("limit 1"));
        if (!ObjectUtils.isEmpty(changeRecord)){
            vo.setBidEndTime(DateUtils.strToDate(changeRecord.getUpdateAfter(), DateUtils.YYYY_MM_DD_HH_MM_SS));
        }
        List<AttachmentVO> attachmentList = attachmentService.listAttachment(AttachmentTypeEnum.BIDING_NOTICE_DOC, tenderNotice.getId());
        vo.setAttachmentList(attachmentList);

        List<AttachmentVO> calibrationAttachmentList = attachmentService.listAttachment(AttachmentTypeEnum.CALIBRATION_DOCUMENT, tenderNotice.getId());
        vo.setCalibrationAttachmentList(calibrationAttachmentList);

        confirmInfo(vo, schemeId, noticeId);
        return vo;
    }

    private void confirmInfo(TenderNoticeDetailVO vo, Long schemeId, Long noticeId){
        Boolean purchaseOfficerVal = Boolean.FALSE;
        Boolean confirmUserVal = Boolean.FALSE;
        Boolean openTodoUserVal = Boolean.FALSE;
        if (null != SecurityUtils.getUserId()){
            ProcurementScheme procurementScheme = procurementSchemeService.getById(schemeId);
            //如果当前登录用户是采购经办人
            if (SecurityUtils.getUserId().equals(procurementScheme.getProcurementOfficer())){
                purchaseOfficerVal = Boolean.TRUE;
            }
            //查询当前登录用户是否为财务确认人员
            if (SecurityUtils.getUserId().toString().equals(procurementScheme.getFinanceConfirmId())){
                confirmUserVal = Boolean.TRUE;
            }
            //查询当前登录用户是否为开标待办人员
            long openCount = biddingOpenPeopleService.count(new LambdaQueryWrapper<BiddingOpenPeople>()
                    .eq(BiddingOpenPeople::getNoticeId, noticeId)
                    .eq(BiddingOpenPeople::getUserId, SecurityUtils.getUserId()));
            if (openCount > 0){
                openTodoUserVal = Boolean.TRUE;
            }
        }
        vo.setPurchaseOfficer(purchaseOfficerVal);
        vo.setFinanceConfirmUser(confirmUserVal);
        vo.setOpenTodoUser(openTodoUserVal);
    }

    @Override
    public TenderNoticeDetailVO detail(Long id) {
        TenderNoticeDetailVO vo = new TenderNoticeDetailVO();
        TenderNoticeSchemeInfoVO tenderNoticeSchemeInfo = baseMapper.findTenderNoticeSchemeInfo(id);
        if (ObjectUtils.isEmpty(tenderNoticeSchemeInfo)){
            return vo;
        }
        TenderNotice tenderNotice = BeanCopierUtil.copyBean(tenderNoticeSchemeInfo, TenderNotice.class);

        List<AttachmentVO> attachmentList = attachmentService.listAttachment(AttachmentTypeEnum.BIDING_NOTICE_DOC, tenderNotice.getId());
        vo.setAttachmentList(attachmentList);

        vo.setSchemeType(tenderNoticeSchemeInfo.getSchemeType());
        vo.setTenderNotice(tenderNotice);
        vo.setNoticeStatusText(TenderNoticeStatusEnum.getValueByCode(vo.getTenderNotice().getNoticeStatus()));

        TenderNoticeChangeRecord changeRecord = getNoticeTimeChange(tenderNotice.getId());
        if (!ObjectUtils.isEmpty(changeRecord)){
            vo.setBidEndTime(DateUtils.strToDate(changeRecord.getUpdateAfter(), DateUtils.YYYY_MM_DD_HH_MM));
        }
        //供应商范围TenderNoticeRange
        List<TenderNoticeRange> rangeList = tenderNoticeRangeService.list(new LambdaQueryWrapper<TenderNoticeRange>()
                .eq(TenderNoticeRange::getNoticeId, id));
        vo.setRangeList(rangeList);
        return vo;
    }

    @Override
    public TenderNoticeSchemeInfoVO getTenderNoticeSchemeInfo(Long id) {
        return baseMapper.findTenderNoticeSchemeInfo(id);
    }

    @Override
    public TenderNotice getTenderNotice(Long id) {
        return this.getById(id);
    }

    @Override
    public long getCountTenderNoticeStatus(Long id, Integer noticeStatus) {
        return this.count(new LambdaQueryWrapper<TenderNotice>()
                .eq(TenderNotice::getId, id)
                .eq(TenderNotice::getNoticeStatus, noticeStatus));
    }

    @Override
    public PageResult<VendorNoticeListVO> selectVendorNoticePage(VendorNoticePageQueryVO queryDTO) {
        IPage<VendorNoticeListVO> iPage = baseMapper.findVendorNoticePage(queryDTO.toMybatisPage(), queryDTO);
        iPage.getRecords().forEach(item -> {
            item.setNoticeStatusText(TenderNoticeStatusEnum.getValueByCode(item.getNoticeStatus()));

            item.setMinProjectName(getMinProjectName(item.getSchemeId()));
        });

        return new PageResult<>(iPage);
    }

    @Override
    public PageResult<VendorPortalNoticeListVO> selectVendorPortalNoticePage(VendorPortalNoticePageQueryVO queryDTO) {
        IPage<VendorPortalNoticeListVO> iPage = baseMapper.findVendorPortalNoticePage(queryDTO.toMybatisPage(), queryDTO);
        for (VendorPortalNoticeListVO record : iPage.getRecords()) {
            record.setMinProjectName(getMinProjectName(record.getSchemeId()));
        }
        return new PageResult<>(iPage);
    }

    @Override
    public PageResult<VendorPortalPublicityListVO> selectVendorPortalPublicityPage(VendorPortalPublicityPageQueryVO queryDTO) {
        IPage<VendorPortalPublicityListVO> iPage = baseMapper.findVendorPortalPublicityPage(queryDTO.toMybatisPage(), queryDTO);
        Date nowDate = DateUtils.getNowDate();
        for (VendorPortalPublicityListVO record : iPage.getRecords()) {
            if (nowDate.after(record.getPublicityEndTime())){
                record.setPublicityStatus(2);
            } else {
                record.setPublicityStatus(1);
            }

            record.setMinProjectName(getMinProjectName(record.getSchemeId()));
        }
        return new PageResult<>(iPage);
    }

    @Override
    public VendorPortalDataStatVO selectVendorPortalDataStat(VendorPortalDataStatQueryVO queryDTO) {
        VendorPortalDataStatVO vo = new VendorPortalDataStatVO();

        //项目个数=招标完成的个数
        //查询已完成的招标公告数据量
        /*long projectNum = this.count(new LambdaQueryWrapper<TenderNotice>()
                .eq(TenderNotice::getNoticeStatus, TenderNoticeStatusEnum.COMPLETE.getState()));*/
        String selectPrgAmount= performanceEvaluationService.selectPrgAmount();
        if (StringUtils.isNotBlank(selectPrgAmount)) {
            vo.setProjectNum(Long.valueOf(selectPrgAmount));
        }else {
            vo.setProjectNum(Long.valueOf(NumberConstant.ZERO));
        }


        //已入住供应商=注册审核通过的（被拉黑名单的也算）
        long vendorNum = vendorService.count(new LambdaQueryWrapper<Vendor>()
                .eq(Vendor::getState, VendorStateEnum.APPROVE.getState()));
        vo.setVendorNum(vendorNum);

        //累计成交额
        //需求提出（之前）：累计成交额=合同签订完成（签章完成）
        //需求提出：王敏2024/09/26
        //1.如果启用了电子签章，累计成交额：通过招投标完成电子签章的总合同金额
        //2.如果没启用电子签章，累计成交额：合同走完流程审批后的总合同金额
        List<Agreement> agreements = agreementService.list(new LambdaQueryWrapper<Agreement>()
                .eq(Agreement::getAgreementState, AgreementStateEnum.APPROVE.getState()));
        BigDecimal transactionMoney = agreements.stream().map(Agreement::getTotalAmountIncTax).reduce(BigDecimal.ZERO, BigDecimal::add);

        vo.setTransactionMoney(transactionMoney);

        return vo;
    }

    @Override
    public PageResult<TwiceBidListVO> selectTwiceBidPage(TwiceBidPageQueryVO queryDTO) {
        IPage<TwiceBidListVO> iPage = baseMapper.findTwiceBidPage(queryDTO.toMybatisPage(), queryDTO);
        return new PageResult<>(iPage);
    }

    @Override
    public boolean updateStatus(Long id, Integer noticeStatus) {
        return this.update(new LambdaUpdateWrapper<TenderNotice>()
                .set(TenderNotice::getNoticeStatus, noticeStatus)
                .eq(TenderNotice::getId, id));
    }

    @Override
    public PageResult<WinningNotifiListVO> selectWinningNotifiPage(WinningNotifiPageQueryVO queryVO) {
        IPage<WinningNotifiListVO> iPage = baseMapper.findWinningNotifiPage(queryVO.toMybatisPage(), queryVO);
        return new PageResult<>(iPage);
    }

    @Override
    public Integer nextTenderNoticeStatus(Integer schemeType, Integer noticeStatus){
        Integer nextNoticeStatus = null;
        if (schemeType == NumberConstant.ONE){
            TenderFlowPublicService publicService = new TenderFlowPublicService();
            nextNoticeStatus = publicService.nextFlow(TenderFlowPublicService.statusEnumList, noticeStatus);
        } else if (schemeType == NumberConstant.TWO){
            TenderFlowInviteService inviteService = new TenderFlowInviteService();
            nextNoticeStatus = inviteService.nextFlow(TenderFlowInviteService.statusEnumList, noticeStatus);
        } else if (schemeType == NumberConstant.THREE){
            TenderFlowEnquiryService enquiryService = new TenderFlowEnquiryService();
            nextNoticeStatus = enquiryService.nextFlow(TenderFlowEnquiryService.statusEnumList, noticeStatus);
        } else if (schemeType == NumberConstant.FOUR){
            TenderFlowSingleService singleService = new TenderFlowSingleService();
            nextNoticeStatus = singleService.nextFlow(TenderFlowSingleService.statusEnumList, noticeStatus);
        }
        return nextNoticeStatus;

    }

    /** 获取最小核算项目名称 */
    private String getMinProjectName(Long schemeId){
        List<MinProjectDataVO> minProjectDataList = procurementSchemeService.selectDataByScheme(schemeId);
        if (!CollectionUtils.isEmpty(minProjectDataList)){
            StringBuilder minProjectNameBuff = new StringBuilder();
            for (MinProjectDataVO dataVO : minProjectDataList) {
                if (!ObjectUtils.isEmpty(dataVO) && StringUtils.isNotEmpty(dataVO.getMinAccountFullName())){
                    if (minProjectNameBuff.length() == 0){
                        minProjectNameBuff.append(dataVO.getMinAccountFullName());
                    } else {
                        minProjectNameBuff.append(",").append(dataVO.getMinAccountFullName());
                    }
                }
            }
            return minProjectNameBuff.toString();
        }
        return null;
    }

    /** 验证请求参数 */
    private void verifyParam(TenderNoticeVO tenderNoticeVO){
        if (tenderNoticeVO.getSchemeType() != null){
            if (tenderNoticeVO.getSchemeType() == NumberConstant.TWO && tenderNoticeVO.getVendorIds().size() < NumberConstant.THREE){
                throw new ParamValidateException("邀请招标需推荐3家供应商以上");
            } else if (tenderNoticeVO.getSchemeType() == NumberConstant.THREE && tenderNoticeVO.getVendorIds().size() < NumberConstant.THREE){
                throw new ParamValidateException("询价采购需推荐3家供应商以上");
            } else if (tenderNoticeVO.getSchemeType() == NumberConstant.FOUR && tenderNoticeVO.getVendorIds().size() != NumberConstant.ONE){
                throw new ParamValidateException("单一来源只能推荐1家供应商");
            }
        }

        TenderNotice tenderNotice = this.getOne(new LambdaQueryWrapper<TenderNotice>()
                .eq(TenderNotice::getSchemeId, tenderNoticeVO.getSchemeId())
                .ne(TenderNotice::getNoticeStatus, TenderNoticeStatusEnum.ABANDON_BID.getState()));
        if (!ObjectUtils.isEmpty(tenderNotice)){
            throw new ParamValidateException("一个采购方案只允许发布一个招标文件");
        }

    }


    /** 获取公告最新一条时间更改记录 */
    private TenderNoticeChangeRecord getNoticeTimeChange(Long noticeId){
        return tenderNoticeChangeRecordService.getOne(new LambdaQueryWrapper<TenderNoticeChangeRecord>()
                .eq(TenderNoticeChangeRecord::getNoticeId, noticeId)
                .eq(TenderNoticeChangeRecord::getType, NumberConstant.ONE)
                .orderByDesc(TenderNoticeChangeRecord::getCreateTime)
                .last("limit 1"));
    }

}
