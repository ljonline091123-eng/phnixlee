package com.zhaocai.business.bidding.service.impl;

import cn.hutool.extra.spring.SpringUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhaocai.business.agreement.domain.Agreement;
import com.zhaocai.business.agreement.service.IAgreementService;
import com.zhaocai.business.bidding.domain.*;
import com.zhaocai.business.bidding.enums.TenderNoticeApprovalStatusEnum;
import com.zhaocai.business.bidding.enums.TenderNoticeApprovalStatusEnum;
import com.zhaocai.business.bidding.enums.TenderNoticeStatusEnum;
import com.zhaocai.business.bidding.mapper.TenderNoticeMapper;
import com.zhaocai.business.bidding.service.*;
import com.zhaocai.business.bidding.vo.req.TenderNoticeVO;
import com.zhaocai.business.bidding.vo.req.UnderlingTenderNoticeQueryVO;
import com.zhaocai.business.bidding.vo.req.query.*;
import com.zhaocai.business.bidding.vo.res.*;
import com.zhaocai.business.common.enums.AgreementStateEnum;
import com.zhaocai.business.common.enums.AttachmentTypeEnum;
import com.zhaocai.business.common.enums.VendorStateEnum;
import com.zhaocai.business.common.exception.ParamValidateException;
import com.zhaocai.business.common.sms.SmsSenderUtil;
import com.zhaocai.business.manager.http.common.config.ThirdPartyTodoFlowGroupEnum;
import com.zhaocai.business.manager.http.common.config.ThirdPartyTodoFlowModuleEnum;
import com.zhaocai.business.manager.http.dto.req.PushThirdPartyTodoTaskRequestDTO;
import com.zhaocai.business.manager.http.dto.req.PushThirdPartyTodoTaskSonRequestDTO;
import com.zhaocai.business.manager.http.dto.res.MinProjectDetailResponseDTO;
import com.zhaocai.business.manager.http.service.ContractPlanService;
import com.zhaocai.business.manager.http.service.PerformanceEvaluationService;
import com.zhaocai.business.manager.http.service.ThridPartyTodoTaskService;
import com.zhaocai.business.procurement.domain.ProcurementScheme;
import com.zhaocai.business.procurement.domain.ProcurementSchemeBidding;
import com.zhaocai.business.procurement.service.IMinProjectService;
import com.zhaocai.business.procurement.service.IProcurementSchemeBiddingService;
import com.zhaocai.business.procurement.service.IProcurementSchemeService;
import com.zhaocai.business.procurement.vo.req.BiddingSchemeListQueryVO;
import com.zhaocai.business.procurement.vo.req.ContractPlanningQueryVO;
import com.zhaocai.business.procurement.vo.res.BiddingSchemeListVO;
import com.zhaocai.business.procurement.vo.res.MinProjectDataVO;
import com.zhaocai.business.procurement.vo.res.MinProjectVO;
import com.zhaocai.business.pub.domain.Attachment;
import com.zhaocai.business.pub.service.IAttachmentService;
import com.zhaocai.business.pub.vo.req.AttachmentRequestVO;
import com.zhaocai.business.pub.vo.res.AttachmentVO;
import com.zhaocai.business.vendor.domain.Vendor;
import com.zhaocai.business.vendor.service.IVendorContactService;
import com.zhaocai.business.vendor.service.IVendorService;
import com.zhaocai.business.vendor.vo.req.VendorManagementListQueryDataVO;
import com.zhaocai.business.vendor.vo.res.VendorMainContactVO;
import com.zhaocai.business.vendor.vo.res.VendorManagementListDataVO;
import com.zhaocai.common.core.bean.PageResult;
import com.zhaocai.common.core.constant.HttpStatus;
import com.zhaocai.common.core.constant.NumberConstant;
import com.zhaocai.common.core.constant.SecurityConstants;
import com.zhaocai.common.core.domain.R;
import com.zhaocai.common.core.exception.CheckedException;
import com.zhaocai.common.core.utils.DateUtils;
import com.zhaocai.common.core.utils.bean.BeanCopierUtil;
import com.zhaocai.common.security.utils.SecurityUtils;
import com.zhaocai.system.api.domain.SysDept;
import com.zhaocai.system.api.domain.SysUser;
import com.zhaocai.system.api.system.RemoteSystemService;
import com.zhaocai.system.api.system.RemoteUserService;
import io.swagger.annotations.ApiModelProperty;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 招标公告Service业务层处理
 *
 * @author WH
 * @date 2024-05-24
 */
@Slf4j
@Service
public class TenderNoticeServiceImpl extends ServiceImpl<TenderNoticeMapper,TenderNotice> implements ITenderNoticeService {

    @Autowired
    private ITenderNoticeRangeService tenderNoticeRangeService;

    @Override
    public Map<String, Integer> numNotice(VendorNoticePageQueryVO queryDTO) {
        return null;
    }

    @Autowired
    private ITenderNoticeChangeRecordService tenderNoticeChangeRecordService;
    @Autowired
    private IAttachmentService attachmentService;
    @Autowired
    private IBiddingResultService biddingResultService;
    @Autowired
    private IVendorContactService vendorContactService;
    @Autowired
    @Lazy
    private IVendorService vendorService;
    @Autowired
    private IProcurementSchemeService procurementSchemeService;
    @Autowired
    private ContractPlanService contractPlanService;
    @Autowired
    private RemoteSystemService remoteSystemService;

    @Autowired
    private ThridPartyTodoTaskService thridPartyTodoTaskService;
    @Lazy
    @Autowired
    private IBiddingOpenPeopleService biddingOpenPeopleService;

    @Autowired
    private PerformanceEvaluationService performanceEvaluationService;
    @Autowired
    private ITenderApplyService tenderApplyService;

    @Autowired
    private IAgreementService agreementService;
    @Autowired
    @Lazy
    private IBiddingInfoService biddingInfoService;
    @Autowired
    @Lazy
    private IProcurementSchemeBiddingService procurementSchemeBiddingService;
    @Autowired
    private RemoteUserService remoteuserservice;


    @Autowired
    private IMinProjectService minProjectService;
    @Autowired
    private SmsSenderUtil smsSenderUtil = SpringUtil.getBean(SmsSenderUtil.class);

    @Override
    public PageResult<TenderNoticeListVO> page(TenderNoticeQueryVO queryDTO) {
        IPage<TenderNoticeListVO> iPage = baseMapper.page(queryDTO.toMybatisPage(), queryDTO);
        return new PageResult<>(iPage);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED,rollbackFor = Exception.class)
    public boolean addNotice(TenderNoticeVO tenderNoticeVO) {
        /* 数据验证 */
        TenderNotice tenderNoticeVerify = this.getOne(new LambdaQueryWrapper<TenderNotice>()
                .eq(TenderNotice::getSchemeId, tenderNoticeVO.getSchemeId())
                .ne(TenderNotice::getNoticeStatus, TenderNoticeStatusEnum.ABANDON_BID.getState()));
        if (!ObjectUtils.isEmpty(tenderNoticeVerify)){
            throw new ParamValidateException("一个采购方案只允许发布一个招标文件");
        }
        TenderNotice tenderNotice = BeanCopierUtil.copyBean(tenderNoticeVO, TenderNotice.class);


        /* 供应商范围 */
        List<Long> vendorIds = tenderNoticeVO.getVendorIds();
        /* 采购方案类型（1公开招标) */
        if (tenderNoticeVO.getSchemeType() != null && 1 == tenderNoticeVO.getSchemeType()){
            //如果是公开招标，那就获取到查询供应商范围的条件，由程序来获取条件内的供应商信息
            VendorManagementListQueryDataVO vendorQueryParam = new VendorManagementListQueryDataVO();
            if (!ObjectUtils.isEmpty(tenderNoticeVO.getVendorQueryParam())){
                /* 供应商查询 参数 */
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
            /* 不设置 */
            tenderNotice.setVendorRange(NumberConstant.ZERO);
        } else {
            tenderNotice.setVendorRange(NumberConstant.ONE);
            flag = true;
        }

        /* 公告状态 */
        tenderNotice.setNoticeStatus(TenderNoticeStatusEnum.TENDER_NOTICE.getState());
        tenderNotice.setState(TenderNoticeApprovalStatusEnum.DRAFT.getState());
        //保存招标公告信息
        boolean res = this.save(tenderNotice);

        //保存招标公告文件附件
//        attachmentService.addAttachment(tenderNoticeVO.getBiddingDocAttachList(), AttachmentTypeEnum.BIDING_NOTICE_MSG_DOC,tenderNotice.getId());

        //保存供应商范围表
        if (flag) {
            //设置范围类型（设置供应商范围|推荐供应商）
            Integer rangeType = NumberConstant.ZERO;
            /* 采购方案类型 1公开招标 */
            if (tenderNoticeVO.getSchemeType() != null && 1 != tenderNoticeVO.getSchemeType()) {
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
                if (!ObjectUtils.isEmpty(contactVO)) {
                    phoneList.add(contactVO.getContactPhone());
                }
            }
            tenderNoticeRangeService.saveBatch(ranges, ranges.size());
        }
        return res;
    }


    /* 保存已通过的供应商报名 */
    @Override
    @Transactional(propagation = Propagation.REQUIRED,rollbackFor = Exception.class)
    public boolean registerStatus(TenderNoticeVO tenderNoticeVO) {
        verifyParam(tenderNoticeVO);
        long count = tenderApplyService.count(new LambdaUpdateWrapper<TenderApply>()
                .eq(TenderApply::getNoticeId, tenderNoticeVO.getId()));
        if (count < NumberConstant.THREE) {
            throw new ParamValidateException("公开招标需供应商报名3家及以上");
        }

        TenderNotice tenderNoticeVerify = this.getOne(new LambdaQueryWrapper<TenderNotice>()
                .eq(TenderNotice::getId, tenderNoticeVO.getId())
                .ne(TenderNotice::getNoticeStatus, TenderNoticeStatusEnum.ABANDON_BID.getState()));

        /* 报名情况环节 设置 审批通过的 供应商列表 */
        if (!ObjectUtils.isEmpty(tenderNoticeVerify) && tenderNoticeVerify.getNoticeStatus().equals(TenderNoticeStatusEnum.TENDER_REGISTER.getState())){
            List<Long> vendorApplyIds = tenderNoticeVO.getVendorApplyIds();
//            if(vendorApplyIds!=null && !vendorApplyIds.isEmpty()){
//                /* 全部设置不通过 */
//                tenderApplyService.update(new LambdaUpdateWrapper<TenderApply>()
//                        .set(TenderApply::getApproveResult, NumberConstant.ZERO)
//                        .eq(TenderApply::getNoticeId, tenderNoticeVerify.getId()));
//                /* 设置通过 */
//                for (Long vendorId : vendorApplyIds) {
//                    tenderApplyService.update(new LambdaUpdateWrapper<TenderApply>()
//                            .set(TenderApply::getApproveResult, NumberConstant.ONE)
//                            .eq(TenderApply::getVendorId, vendorId)
//                            .eq(TenderApply::getNoticeId, tenderNoticeVerify.getId()));
//                }
//            }else {
//                throw new ParamValidateException("至少需要选中一家已报名的供应商");
//            }

            /* 全部设置通过 */
            tenderApplyService.update(new LambdaUpdateWrapper<TenderApply>()
                    .set(TenderApply::getApproveResult, NumberConstant.ONE)
                    .eq(TenderApply::getNoticeId, tenderNoticeVerify.getId()));

            TenderNoticeSchemeInfoVO detailVO = getTenderNoticeSchemeInfo(tenderNoticeVerify.getId());
            /* 根据招标公告流程状态 和 采购方案确定下一步流程 */
            Integer nextNoticeStatus = nextTenderNoticeStatus(detailVO.getSchemeType(), tenderNoticeVerify.getNoticeStatus());
            /* 报名保存 状态：报名截至 */
            return updateStatus(tenderNoticeVerify.getId(), nextNoticeStatus);
        }
        return false;
    }

    /* 采购方案 和 招标公告(携带报名情况之前填的数据) 进来的招标文件，直接进入投标环节 */
    @Override
    @Transactional(propagation = Propagation.REQUIRED,rollbackFor = Exception.class)
    public boolean add(TenderNoticeVO tenderNoticeVO) {
        verifyParam(tenderNoticeVO);
        TenderNotice tenderNotice = BeanCopierUtil.copyBean(tenderNoticeVO, TenderNotice.class);

        /* 公开招标流程 如果不用之前的公告报名情况的id校验，旧数据就不会影响 */
        TenderNotice tenderNoticeVerify = this.getOne(new LambdaQueryWrapper<TenderNotice>()
                .eq(TenderNotice::getSchemeId, tenderNoticeVO.getSchemeId())
                .ne(TenderNotice::getNoticeStatus, TenderNoticeStatusEnum.ABANDON_BID.getState()));
        /* 公开招标使用 公告的数据和报名情况的数据。 */
        if(tenderNotice.getId()==null && tenderNoticeVerify!=null && tenderNoticeVerify.getId()!=null){
            tenderNotice.setId(tenderNoticeVerify.getId());
        }
        /* 公开招标使用 公告的数据和报名情况的数据。 */
        ProcurementScheme procurementScheme = procurementSchemeService.getById(tenderNoticeVO.getSchemeId());
        if(procurementScheme!=null && procurementScheme.getProcurementType().equals(NumberConstant.ONE) && tenderNoticeVerify!=null && tenderNoticeVerify.getId()!=null){
            tenderNotice.setContactNotice(tenderNoticeVerify.getContactNotice());
            tenderNotice.setPhoneNotice(tenderNoticeVerify.getPhoneNotice());
            tenderNotice.setEmailNotice(tenderNoticeVerify.getEmailNotice());
            tenderNotice.setApplyTimeNotice(tenderNoticeVerify.getApplyTimeNotice());
            tenderNotice.setAttachIdNotice(tenderNoticeVerify.getAttachIdNotice());
            tenderNotice.setVendorRange(tenderNoticeVerify.getVendorRange());
        }
        /* 设置第一次的二次报价时间也是投标截至时间 */
        tenderNotice.setTwiceTime(tenderNoticeVO.getApplyTime());
        tenderNotice.setTwiceQuotVersion(NumberConstant.ONE);/* 第一次的二次报价版本号，后面累加上去 */
        tenderNotice.setTwiceQuotState(NumberConstant.ONE);/* 开放报价 */
        tenderNotice.setPaymentType(tenderNoticeVO.getPaymentType());/* 付款方式 */


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
        boolean res = this.saveOrUpdate(tenderNotice);

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
        //如果第一次发布version=1，且选择了是否收取保证金 receive=1为收取，则推送相关财务确认人员信息
       System.out.println("是否保证金:"+procurementScheme.getIsReceiveDeposit());
        System.out.println("版本:"+tenderNotice.getTwiceQuotVersion());
        if(procurementScheme.getIsReceiveDeposit()!=null
                &&procurementScheme.getIsReceiveDeposit()==1
                &&tenderNotice.getTwiceQuotVersion()!=null
                &&tenderNotice.getTwiceQuotVersion() == 1){
            //调第三方接口，生成开标人员的待办信息
            try {
                dealOpenPeopleTodoTask(procurementScheme, tenderNotice);
            }catch (Exception e){
                log.error(e.toString());
            }


        }
        return res;
    }

    /** 更新采购方案 采购方式 更新招标文件对象 , 传值了就修改，没传值不修改。 */
    private void updateSchemeBidding(TenderNoticeVO tenderNoticeVO){
        if(tenderNoticeVO!=null && tenderNoticeVO.getProcurementScheme()!=null && tenderNoticeVO.getProcurementScheme().getProcurementType()!=null){
            /* 更新采购方案 采购方式 */
            procurementSchemeService.update(new LambdaUpdateWrapper<ProcurementScheme>()
                    .set(ProcurementScheme::getProcurementType,tenderNoticeVO.getProcurementScheme().getProcurementType())
                    .eq(ProcurementScheme::getId,tenderNoticeVO.getSchemeId()));
        }
        if(tenderNoticeVO!=null && tenderNoticeVO.getProcurementSchemeBidding()!=null){
            /* 更新招标文件对象 */
            procurementSchemeBiddingService.update(new LambdaUpdateWrapper<ProcurementSchemeBidding>()
                    .set(ProcurementSchemeBidding::getEvaluationTemplateId,tenderNoticeVO.getProcurementSchemeBidding().getEvaluationTemplateId())/* 评分模板id */
                    .set(ProcurementSchemeBidding::getBiddingTemplateId,tenderNoticeVO.getProcurementSchemeBidding().getBiddingTemplateId())/* 招标文件模板id */
                    .set(ProcurementSchemeBidding::getContractTemplateId,tenderNoticeVO.getProcurementSchemeBidding().getContractTemplateId())/* 合同模板id */
                    .set(ProcurementSchemeBidding::getBiddingAttachmentId,tenderNoticeVO.getProcurementSchemeBidding().getBiddingAttachmentId())/* 招标文件附件id */
                    .eq(ProcurementSchemeBidding::getSchemeId,tenderNoticeVO.getSchemeId()));
            attachmentService.updateBusiness(tenderNoticeVO.getProcurementSchemeBidding().getBiddingAttachmentId(), AttachmentTypeEnum.SCHEME_BIDDING,tenderNoticeVO.getSchemeId());
        }

    }

    private void dealOpenPeopleTodoTask(ProcurementScheme procurementScheme, TenderNotice tenderNotice) {
        PushThirdPartyTodoTaskRequestDTO parentRequestDTO = new PushThirdPartyTodoTaskRequestDTO();
        List<PushThirdPartyTodoTaskSonRequestDTO> messageList = new ArrayList<>();
        PushThirdPartyTodoTaskSonRequestDTO requestDTO = new PushThirdPartyTodoTaskSonRequestDTO();
        MinProjectVO project = minProjectService.getMinProjectByMinAccountCode(procurementScheme.getProjectCode());
        String label = "";
        switch (procurementScheme.getProcurementType()){
             case 1:
                 label = "公开招标";
             case 2:
                 label = "邀请招标";
             case 3:
                 label = "询价";
             case 4:
                 label = "单一来源";
             default:
        }
        requestDTO.setTitle("财务人员待办信息");

        String xm = (procurementScheme==null?"":procurementScheme.getFinanceConfirmName()==null?"":procurementScheme.getFinanceConfirmName())
            + "你好!"
            + (project==null?"":project.getMinAccountFullName()==null?"":project.getMinAccountFullName())
            + "项目的"
            + (procurementScheme==null?"":procurementScheme.getProcurementSchemeName()==null?"":procurementScheme.getProcurementSchemeName())
            + "、编号为"
            + (procurementScheme==null?"":procurementScheme.getProcurementSchemeCode()==null?"":procurementScheme.getProcurementSchemeCode()) +
            "、招标方式为"
            + label
            + "于"
            + (tenderNotice==null?"":tenderNotice.getCreateTime()==null?"":this.formatDate(tenderNotice.getCreateTime()))
            + "发布了招标文件、开启了招标工作，需要收取投标保证金。请您及时关注投标人是否按时缴纳保证金。";

        log.info("[财务人员待办信息][xm] {}",xm);
        requestDTO.setContent(xm);
        requestDTO.setPrjName((project==null?"":project.getMinAccountSimpleName()==null?"":project.getMinAccountSimpleName()));
        requestDTO.setArrivalTime(formatDate(new Date()));
        requestDTO.setCreateTime(formatDate(new Date()));
        String thridUserId = SecurityUtils.getThridUserId();
        requestDTO.setMsgFromPerCode(StringUtils.isNotEmpty(thridUserId) ? Long.parseLong(thridUserId) : null);
        requestDTO.setMsgFromPerName(SecurityUtils.getLoginUserNickName());
        String findThirdUserId = findThirdUserId(procurementScheme.getFinanceConfirmId()==null?null:Long.valueOf(procurementScheme.getFinanceConfirmId()));
        requestDTO.setMsgToPerCode(StringUtils.isNotEmpty(findThirdUserId) ? Long.parseLong(findThirdUserId) : null);
        requestDTO.setMsgToPerName((procurementScheme==null?"":procurementScheme.getFinanceConfirmName()==null?"":procurementScheme.getFinanceConfirmName()));
        requestDTO.setFlowGroup(ThirdPartyTodoFlowGroupEnum.XCW_BID.getDesc());
        requestDTO.setFlowModule(ThirdPartyTodoFlowModuleEnum.BID_MANAGE.getDesc());
        requestDTO.setFlowName((procurementScheme==null?"":procurementScheme.getFinanceConfirmName()==null?"":procurementScheme.getFinanceConfirmName()) + "的" + ThirdPartyTodoFlowGroupEnum.XCW_BID.getDesc());
        /* 先跳转到列表，下面不报错再跳转覆盖 */
        requestDTO.setDetailUrl("/procurement/bindding");

        String base64Encoded = "";
        if(tenderNotice!=null && tenderNotice.getId()!=null){
            BiddingSchemeListQueryVO query = new BiddingSchemeListQueryVO();
            query.setPageNumber(1);
            query.setPageSize(1);
            query.setNoticeId(tenderNotice.getId());
            IPage<BiddingSchemeListVO> iPage = procurementSchemeService.selectBiddingSchemePageList(query);
            if(iPage.getTotal()>0){
                BiddingSchemeListVO biddingSchemeListVO = iPage.getRecords().get(0);
                // 将数据转换为 JSON 字符串
                ObjectMapper objectMapper = new ObjectMapper();
                /* 过滤空属性json生成 */
                objectMapper.setSerializationInclusion(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL);
                try{
                    String jsonString = objectMapper.writeValueAsString(biddingSchemeListVO);
                    // 使用 Base64 编码 JSON 字符串
                    base64Encoded = Base64.getEncoder().encodeToString(jsonString.getBytes("UTF-8"));
                    // 模仿 encodeURIComponent
                    base64Encoded = URLEncoder.encode(base64Encoded, "UTF-8");
                    /* 精准定位跳转到该条招标对象 */
                    requestDTO.setDetailUrl("/procurement/tendering/"+base64Encoded);
                    log.info("[财务人员待办信息][Base64编码转换] {} ", base64Encoded);
                }catch (JsonProcessingException | UnsupportedEncodingException e){
                    log.info("[财务人员待办信息][Base64编码转换 ERROR ] {} ",e.getMessage());
                }
            }
        }

        //            requestDTO.setDetailUrl("/procurement/plan-detail/IjE4MTkyODk4NDM3Njk0NzA5Nzgi");
        //            requestDTO.setUserObj("{\\\"id\\\":1111}");
        //            requestDTO.setUserObj(openPeople.toString());
        //推送消息类型 1工作通知
        requestDTO.setType(NumberConstant.ONE);
        //推送公司类型 2晟晟
        requestDTO.setCompanyType(NumberConstant.THREE);
        messageList.add(requestDTO);
        log.info("[财务人员待办信息][messageList] {}",messageList);
        parentRequestDTO.setMessageList(messageList);
        parentRequestDTO.setAuthorization(SecurityUtils.getMasterControlToken());
        thridPartyTodoTaskService.pushTodoTask(parentRequestDTO);
        log.info("[财务人员待办信息][推送] {}",parentRequestDTO);
    }

    private String formatDate(Date date){
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
        return sdf.format(date);
    }

    private String findThirdUserId(Long userId){
        R<SysUser> sysUser = remoteuserservice.selectUserInFoById(userId, SecurityConstants.INNER);
        if(sysUser.getCode() == HttpStatus.ERROR){
            throw new CheckedException("获取用户信息失败");
        }
        if (null != sysUser.getData()){
            return sysUser.getData().getThridUserId();
        }
        return null;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean aNewAdd(TenderNoticeVO tenderNoticeVO) {
        Integer schemeType = tenderNoticeVO.getSchemeType();
        verifyNewAddParam(tenderNoticeVO);
        //删除旧招标公告数据
        this.remove(new LambdaUpdateWrapper<TenderNotice>()
                .eq(TenderNotice::getSchemeId, tenderNoticeVO.getSchemeId()));

        /* 更新采购方案 采购方式 更新招标文件对象 */
        updateSchemeBidding(tenderNoticeVO);

        if(schemeType == NumberConstant.ONE)
            return this.addNotice(tenderNoticeVO);
        else
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

    /** 获取投标单详情信息 */
    @Override
    public TenderNoticeDetailVO getInfo(Long schemeId, Long noticeId) {
        TenderNoticeDetailVO vo = new TenderNoticeDetailVO();
        /* 获取公告对象 */
        TenderNotice tenderNotice = this.getOne(new LambdaQueryWrapper<TenderNotice>()
                .eq(null != noticeId, TenderNotice::getId, noticeId)
                .eq(TenderNotice::getSchemeId, schemeId));
        if (ObjectUtils.isEmpty(tenderNotice)){
            return vo;
        }

        vo.setTenderNotice(tenderNotice);
        /* 获取 招标公告 对应 状态 */
        vo.setNoticeStatusText(TenderNoticeStatusEnum.getValueByCode(vo.getTenderNotice().getNoticeStatus()));

        /* 招标公告变更记录对象 */
        TenderNoticeChangeRecord changeRecord = tenderNoticeChangeRecordService.getOne(
                new LambdaQueryWrapper<TenderNoticeChangeRecord>()
                .and(q -> q.eq(TenderNoticeChangeRecord::getNoticeStatus, null)
                    .or().eq(TenderNoticeChangeRecord::getNoticeStatus, TenderNoticeStatusEnum.TENDER_ISSUE.getState()))
                .eq(TenderNoticeChangeRecord::getNoticeId, tenderNotice.getId())
                .eq(TenderNoticeChangeRecord::getType, NumberConstant.ONE)
                .orderByDesc(TenderNoticeChangeRecord::getCreateTime).last("limit 1"));
        if (!ObjectUtils.isEmpty(changeRecord)){
            /* 更新 投标截止时间 */
            vo.setBidEndTime(DateUtils.strToDate(changeRecord.getUpdateAfter(), DateUtils.YYYY_MM_DD_HH_MM_SS));
        }
        /* 招标公告变更记录对象 */
        TenderNoticeChangeRecord changeRecordNotice = tenderNoticeChangeRecordService.getOne(
                new LambdaQueryWrapper<TenderNoticeChangeRecord>()
                        .eq(TenderNoticeChangeRecord::getNoticeStatus, TenderNoticeStatusEnum.TENDER_NOTICE.getState())
                        .eq(TenderNoticeChangeRecord::getNoticeId, tenderNotice.getId())
                        .eq(TenderNoticeChangeRecord::getType, NumberConstant.ONE)
                        .orderByDesc(TenderNoticeChangeRecord::getCreateTime).last("limit 1"));
        if (!ObjectUtils.isEmpty(changeRecordNotice)){
            /* 更新 报名截止时间 */
            vo.setApplyTimeNotice(DateUtils.strToDate(changeRecordNotice.getUpdateAfter(), DateUtils.YYYY_MM_DD_HH_MM_SS));
            vo.getTenderNotice().setApplyTimeNotice(DateUtils.strToDate(changeRecordNotice.getUpdateAfter(), DateUtils.YYYY_MM_DD_HH_MM_SS));
        }
        /* 获取 招标文件附件信息 */
        List<AttachmentVO> attachmentList = attachmentService.listAttachment(AttachmentTypeEnum.BIDING_NOTICE_DOC, tenderNotice.getId());
        vo.setAttachmentList(attachmentList);

        /* 招标文件公告附件 */
        Attachment attachmentNoticeData = attachmentService.getById(vo.getTenderNotice().getAttachIdNotice());
        if (attachmentNoticeData!=null) {
            AttachmentVO attachmentNotice = BeanCopierUtil.copyBean(attachmentNoticeData, AttachmentVO.class);
            vo.setAttachmentNotice(attachmentNotice);
        }

        /* 获取 定标附件 */
        List<AttachmentVO> calibrationAttachmentList = attachmentService.listAttachment(AttachmentTypeEnum.CALIBRATION_DOCUMENT, tenderNotice.getId());
        vo.setCalibrationAttachmentList(calibrationAttachmentList);

        /* 获取供应商报名列表 */
        List<TenderApply> tenderApplyList = tenderApplyService.list(new LambdaQueryWrapper<TenderApply>()
                .eq(TenderApply::getNoticeId, noticeId));
        if (!ObjectUtils.isEmpty(tenderApplyList)) {
            vo.setTenderApplyList(tenderApplyList);
        }

        //* 获取供应商范围报名列表 *//*
        List<TenderNoticeRange> rangeList = tenderNoticeRangeService.list(new LambdaQueryWrapper<TenderNoticeRange>()
                .eq(TenderNoticeRange::getNoticeId, noticeId));
        for (TenderNoticeRange range : rangeList) {
            Vendor vendor = vendorService.getById(range.getVendorId());
            if(vendor!=null){
                range.setVendorName(vendor.getEnterpriseName());
            }
        }
        if (!ObjectUtils.isEmpty(rangeList)) {
            vo.setRangeList(rangeList);
        }
        /* 设置 人员角色状态  */
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
        /* 是否为采购经办人 */
        vo.setPurchaseOfficer(purchaseOfficerVal);
        /* 是否为财务确认人员 */
        vo.setFinanceConfirmUser(confirmUserVal);
        /* 是否为开标待办人员 */
        vo.setOpenTodoUser(openTodoUserVal);
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

        /* 采购方案类型（采购计划类别（1购买材料 2租赁材料 3租赁机械（设备） 4专业分包 5劳务分包 6其他）） */
        vo.setProcurementPlanType(tenderNoticeSchemeInfo.getProcurementPlanType());
        /* 采购方案类型（1公开招标 2邀请招标 3询价采购 4单一来源） */
        vo.setSchemeType(tenderNoticeSchemeInfo.getSchemeType());
        /* 招标公告变更记录对象 */
        TenderNoticeChangeRecord changeRecordNotice = tenderNoticeChangeRecordService.getOne(
                new LambdaQueryWrapper<TenderNoticeChangeRecord>()
                        .eq(TenderNoticeChangeRecord::getNoticeStatus, TenderNoticeStatusEnum.TENDER_NOTICE.getState())
                        .eq(TenderNoticeChangeRecord::getNoticeId, id)
                        .eq(TenderNoticeChangeRecord::getType, NumberConstant.ONE)
                        .orderByDesc(TenderNoticeChangeRecord::getCreateTime).last("limit 1"));
        if (!ObjectUtils.isEmpty(changeRecordNotice)){
            /* 更新 报名截止时间 */
            tenderNotice.setApplyTimeNotice(DateUtils.strToDate(changeRecordNotice.getUpdateAfter(), DateUtils.YYYY_MM_DD_HH_MM_SS));
        }

        /* 招标公告附件 */
        Attachment attachmentNoticeData = attachmentService.getOne(new LambdaQueryWrapper<Attachment>()
                .eq(Attachment::getId, tenderNotice.getAttachIdNotice()));
        if (attachmentNoticeData!=null) {
            AttachmentVO attachmentNotice = BeanCopierUtil.copyBean(attachmentNoticeData, AttachmentVO.class);
            vo.setAttachmentNotice(attachmentNotice);
        }

        /* 招标文件附件 */
        List<AttachmentVO> attachmentList = attachmentService.listAttachment(AttachmentTypeEnum.BIDING_NOTICE_DOC, tenderNotice.getId());
        vo.setAttachmentList(attachmentList);

        TenderNoticeChangeRecord changeRecord = getNoticeTimeChange(tenderNotice.getId());
        if (!ObjectUtils.isEmpty(changeRecord)){
            vo.setBidEndTime(DateUtils.strToDate(changeRecord.getUpdateAfter(), DateUtils.YYYY_MM_DD_HH_MM));
            /* 更新 投标截止时间 */
            tenderNotice.setApplyTime(DateUtils.strToDate(changeRecord.getUpdateAfter(), DateUtils.YYYY_MM_DD_HH_MM_SS));
        }
        /* 招标对象 */
        vo.setTenderNotice(tenderNotice);
        vo.setNoticeStatusText(TenderNoticeStatusEnum.getValueByCode(vo.getTenderNotice().getNoticeStatus()));

        //供应商范围TenderNoticeRange
        List<TenderNoticeRange> rangeList = tenderNoticeRangeService.list(new LambdaQueryWrapper<TenderNoticeRange>()
                .eq(TenderNoticeRange::getNoticeId, id));
        vo.setRangeList(rangeList);

        Vendor vendor = vendorService.getByLoginUser(SecurityUtils.getUserId());
        long openCount = tenderApplyService.count(new LambdaQueryWrapper<TenderApply>()
                .eq(TenderApply::getVendorId, vendor.getId())
                .eq(TenderApply::getNoticeId, tenderNotice.getId()));
        if(openCount > 0){
            vo.setApplyStatus("已报名");
        }else {
            vo.setApplyStatus("未报名");
        }
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
    public PageResult<VendorNoticeListVO> selectVendorNoticePageNotice(VendorNoticePageQueryVO queryDTO) {
        IPage<VendorNoticeListVO> iPage = baseMapper.findVendorNoticePageNotice(queryDTO.toMybatisPage(), queryDTO);
        iPage.getRecords().forEach(item -> {
            item.setNoticeStatusText(TenderNoticeStatusEnum.getValueByCode(item.getNoticeStatus()));

            /* 获取 {项目简称（最小核算项目名称）} */
            item.setMinProjectName(getMinProjectName(item.getSchemeId()));
            /* 招标公告变更记录对象 */
            TenderNoticeChangeRecord changeRecordNotice = tenderNoticeChangeRecordService.getOne(
                    new LambdaQueryWrapper<TenderNoticeChangeRecord>()
                            .eq(TenderNoticeChangeRecord::getNoticeStatus, TenderNoticeStatusEnum.TENDER_NOTICE.getState())
                            .eq(TenderNoticeChangeRecord::getNoticeId, item.getNoticeId())
                            .eq(TenderNoticeChangeRecord::getType, NumberConstant.ONE)
                            .orderByDesc(TenderNoticeChangeRecord::getCreateTime).last("limit 1"));
            if (!ObjectUtils.isEmpty(changeRecordNotice)){
                /* 更新 报名截止时间 */
                item.setApplyTimeNotice(DateUtils.strToDate(changeRecordNotice.getUpdateAfter(), DateUtils.YYYY_MM_DD_HH_MM_SS));
            }

            /* 招标公告附件 */
            Attachment attachmentNoticeData = attachmentService.getById(item.getAttachIdNotice());
            if (attachmentNoticeData!=null) {
                AttachmentVO attachmentNotice = BeanCopierUtil.copyBean(attachmentNoticeData, AttachmentVO.class);
                item.setAttachmentNotice(attachmentNotice);
            }

            /* 不使用采购人的招标单位信息，使用采购方案对应项目的的招标单位信息 */
            item.setUnit(getDeptName(item.getSchemeId()));
        });

        return new PageResult<>(iPage);
    }

    @Override
    public PageResult<VendorNoticeListVO> selectVendorNoticePage(VendorNoticePageQueryVO queryDTO) {
        IPage<VendorNoticeListVO> iPage = baseMapper.findVendorNoticePage(queryDTO.toMybatisPage(), queryDTO);
        Vendor vendor = vendorService.getByLoginUser(SecurityUtils.getUserId());
        iPage.getRecords().forEach(item -> {
            item.setNoticeStatusText(TenderNoticeStatusEnum.getValueByCode(item.getNoticeStatus()));


            BiddingInfo biddingInfo = biddingInfoService.getOne(new LambdaQueryWrapper<BiddingInfo>()
                    .eq(BiddingInfo::getNoticeId, item.getNoticeId())
                    .eq(BiddingInfo::getVendorId, vendor.getId())
                    .orderByDesc(BiddingInfo::getTwiceQuotVersion).last("limit 1"));
            if(biddingInfo!=null){
                item.setBiddingInfoId(biddingInfo.getId());
                item.setTwiceQuot(biddingInfo.getTwiceQuot());
                item.setBidStatus("已投标");
            }else{
                item.setBidStatus("未投标");
            }

            /* 招标公告变更记录对象 */
            TenderNoticeChangeRecord changeRecord = tenderNoticeChangeRecordService.getOne(
                    new LambdaQueryWrapper<TenderNoticeChangeRecord>()
                            .and(q -> q.eq(TenderNoticeChangeRecord::getNoticeStatus, null)
                                    .or().eq(TenderNoticeChangeRecord::getNoticeStatus, TenderNoticeStatusEnum.TENDER_ISSUE.getState()))
                            .eq(TenderNoticeChangeRecord::getNoticeId, item.getNoticeId())
                            .eq(TenderNoticeChangeRecord::getType, NumberConstant.ONE)
                            .orderByDesc(TenderNoticeChangeRecord::getCreateTime).last("limit 1"));
            if (!ObjectUtils.isEmpty(changeRecord)){
                /* 更新 投标截止时间 */
                item.setApplyTime(DateUtils.strToDate(changeRecord.getUpdateAfter(), DateUtils.YYYY_MM_DD_HH_MM_SS));
            }
            /* 招标公告变更记录对象 */
            TenderNoticeChangeRecord changeRecordNotice = tenderNoticeChangeRecordService.getOne(
                    new LambdaQueryWrapper<TenderNoticeChangeRecord>()
                            .eq(TenderNoticeChangeRecord::getNoticeStatus, TenderNoticeStatusEnum.TENDER_NOTICE.getState())
                            .eq(TenderNoticeChangeRecord::getNoticeId, item.getNoticeId())
                            .eq(TenderNoticeChangeRecord::getType, NumberConstant.ONE)
                            .orderByDesc(TenderNoticeChangeRecord::getCreateTime).last("limit 1"));
            if (!ObjectUtils.isEmpty(changeRecordNotice)){
                /* 更新 报名截止时间 */
                item.setApplyTimeNotice(DateUtils.strToDate(changeRecordNotice.getUpdateAfter(), DateUtils.YYYY_MM_DD_HH_MM_SS));
            }

            /* 招标公告附件 */
            Attachment attachmentNoticeData = attachmentService.getById(item.getAttachIdNotice());
            if (attachmentNoticeData!=null) {
                AttachmentVO attachmentNotice = BeanCopierUtil.copyBean(attachmentNoticeData, AttachmentVO.class);
                item.setAttachmentNotice(attachmentNotice);
            }
            item.setMinProjectName(getMinProjectName(item.getSchemeId()));
        });

        return new PageResult<>(iPage);
    }

    /* 供应商首页 未登录只能查看公开招标 */
    /* 供应商首页 登录后不仅仅查看公开招标还有邀请和单一等等，是根据供应商id来查询 */
    /* 供应商 工作台首页 消息栏 列表数据 */
    @Override
    public PageResult<VendorPortalNoticeListVO> selectVendorPortalNoticePage(VendorPortalNoticePageQueryVO queryDTO) {
        IPage<VendorPortalNoticeListVO> iPage = baseMapper.findVendorPortalNoticePage(queryDTO.toMybatisPage(), queryDTO);
        for (VendorPortalNoticeListVO record : iPage.getRecords()) {
            record.setMinProjectName(getMinProjectName(record.getSchemeId()));

            /* 不使用采购人的招标单位信息，使用采购方案对应项目的的招标单位信息 */
            record.setUnit(getDeptName(record.getSchemeId()));
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
        /* 四舍五入成万元 */
        transactionMoney = transactionMoney.divide(new BigDecimal(10000),2, RoundingMode.HALF_UP);
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
        if (schemeType == NumberConstant.ONE){/* 公开招标 */
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

    @Override
    public List<ContractPlanningNoticeVO> getListByContractPlanningId(ContractPlanningQueryVO queryVO) {
        return baseMapper.getListByContractPlanningId(queryVO);
    }

    @Override
    public PageResult<VendorPortalNoticeListVO> selectVendorPortalNoticePageTwo(VendorPortalNoticePageQueryVO queryDTO) {
        IPage<VendorPortalNoticeListVO> iPage = baseMapper.findVendorPortalNoticePageTwo(queryDTO.toMybatisPage(), queryDTO);
        for (VendorPortalNoticeListVO record : iPage.getRecords()) {
            record.setMinProjectName(getMinProjectName(record.getSchemeId()));
        }
        return new PageResult<>(iPage);
    }

    /**
     * 获取招标公告列表-(第三方-招标公告接口)
     * @param queryVO
     * @return
     */
    @Override
    public PageResult<TenderNoticeVO> listTenderNoticePage(UnderlingTenderNoticeQueryVO queryVO) {
        // 获取分页数据
        IPage<TenderNoticeVO> pages = baseMapper.listTenderNoticePage(queryVO.toMybatisPage(), queryVO);

        // 设置附件数据到 TenderNoticeVO 对象中
        for (TenderNoticeVO record : pages.getRecords()) {
            Long attachIdNotice = record.getAttachIdNotice();
            if (attachIdNotice != null) {
                Attachment attachmentNoticeData = attachmentService.getById(attachIdNotice);
                if (attachmentNoticeData != null) {
                    AttachmentRequestVO attachmentNotice = BeanCopierUtil.copyBean(attachmentNoticeData, AttachmentRequestVO.class);
                    record.setBiddingDocAttachList(Collections.singletonList(attachmentNotice));
                }
            }
        }

        return new PageResult<>(pages);
    }

    /** 根据采购方案往合约拆分查询最小核算项目对应的招标单位部门名称 */
    private String getDeptName(Long schemeId){
        /* 根据采购方案往合约拆分查询最小核算项目信息 */
        List<MinProjectDataVO> minProjectDataList = procurementSchemeService.selectDataByScheme(schemeId);
        if (!CollectionUtils.isEmpty(minProjectDataList)){
            /* 用现成方法的查询 */
            MinProjectDetailResponseDTO projectDetail = contractPlanService.getMinProjectDetail(minProjectDataList.get(0).getProjectCode());
            if(projectDetail!=null){
                if (StringUtils.isNotBlank(projectDetail.getManagementOrgId())) {
                    /* 获取部门信息 */
                    SysDept sysDept = remoteSystemService.getByThridDeptId(projectDetail.getManagementOrgId(), SecurityConstants.INNER);
                    return Optional.ofNullable(sysDept)
                            .map(SysDept::getDeptName)
                            .orElse("");
                }
            }
        }
        return null;
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
        ProcurementScheme scheme = procurementSchemeService.getById(tenderNoticeVO.getSchemeId());
        /* 采购方案 1公开招标 进入到这个 招标文件环节的需要验证 */
        if (scheme.getProcurementType() == NumberConstant.ONE){
//            TenderNotice tenderNoticeVerify = this.getOne(new LambdaQueryWrapper<TenderNotice>()
//                    .eq(TenderNotice::getId, tenderNoticeVO.getId())
//                    .ne(TenderNotice::getNoticeStatus, TenderNoticeStatusEnum.ABANDON_BID.getState()));
            /* 如果不用id校验，旧数据就不会影响 */
            TenderNotice tenderNoticeVerify = this.getOne(new LambdaQueryWrapper<TenderNotice>()
                    .eq(TenderNotice::getSchemeId, tenderNoticeVO.getSchemeId())
                    .ne(TenderNotice::getNoticeStatus, TenderNoticeStatusEnum.ABANDON_BID.getState()));
            if (ObjectUtils.isEmpty(tenderNoticeVerify)){
                throw new ParamValidateException("未获取到招标文件");
            }
        }else {
            /* 直接进入到招标文件环节 */
            TenderNotice tenderNotice = this.getOne(new LambdaQueryWrapper<TenderNotice>()
                    .eq(TenderNotice::getSchemeId, tenderNoticeVO.getSchemeId())
                    .ne(TenderNotice::getNoticeStatus, TenderNoticeStatusEnum.ABANDON_BID.getState()));
            if (!ObjectUtils.isEmpty(tenderNotice)){
                throw new ParamValidateException("一个采购方案只允许发布一个招标文件");
            }
        }
    }


    /** 获取公告文件最新一条时间更改记录 */
    private TenderNoticeChangeRecord getNoticeTimeChange(Long noticeId){
        return tenderNoticeChangeRecordService.getOne(new LambdaQueryWrapper<TenderNoticeChangeRecord>()
                .and(q -> q.eq(TenderNoticeChangeRecord::getNoticeStatus, null)
                        .or().eq(TenderNoticeChangeRecord::getNoticeStatus, TenderNoticeStatusEnum.TENDER_ISSUE.getState()))
                .eq(TenderNoticeChangeRecord::getNoticeId, noticeId)
                .eq(TenderNoticeChangeRecord::getType, NumberConstant.ONE)
                .orderByDesc(TenderNoticeChangeRecord::getCreateTime)
                .last("limit 1"));
    }

    /** 获取公告报名最新一条时间更改记录 */
    private TenderNoticeChangeRecord getNoticeTimeNoticeChange(Long noticeId){
        return tenderNoticeChangeRecordService.getOne(new LambdaQueryWrapper<TenderNoticeChangeRecord>()
                .eq(TenderNoticeChangeRecord::getNoticeStatus, TenderNoticeStatusEnum.TENDER_NOTICE.getState())
                .eq(TenderNoticeChangeRecord::getNoticeId, noticeId)
                .eq(TenderNoticeChangeRecord::getType, NumberConstant.ONE)
                .orderByDesc(TenderNoticeChangeRecord::getCreateTime)
                .last("limit 1"));
    }

}
