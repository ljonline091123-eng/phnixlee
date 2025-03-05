package com.zhaocai.business.bidding.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhaocai.business.bidding.domain.*;
import com.zhaocai.business.bidding.enums.BiddingInfoStatusEnum;
import com.zhaocai.business.bidding.enums.TenderNoticeStatusEnum;
import com.zhaocai.business.bidding.service.*;
import com.zhaocai.business.bidding.vo.req.BidQuotationVO;
import com.zhaocai.business.bidding.vo.req.BidVO;
import com.zhaocai.business.bidding.vo.req.query.TwiceBidPageQueryVO;
import com.zhaocai.business.bidding.vo.req.query.VendorBidPdfFileRequstVO;
import com.zhaocai.business.bidding.vo.req.query.VendorNoticePageQueryVO;
import com.zhaocai.business.bidding.vo.req.query.WinningNotifiPageQueryVO;
import com.zhaocai.business.bidding.vo.res.*;
import com.zhaocai.business.common.enums.AttachmentTypeEnum;
import com.zhaocai.business.common.enums.PriceTypeEnum;
import com.zhaocai.business.common.enums.ProcurementPlanTypeEnum;
import com.zhaocai.business.common.enums.VendorContactStateEnum;
import com.zhaocai.business.common.exception.ParamValidateException;
import com.zhaocai.business.common.utils.AmountCalUtil;
import com.zhaocai.business.manager.http.common.config.ThirdPartyTodoFlowGroupEnum;
import com.zhaocai.business.manager.http.common.config.ThirdPartyTodoFlowModuleEnum;
import com.zhaocai.business.manager.http.dto.req.PushThirdPartyTodoTaskRequestDTO;
import com.zhaocai.business.manager.http.dto.req.PushThirdPartyTodoTaskSonRequestDTO;
import com.zhaocai.business.manager.http.service.ThridPartyTodoTaskService;
import com.zhaocai.business.procurement.domain.MaterialsList;
import com.zhaocai.business.procurement.domain.ProcurementScheme;
import com.zhaocai.business.procurement.service.IMaterialsListService;
import com.zhaocai.business.procurement.service.IMinProjectService;
import com.zhaocai.business.procurement.service.IProcurementSchemeService;
import com.zhaocai.business.procurement.vo.req.BiddingSchemeListQueryVO;
import com.zhaocai.business.procurement.vo.res.*;
import com.zhaocai.business.pub.service.IAttachmentService;
import com.zhaocai.business.pub.vo.req.AttachmentRequestVO;
import com.zhaocai.business.pub.vo.res.AttachmentVO;
import com.zhaocai.business.vendor.domain.Vendor;
import com.zhaocai.business.vendor.domain.VendorContact;
import com.zhaocai.business.vendor.service.IVendorContactService;
import com.zhaocai.business.vendor.service.IVendorService;
import com.zhaocai.common.core.bean.PageResult;
import com.zhaocai.common.core.constant.HttpStatus;
import com.zhaocai.common.core.constant.NumberConstant;
import com.zhaocai.common.core.constant.SecurityConstants;
import com.zhaocai.common.core.domain.R;
import com.zhaocai.common.core.exception.CheckedException;
import com.zhaocai.common.core.utils.DateUtils;
import com.zhaocai.common.core.utils.NumberUtil;
import com.zhaocai.common.core.utils.bean.BeanCopierUtil;
import com.zhaocai.common.core.utils.ip.IpUtils;
import com.zhaocai.common.security.utils.SecurityUtils;
import com.zhaocai.system.api.domain.SysUser;
import com.zhaocai.system.api.system.RemoteUserService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author ssy
 * @date 2024/5/27 18:21
 */
@Slf4j
@Service
public class VendorBidServiceImpl implements IVendorBidService {

    @Autowired
    private ITenderNoticeService tenderNoticeService;
    @Autowired
    private IBiddingInfoService biddingInfoService;
    @Autowired
    private IBiddingListQuotationService biddingListQuotationService;
    @Autowired
    private IVendorService vendorService;
    @Autowired
    private IVendorContactService vendorContactService;
    @Autowired
    private IAttachmentService attachmentService;
    @Autowired
    private ITenderApplyService tenderApplyService;

    @Autowired
    private RemoteUserService remoteuserservice;

    @Autowired
    private IProcurementSchemeService procurementSchemeService;
    @Autowired
    private IMinProjectService minProjectService;

    @Autowired
    private ThridPartyTodoTaskService thridPartyTodoTaskService;
    @Autowired
    private IMaterialsListService materialsListService;


    /* 在线报名 招标状态为  11发布 12报名情况 的数据 */
    @Override
    public PageResult<VendorNoticeListVO> pageNotice(VendorNoticePageQueryVO queryDTO) {
//        queryDTO.setVendorId(getVendor(SecurityUtils.getUserId()).getId());
        Vendor vendor = getVendor(SecurityUtils.getUserId());
        queryDTO.setVendorId(vendor.getId());
        queryDTO.setRegisterApprovalTime(vendor.getRegisterApprovalTime());
        PageResult<VendorNoticeListVO> pageResult = tenderNoticeService.selectVendorNoticePageNotice(queryDTO);
        return pageResult;
    }

    @Override
    public Map<String, Integer> numNotice(VendorNoticePageQueryVO queryDTO) {
        Vendor vendor = getVendor(SecurityUtils.getUserId());
        queryDTO.setVendorId(vendor.getId());
        queryDTO.setRegisterApprovalTime(vendor.getRegisterApprovalTime());
        Map<String, Integer> map = tenderNoticeService.numNotice(queryDTO);
        return map;
    }

    /* 在线报名 招标状态为 非 (0废标 11发布 12报名情况) 并且在报名列表里面 的数据 */
    @Override
    public PageResult<VendorNoticeListVO> page(VendorNoticePageQueryVO queryDTO) {
//        queryDTO.setVendorId(getVendor(SecurityUtils.getUserId()).getId());
        Vendor vendor = getVendor(SecurityUtils.getUserId());
        queryDTO.setVendorId(vendor.getId());
        queryDTO.setRegisterApprovalTime(vendor.getRegisterApprovalTime());
        PageResult<VendorNoticeListVO> pageResult = tenderNoticeService.selectVendorNoticePage(queryDTO);
        return pageResult;
    }


    @Override
    public List<AttachmentVO> getVendorBidPdfFileList(VendorBidPdfFileRequstVO requstVO) throws IOException {
        //招标文件的word附件
        List<AttachmentVO> attachmentVOList = requstVO.getOldAttachmentList();
        String unit = requstVO.getUnit();
        //判断是否已经存在该招标文件的pdf附件
        List<AttachmentVO> pdfList= attachmentService.listAttachment(AttachmentTypeEnum.BIDING_NOTICE_PDF, requstVO.getNoticeId());
        if(CollectionUtils.isEmpty(pdfList)) {
            //把招标文件的word文件转换为pdf附件
            for (AttachmentVO attachmentVO : attachmentVOList) {
                String yozoPdfFileUrl = attachmentService.convertOfficeToPdf(attachmentVO.getFileName(), attachmentVO.getFileUrl(), unit);
                AttachmentRequestVO attachmentRequestVO = attachmentService.downloadYOZOFileAndUploadMINIO(attachmentVO.getFileName(), yozoPdfFileUrl);
                //把招标公告 tb_tender_notice的id,设为pdf附件的businessId；
                attachmentService.addAttachment(attachmentRequestVO, AttachmentTypeEnum.BIDING_NOTICE_PDF, requstVO.getNoticeId());
            }
        }
        /* 查询招标文件的pdf附件 */
        List<AttachmentVO> PdfAttachmentList = attachmentService.listAttachment(AttachmentTypeEnum.BIDING_NOTICE_PDF, requstVO.getNoticeId());
        return   PdfAttachmentList;
    }

    @Override
    public TenderNoticeDetailVO detail(Long noticeId) {
        TenderNoticeDetailVO detail = tenderNoticeService.detail(noticeId);
        TenderNotice tenderNotice = detail.getTenderNotice();
//        detail.setTwiceTime(tenderNotice.getApplyTime());
//        detail.setTwiceQuot(tenderNotice.getTwiceQuotState());
        Long biddingInfoId = null;

        Long vendorId = getVendor(SecurityUtils.getUserId()).getId();
        /* 供应商投标数据 根据最大的版本来 获取一条数据。二次报价改成N次报价逻辑。 */
        /** 二次报价改成N次报价逻辑 -> {@link com.zhaocai.business.bidding.service.impl.BiddingInfoServiceImpl#twiceBidConf} */
        BiddingInfo biddingInfo = biddingInfoService.getOne(new LambdaQueryWrapper<BiddingInfo>()
                .eq(BiddingInfo::getNoticeId, noticeId)
                .eq(BiddingInfo::getVendorId, vendorId)
                .orderByDesc(BiddingInfo::getTwiceQuotVersion).last("limit 1"));/* 获取供应商最新版本的投标数据 */
        if (ObjectUtils.isEmpty(biddingInfo)){
            detail.setBidStatus(NumberConstant.ZERO);
        } else {
            detail.setBidStatus(NumberConstant.ONE);
            biddingInfoId =  biddingInfo.getId();
        }
        detail.setBidStatus(ObjectUtils.isEmpty(biddingInfo) ? NumberConstant.ZERO : NumberConstant.ONE);

        //投标截止时间
        Date applyTime = tenderNotice.getApplyTime();
        Date nowDate = DateUtils.getNowDate();
        if (detail.getBidEndTime() != null){
            applyTime = detail.getBidEndTime();
        }

        Long endTimeStamp = 0L;
        if (applyTime.after(nowDate)){
            endTimeStamp = applyTime.getTime() - nowDate.getTime();
        }
        detail.setEndTimeStamp(endTimeStamp);
        if (!TenderNoticeStatusEnum.TENDER_ISSUE.getState().equals(tenderNotice.getNoticeStatus())
                || applyTime.before(nowDate)){
            detail.setBidEndStatus(NumberConstant.ONE);
        } else {
            detail.setBidEndStatus(NumberConstant.ZERO);
        }

        //二次报价信息
        if (TenderNoticeStatusEnum.EVALUATION_BID.getState().equals(tenderNotice.getNoticeStatus()) && ObjectUtils.isNotEmpty(biddingInfo)){
            detail.setTwiceQuot(biddingInfo.getTwiceQuot());
            //如果在评标阶段
            //查询二次报价的数据
            BiddingInfo newestBiddingInfo = biddingInfoService.getOne(new LambdaQueryWrapper<BiddingInfo>()
                    .eq(BiddingInfo::getParentId, biddingInfo.getId())
                    .and(q -> q.eq(BiddingInfo::getPriceChangeState, NumberConstant.ONE)/* 已经调价 */
                            .or().isNull(BiddingInfo::getPriceChangeState))/* 历史数据兼容 */
                    .orderByDesc(BiddingInfo::getCreateTime).last("limit 1"));
            if (ObjectUtils.isNotEmpty(newestBiddingInfo)){
                biddingInfoId = newestBiddingInfo.getId();
            }
        }
        detail.setBiddingInfoId(biddingInfoId);

        return detail;
    }

    /* 供应商报名 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean bidNotice(BidVO bidVO) {
        Vendor vendor = getVendor(SecurityUtils.getUserId());
        VendorContact vendorContact = getVendorContact(SecurityUtils.getUserId());

        TenderNoticeDetailVO detail = tenderNoticeService.detail(bidVO.getNoticeId());
        TenderNotice tenderNotice = detail.getTenderNotice();

        /* 校验报名 */
        checkBidNotice(vendor, vendorContact, bidVO, detail);

        TenderApply tenderApply = new TenderApply();
        tenderApply.setNoticeId(tenderNotice.getId());/* 招标id */
        tenderApply.setSchemeId(tenderNotice.getSchemeId());/* 采购方案id */
        tenderApply.setContact(bidVO.getContact());/* 联系人 */
        tenderApply.setPhone(bidVO.getPhone());/* 联系人电话 */
        tenderApply.setVendorId(vendor.getId());/* 供应商id */
        tenderApply.setVendorName(vendor.getEnterpriseName());/* 供应商名称 */
        //设置当前请求的id地址
        tenderApply.setIpAddress(IpUtils.getIpAddr());
        tenderApplyService.save(tenderApply);
        /* 报名保存 状态：报名截至 */
        return tenderNoticeService.updateStatus(tenderNotice.getId(), TenderNoticeStatusEnum.TENDER_REGISTER.getState());
    }

    /* 供应商投标 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean bid(BidVO bidVO) {
        Vendor vendor = getVendor(SecurityUtils.getUserId());
        VendorContact vendorContact = getVendorContact(SecurityUtils.getUserId());

        TenderNoticeDetailVO detail = tenderNoticeService.detail(bidVO.getNoticeId());
        TenderNotice tenderNotice = detail.getTenderNotice();

        //校验投标
        checkBid(vendor, vendorContact, bidVO, detail);

        BiddingInfo biddingInfo;
        ProcurementScheme scheme = procurementSchemeService.getById(tenderNotice.getSchemeId());
        if (null != bidVO.getBiddingInfoId()){
            //修改投标单
            biddingInfo = biddingInfoService.getById(bidVO.getBiddingInfoId());
            biddingInfo.setIpAddress(IpUtils.getIpAddr());
            biddingInfo.setUpdateTime(new Date());
            biddingInfo.setPriceChangeState(NumberConstant.ONE);/* 已调价 */
            biddingInfo.setExpertState(NumberConstant.ZERO);/* 未评分 */
            biddingInfo.setBiddingStatus(BiddingInfoStatusEnum.HAVE_BACK.getState());/* 已回标 */
            biddingInfoService.updateById(biddingInfo);
            //删除投标标书附件
            attachmentService.deleteByBusinessId(AttachmentTypeEnum.BIDING_DOCUMENT, bidVO.getBiddingInfoId());
            //1.删除投标清单报价信息
            biddingListQuotationService.remove(new LambdaQueryWrapper<BiddingListQuotation>()
                    .eq(BiddingListQuotation::getBiddingInfoId, bidVO.getBiddingInfoId()));
        } else {
            //新增投标单
            biddingInfo = BeanCopierUtil.copyBean(bidVO, BiddingInfo.class);
            biddingInfo.setVendorId(vendor.getId());
            biddingInfo.setVendorName(vendor.getEnterpriseName());
            biddingInfo.setBiddingStatus(BiddingInfoStatusEnum.HAVE_BACK.getState());/* 已回标 */
            //采购方案中isReceiveDeposit（0 不收  1 收）
            if (scheme.getIsReceiveDeposit() == NumberConstant.ONE){
                //给一个默认值0
                biddingInfo.setCollectDeposit(NumberConstant.ZERO);
            }
            //设置当前请求的id地址
            biddingInfo.setIpAddress(IpUtils.getIpAddr());

            biddingInfo.setTwiceTime(tenderNotice.getTwiceTime());/* 二次报价截至时间同步 */
            biddingInfo.setTwiceQuot(NumberConstant.ONE);/* 开启调价 */
            biddingInfo.setTwiceQuotVersion(tenderNotice.getTwiceQuotVersion());/* 二次报价版本号同步 */
            biddingInfo.setPriceChangeState(NumberConstant.ONE);/* 已调价 */
            biddingInfo.setExpertState(NumberConstant.ZERO);/* 未评分 */

            //保存投标单信息
            biddingInfoService.save(biddingInfo);
                //如果第一次提交，且选择了是否收取保证金 receive=1为收取，则推送相关财务确认人员信息
            System.out.println("版本:"+tenderNotice.getTwiceQuotVersion());
            if(scheme.getIsReceiveDeposit()!=null
                    &&scheme.getIsReceiveDeposit()==1
                    &&tenderNotice.getTwiceQuotVersion()!=null
                    &&tenderNotice.getTwiceQuotVersion() == 1){
                //调第三方接口，生成开标人员的待办信息
                try {
                    dealOpenPeopleTodoTask(scheme, tenderNotice,vendor);
                }catch (Exception e){
                    log.error(e.toString());
                }


            }
        }

        //保存投标标书附件
        attachmentService.addAttachment(bidVO.getAttachmentList(), AttachmentTypeEnum.BIDING_DOCUMENT,
                biddingInfo.getId());

        //转化成投标清单对象
        List<BidQuotationVO> bidQuotationVoS = this.convMaterials2Quotation(bidVO.getMaterialsList());

        //含税总价,不含税总价
        BigDecimal bidTaxPrice = BigDecimal.ZERO;
        BigDecimal bidNotTaxPrice = BigDecimal.ZERO;
        List<BiddingListQuotation> quotations = new ArrayList<>();

        /* 根据清单确定固定价，浮动价的计算方式 */
        List<Long> materialsIds = bidQuotationVoS.stream().map(BidQuotationVO::getMaterialsId).collect(Collectors.toList());
        List<MaterialsList> materialsListList = materialsListService.list(new LambdaQueryWrapper<MaterialsList>().in(MaterialsList::getId, materialsIds));
        Map<Long,MaterialsList> materialsListMap = materialsListList.stream().collect(Collectors.toMap(MaterialsList::getId, Function.identity(),(existing, replacement) -> replacement));

        for (BidQuotationVO quotationVO : bidQuotationVoS) {

            BiddingListQuotation quotation = BeanCopierUtil.copyBean(quotationVO, BiddingListQuotation.class);
            quotation.setBiddingInfoId(biddingInfo.getId());

            //物料数量
            BigDecimal amount = quotationVO.getAmount();
            //税率A
            BigDecimal taxRateVal = quotationVO.getTaxRate() == null ? BigDecimal.ZERO : quotationVO.getTaxRate();
            BigDecimal taxRate = taxRateVal.divide(new BigDecimal(100));

            BigDecimal taxPrice;
            BigDecimal notTaxPrice;
            if(materialsListMap.get(quotationVO.getMaterialsId()) != null){
                /* 使用清单带的备注信息，从采购计划录入的，传递到采购方案到招标管理。 */
                quotation.setRemark(materialsListMap.get(quotationVO.getMaterialsId()).getRemark());
                if(materialsListMap.get(quotationVO.getMaterialsId()).getPriceType().equals(PriceTypeEnum.FLOAT_PRICE.getType())){
                    /* 如果合约规划拆分的清单是 浮动价 */
                    //浮动价计算方式
                    BigDecimal floatingPrice = quotationVO.getFloatingPrice();
                    //卸费
//                    BigDecimal unloadingFee = quotationVO.getUnloadingFee();
                    //基价
                    BigDecimal basePrice = quotationVO.getBasePrice();
                    //含税单价
                    BigDecimal taxUnitPrice = NumberUtil.add(4, basePrice, floatingPrice);
                    //计算不含税单价
                    BigDecimal notTaxUnitPrice = AmountCalUtil.calUnitPriceExclTax(taxUnitPrice, taxRateVal);
                    //计算含税总价C（每项（基价±浮动价）*每项清单数量）
                    taxPrice = AmountCalUtil.calTotalAmountInclTax(amount, taxUnitPrice);
                    //计算不含税总价
                    notTaxPrice = AmountCalUtil.calTotalAmountExclTax(taxPrice, taxRateVal);
                    quotation.setTaxUnitPrice(taxUnitPrice);
                    quotation.setNotTaxUnitPrice(notTaxUnitPrice);
                    quotation.setTaxPrice(taxPrice);
                    quotation.setNotTaxPrice(notTaxPrice);
                    quotations.add(quotation);
                }else if(materialsListMap.get(quotationVO.getMaterialsId()).getPriceType().equals(PriceTypeEnum.FLOAT_RATE.getType())){
                    /* 如果合约规划拆分的清单是 浮动率 */
                    BigDecimal floatingRate = quotationVO.getFloatingRate();
                    //基价
                    BigDecimal basePrice = quotationVO.getBasePrice();
                    //含税单价 基价 * 浮动率
                    BigDecimal taxUnitPrice = AmountCalUtil.calTotalAmountIncTax(basePrice, floatingRate);
                    //计算不含税单价
                    BigDecimal notTaxUnitPrice = AmountCalUtil.calUnitPriceExclTax(taxUnitPrice, taxRateVal);
                    //计算含税总价C（每项（基价±浮动价）*每项清单数量）
                    taxPrice = AmountCalUtil.calTotalAmountInclTax(amount, taxUnitPrice);
                    //计算不含税总价
                    notTaxPrice = AmountCalUtil.calTotalAmountExclTax(taxPrice, taxRateVal);

                    quotation.setTaxUnitPrice(taxUnitPrice);
                    quotation.setNotTaxUnitPrice(notTaxUnitPrice);
                    quotation.setTaxPrice(taxPrice);
                    quotation.setNotTaxPrice(notTaxPrice);
                    quotations.add(quotation);
                }else if(materialsListMap.get(quotationVO.getMaterialsId()).getPriceType().equals(PriceTypeEnum.FIXED_PRICE.getType())){
                    /* 如果合约规划拆分的清单是 固定价 */
                    //固定价计算方式
                    //含税单价B
                    BigDecimal taxUnitPrice = quotationVO.getTaxUnitPrice() == null ? BigDecimal.ZERO : quotationVO.getTaxUnitPrice();
                    //计算不含税单价
                    BigDecimal notTaxUnitPrice = AmountCalUtil.calUnitPriceExclTax(taxUnitPrice, taxRateVal);
                    //计算含税总价C
                    taxPrice = AmountCalUtil.calTotalAmountInclTax(amount, taxUnitPrice);
                    //计算不含税总价
                    notTaxPrice = AmountCalUtil.calTotalAmountExclTax(taxPrice, taxRateVal);
                    quotation.setNotTaxUnitPrice(notTaxUnitPrice);

                    //增加校验报价的价格条件（只有劳务和专业分包可以高于第一次报价，其他的报价控制不能高于第一次报价）
                    if (!ProcurementPlanTypeEnum.SPECIALTY_SUBCONTRACT.getType().equals(scheme.getProcurementPlanType()) &&
                            !ProcurementPlanTypeEnum.SERVICE_SUBCONTRACT.getType().equals(scheme.getProcurementPlanType())){
                        /*  查询上一次已调价对象 */
                        BiddingInfo newestBiddingInfo = biddingInfoService.getOne(new LambdaQueryWrapper<BiddingInfo>()
                                .eq(BiddingInfo::getParentId, biddingInfo.getId())
                                .and(q -> q.eq(BiddingInfo::getPriceChangeState, NumberConstant.ONE)/* 已经调价 */
                                        .or().isNull(BiddingInfo::getPriceChangeState))/* 历史数据兼容 */
                                .orderByDesc(BiddingInfo::getCreateTime).last("limit 1"));
                        //校验含税单价
                        checkTaxUnitPrice(newestBiddingInfo==null?biddingInfo:newestBiddingInfo, quotationVO, taxUnitPrice);
                    }
                    quotation.setTaxPrice(taxPrice);
                    quotation.setNotTaxPrice(notTaxPrice);
                    quotations.add(quotation);
                }else{
                    /* 原来的浮动价固定价处理方法 */
                    oldMethods(quotationVO, scheme, taxRateVal, amount, quotation, biddingInfo, quotations);
                }
            }else{
                /* 原来的浮动价固定价处理方法 */
                oldMethods(quotationVO, scheme, taxRateVal, amount, quotation, biddingInfo, quotations);
            }

            bidTaxPrice = bidTaxPrice.add(quotation.getTaxPrice());
            bidNotTaxPrice = bidNotTaxPrice.add(quotation.getNotTaxPrice());
        }
        //保存投标清单报价信息
        biddingListQuotationService.saveBatch(quotations);

        biddingInfo.setTaxPrice(bidTaxPrice);
        biddingInfo.setNotTaxPrice(bidNotTaxPrice);
        boolean res = biddingInfoService.updateById(biddingInfo);

        return res;
    }

    /* 原来的浮动价固定价处理方法 */
    private void oldMethods(BidQuotationVO quotationVO, ProcurementScheme scheme, BigDecimal taxRateVal, BigDecimal amount, BiddingListQuotation quotation, BiddingInfo biddingInfo, List<BiddingListQuotation> quotations) {
        BigDecimal taxPrice;
        BigDecimal notTaxPrice;
        //（subject_matter 是 1（钢筋）|| 2（砼））& price_type 浮动价
        if (PriceTypeEnum.FLOAT_PRICE.getType().equals(scheme.getPriceType())
                && scheme.getSubjectMatterType()!=null&&(scheme.getSubjectMatterType() == 1 || scheme.getSubjectMatterType() == 2)){
            //浮动价计算方式
            BigDecimal floatingPrice = quotationVO.getFloatingPrice();
            //卸费
            BigDecimal unloadingFee = quotationVO.getUnloadingFee();
            //基价
            BigDecimal basePrice = quotationVO.getBasePrice();
            //含税单价
            BigDecimal taxUnitPrice = NumberUtil.add(4, basePrice, floatingPrice, unloadingFee);
            //计算不含税单价
            BigDecimal notTaxUnitPrice = AmountCalUtil.calUnitPriceExclTax(taxUnitPrice, taxRateVal);
            //计算含税总价C（每项（基价±浮动价+运费+卸费）*每项清单数量）
            taxPrice = AmountCalUtil.calTotalAmountInclTax(amount, taxUnitPrice);
            //计算不含税总价
            notTaxPrice = AmountCalUtil.calTotalAmountExclTax(taxPrice, taxRateVal);
            quotation.setTaxUnitPrice(taxUnitPrice);
            quotation.setNotTaxUnitPrice(notTaxUnitPrice);
        } else {
            //固定价计算方式
            //含税单价B
            BigDecimal taxUnitPrice = quotationVO.getTaxUnitPrice() == null ? BigDecimal.ZERO : quotationVO.getTaxUnitPrice();
            //计算不含税单价
            BigDecimal notTaxUnitPrice = AmountCalUtil.calUnitPriceExclTax(taxUnitPrice, taxRateVal);
            //计算含税总价C
            taxPrice = AmountCalUtil.calTotalAmountInclTax(amount, taxUnitPrice);
            //计算不含税总价
            notTaxPrice = AmountCalUtil.calTotalAmountExclTax(taxPrice, taxRateVal);
            quotation.setNotTaxUnitPrice(notTaxUnitPrice);

            //增加校验报价的价格条件（只有劳务和专业分包可以高于第一次报价，其他的报价控制不能高于第一次报价）
            if (!ProcurementPlanTypeEnum.SPECIALTY_SUBCONTRACT.getType().equals(scheme.getProcurementPlanType()) &&
                    !ProcurementPlanTypeEnum.SERVICE_SUBCONTRACT.getType().equals(scheme.getProcurementPlanType())){
                /*  查询上一次已调价对象 */
                BiddingInfo newestBiddingInfo = biddingInfoService.getOne(new LambdaQueryWrapper<BiddingInfo>()
                        .eq(BiddingInfo::getParentId, biddingInfo.getId())
                        .and(q -> q.eq(BiddingInfo::getPriceChangeState, NumberConstant.ONE)/* 已经调价 */
                                .or().isNull(BiddingInfo::getPriceChangeState))/* 历史数据兼容 */
                        .orderByDesc(BiddingInfo::getCreateTime).last("limit 1"));
                //校验含税单价
                checkTaxUnitPrice(newestBiddingInfo==null?biddingInfo:newestBiddingInfo, quotationVO, taxUnitPrice);
            }
        }
        quotation.setTaxPrice(taxPrice);
        quotation.setNotTaxPrice(notTaxPrice);
        quotations.add(quotation);
    }



    private void dealOpenPeopleTodoTask(ProcurementScheme procurementScheme, TenderNotice tenderNotice,  Vendor vendor) {
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
                + "发布了招标文件、供应商"
                + vendor.getEnterpriseName()
                + "已经响应提交了投标文件，且需要收取投标保证金、请及时确认是否收到保证金！";

        log.info("[供应商][财务人员待办信息][xm] {}",xm);
        requestDTO.setContent(xm);
        requestDTO.setPrjName((project==null?"":project.getMinAccountSimpleName()==null?"":project.getMinAccountSimpleName()));
        requestDTO.setArrivalTime(formatDate(new Date()));
        requestDTO.setCreateTime(formatDate(new Date()));
        String thridUserId = SecurityUtils.getThridUserId();
        requestDTO.setMsgFromPerCode(StringUtils.isNotEmpty(thridUserId) ? Long.parseLong(thridUserId) : null);
        requestDTO.setMsgFromPerName(SecurityUtils.getLoginUserNickName());
        String findThirdUserId = findThirdUserId(procurementScheme.getFinanceConfirmId()==null?null:Long.valueOf(procurementScheme.getFinanceConfirmId()));
        requestDTO.setMsgToPerCode(StringUtils.isNotEmpty(findThirdUserId) ? Long.parseLong(findThirdUserId) : null);
        requestDTO.setMsgToPerName(procurementScheme.getFinanceConfirmName());
        requestDTO.setFlowGroup(ThirdPartyTodoFlowGroupEnum.XCW_BID.getDesc());
        requestDTO.setFlowModule(ThirdPartyTodoFlowModuleEnum.BID_MANAGE.getDesc());
        requestDTO.setFlowName(procurementScheme.getFinanceConfirmName() + "的" + ThirdPartyTodoFlowGroupEnum.XCW_BID.getDesc());
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
        parentRequestDTO.setMessageList(messageList);
        log.info("[财务人员待办信息][messageList] {}",messageList);
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

    /** 校验是否可以报名 */
    private void checkBidNotice(Vendor vendor, VendorContact vendorContact, BidVO bidVO, TenderNoticeDetailVO detail){
        TenderNotice tenderNotice = detail.getTenderNotice();
        /* 供应商范围 直接查询表 */
        List<TenderNoticeRange> rangeList = detail.getRangeList();
        //判断当前供应商联系人账号是否有效
        if (!VendorContactStateEnum.VALID.equalsState(vendorContact.getState())) {
            throw new ParamValidateException("您当前供应商账户状态不是有效状态，请先设置有效状态再操作");
        }
        long count = tenderApplyService.count(new LambdaQueryWrapper<TenderApply>()
                .eq(TenderApply::getVendorId, vendor.getId())
                .eq(TenderApply::getNoticeId, bidVO.getNoticeId()));
        if (count > 0){
            throw new ParamValidateException("不允许重复报名");
        }

        /* 获取的 变更后的值。 */
        if (tenderNotice.getApplyTimeNotice().before(DateUtils.getNowDate())){
            throw new ParamValidateException("报名时间已截止，不允许报名");
        }
        /* 状态 ： 招标公告 和 报名情况，下一步 报名截至 就不能报名了。 */
        if (!TenderNoticeStatusEnum.TENDER_NOTICE.getState().equals(tenderNotice.getNoticeStatus())&&!TenderNoticeStatusEnum.TENDER_REGISTER.getState().equals(tenderNotice.getNoticeStatus())){
            throw new ParamValidateException("投标公告状态已变更，不允许报名");
        }
        /* 非公开招标进行 供应商范围校验，公开招标不做验证 */
        if (tenderNotice.getVendorRange() == 1 && detail.getSchemeType() != NumberConstant.ONE && !CollectionUtils.isEmpty(rangeList)){
            List<Long> vendorIds = rangeList.stream().map(TenderNoticeRange::getVendorId).collect(Collectors.toList());
            if (!vendorIds.contains(vendor.getId())){
                throw new ParamValidateException("不在设置的供应商范围内，不允许报名");
            }
        }
    }

    /** 校验是否可以投标 */
    private void checkBid(Vendor vendor, VendorContact vendorContact, BidVO bidVO, TenderNoticeDetailVO detail){
        TenderNotice tenderNotice = detail.getTenderNotice();
        /* 供应商范围 直接查询表 */
        List<TenderNoticeRange> rangeList = detail.getRangeList();
        //判断当前供应商联系人账号是否有效
        if (!VendorContactStateEnum.VALID.equalsState(vendorContact.getState())) {
            throw new ParamValidateException("您当前供应商账户状态不是有效状态，请先设置有效状态再操作");
        }


        if (null != bidVO.getBiddingInfoId()){
            BiddingInfo biddingInfo = biddingInfoService.getById(bidVO.getBiddingInfoId());
            if(biddingInfo==null){
                throw new ParamValidateException("未找到投标数据");
            }
            if(tenderNotice.getTwiceQuotVersion()>1){
                if (biddingInfo.getTwiceQuot().equals(NumberConstant.ZERO)){
                    throw new ParamValidateException("二次报价已截止，不允许投标");
                }
                if (biddingInfo.getTwiceTime().before(DateUtils.getNowDate())){
                    throw new ParamValidateException("二次报价时间已截止，不允许投标");
                }
            }
        } else {
            long count = biddingInfoService.count(new LambdaQueryWrapper<BiddingInfo>()
                    .eq(BiddingInfo::getVendorId, vendor.getId())/* 供应商id */
                    .eq(BiddingInfo::getSchemeId, bidVO.getSchemeId())/* 采购方案id */
                    .eq(BiddingInfo::getNoticeId, bidVO.getNoticeId())/* 招标对象id */
                    .isNull(BiddingInfo::getParentId));
            if (count > 0){
                throw new ParamValidateException("不允许重复投标");
            }
        }

        /* 二次报价不做验证 */
        if(tenderNotice.getTwiceQuotVersion()==null || tenderNotice.getTwiceQuotVersion()==NumberConstant.ONE)
            if (detail.getBidEndTime() != null){
                if (detail.getBidEndTime().before(DateUtils.getNowDate())){
                    throw new ParamValidateException("投标时间已截止，不允许投标");
                }
            } else {
                if (tenderNotice.getApplyTime().before(DateUtils.getNowDate())){
                    throw new ParamValidateException("投标时间已截止，不允许投标");
                }
            }
        /* 第一次投标 */
        if(tenderNotice.getTwiceQuotVersion()!=null && tenderNotice.getTwiceQuotVersion().equals(NumberConstant.ONE)){
            if (!TenderNoticeStatusEnum.TENDER_ISSUE.getState().equals(tenderNotice.getNoticeStatus())){
                throw new ParamValidateException("投标公告状态已变更，不允许投标");
            }
            /* 第n+1次投标 */
        }else if(tenderNotice.getTwiceQuotVersion()!=null && !tenderNotice.getTwiceQuotVersion().equals(NumberConstant.ONE)){
            if (!TenderNoticeStatusEnum.EVALUATION_BID.getState().equals(tenderNotice.getNoticeStatus())){
                throw new ParamValidateException("投标公告状态已变更，不允许投标");
            }
        }
        /* 非公开招标进行 供应商范围校验，公开招标不做验证 */
        if (tenderNotice.getVendorRange() == 1 && detail.getSchemeType() != NumberConstant.ONE && !CollectionUtils.isEmpty(rangeList)){
            List<Long> vendorIds = rangeList.stream().map(TenderNoticeRange::getVendorId).collect(Collectors.toList());
            if (!vendorIds.contains(vendor.getId())){
                throw new ParamValidateException("不在设置的供应商范围内，不允许投标");
            }
        }
        ProcurementScheme scheme = procurementSchemeService.getById(tenderNotice.getSchemeId());
        //采购方案 招标类型 1公开招标
        if (scheme.getProcurementType() == NumberConstant.ONE){
            long openCount = tenderApplyService.count(new LambdaQueryWrapper<TenderApply>()
                    .eq(TenderApply::getVendorId, vendor.getId())
                    .eq(TenderApply::getNoticeId, tenderNotice.getId()));
            if (openCount<1){
                throw new ParamValidateException("未报名，不允许投标");
            }
        }

        /* 每次投标不允许高于上一次投标报价 */
        if (null != bidVO.getBiddingInfoId()){
            BiddingInfo biddingInfo = biddingInfoService.getById(bidVO.getBiddingInfoId());
            //转化成投标清单对象
            List<BidQuotationVO> bidQuotationVoS = this.convMaterials2Quotation(bidVO.getMaterialsList());
            //含税总价,不含税总价
            BigDecimal bidTaxPrice = BigDecimal.ZERO;
            /* 根据清单确定固定价，浮动价的计算方式 */
            List<Long> materialsIds = bidQuotationVoS.stream().map(BidQuotationVO::getMaterialsId).collect(Collectors.toList());
            List<MaterialsList> materialsListList = materialsListService.list(new LambdaQueryWrapper<MaterialsList>().in(MaterialsList::getId, materialsIds));
            Map<Long,MaterialsList> materialsListMap = materialsListList.stream().collect(Collectors.toMap(MaterialsList::getId, Function.identity(),(existing, replacement) -> replacement));

            BiddingInfo newestBiddingInfo = biddingInfoService.getOne(new LambdaQueryWrapper<BiddingInfo>()
                    .eq(BiddingInfo::getParentId, biddingInfo.getParentId())
                    .orderByDesc(BiddingInfo::getCreateTime).last("limit 1"));
            for (BidQuotationVO quotationVO : bidQuotationVoS) {
                //物料数量
                BigDecimal amount = quotationVO.getAmount();
                BigDecimal taxPrice;
                if(materialsListMap.get(quotationVO.getMaterialsId()) != null){
                    if(materialsListMap.get(quotationVO.getMaterialsId()).getPriceType().equals(PriceTypeEnum.FLOAT_PRICE.getType())){
                        /* 如果合约规划拆分的清单是 浮动价 */
                        BigDecimal floatingPrice = quotationVO.getFloatingPrice();
                        //卸费
//                        BigDecimal unloadingFee = quotationVO.getUnloadingFee();
                        //基价
                        BigDecimal basePrice = quotationVO.getBasePrice();
                        //含税单价
//                        BigDecimal taxUnitPrice = NumberUtil.add(4, basePrice, floatingPrice, unloadingFee);
                        BigDecimal taxUnitPrice = NumberUtil.add(4, basePrice, floatingPrice);
                        //计算含税总价C（每项（基价±浮动价）*每项清单数量）
                        taxPrice = AmountCalUtil.calTotalAmountInclTax(amount, taxUnitPrice);
                        bidTaxPrice = bidTaxPrice.add(taxPrice);
                        /** 校验含税单价 */
                        //校验含税单价
                        checkTaxUnitPrice(newestBiddingInfo==null?biddingInfo:newestBiddingInfo, quotationVO, taxUnitPrice);
                    }else if(materialsListMap.get(quotationVO.getMaterialsId()).getPriceType().equals(PriceTypeEnum.FLOAT_RATE.getType())){
                        /* 如果合约规划拆分的清单是 浮动率 */
                        BigDecimal floatingRate = quotationVO.getFloatingRate();
                        //基价
                        BigDecimal basePrice = quotationVO.getBasePrice();
                        //含税单价 基价 * 浮动率
                        BigDecimal taxUnitPrice = AmountCalUtil.calTotalAmountIncTax(basePrice, floatingRate);
                        //计算含税总价C（每项（基价±浮动价）*每项清单数量）
                        taxPrice = AmountCalUtil.calTotalAmountInclTax(amount, taxUnitPrice);
                        bidTaxPrice = bidTaxPrice.add(taxPrice);
                        /** 校验含税单价 */
                        //校验含税单价
                        checkTaxUnitPrice(newestBiddingInfo==null?biddingInfo:newestBiddingInfo, quotationVO, taxUnitPrice);
                    }else if(materialsListMap.get(quotationVO.getMaterialsId()).getPriceType().equals(PriceTypeEnum.FIXED_PRICE.getType())){
                        /* 如果合约规划拆分的清单是 固定价 */
                        //含税单价B
                        BigDecimal taxUnitPrice = quotationVO.getTaxUnitPrice() == null ? BigDecimal.ZERO : quotationVO.getTaxUnitPrice();
                        //计算含税总价C
                        taxPrice = AmountCalUtil.calTotalAmountInclTax(amount, taxUnitPrice);
                        bidTaxPrice = bidTaxPrice.add(taxPrice);
                        //增加校验报价的价格条件（只有劳务和专业分包可以高于第一次报价，其他的报价控制不能高于第一次报价）
                        if (!ProcurementPlanTypeEnum.SPECIALTY_SUBCONTRACT.getType().equals(scheme.getProcurementPlanType()) &&
                                !ProcurementPlanTypeEnum.SERVICE_SUBCONTRACT.getType().equals(scheme.getProcurementPlanType())){
                            /** 校验含税单价 */
                            //校验含税单价
                            checkTaxUnitPrice(newestBiddingInfo==null?biddingInfo:newestBiddingInfo, quotationVO, taxUnitPrice);
                        }
                    }
                }
            }
            if(newestBiddingInfo!=null && bidTaxPrice.compareTo(newestBiddingInfo.getTaxPrice())>0){
                throw new ParamValidateException("清单报价的含税总价不能高于最近一次报价");
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean withdrawBid(Long biddingInfoId) {
        BiddingInfo biddingInfo = biddingInfoService.getById(biddingInfoId);
        //投标截止时间24小时外可以撤回
        TenderNoticeDetailVO detailVO = tenderNoticeService.detail(biddingInfo.getNoticeId());
        //投标截止时间
        Date applyTime = detailVO.getTenderNotice().getApplyTime();
        if (detailVO.getBidEndTime() != null){
            applyTime = detailVO.getBidEndTime();
        }

        //当前时间
        Date nextDay = DateUtils.plusDay(DateUtils.getNowDate(), NumberConstant.ONE);
        if (nextDay.after(applyTime)){
            throw new ParamValidateException("投标截止时间24小时外才允许撤回");
        }
        if (!detailVO.getTenderNotice().getNoticeStatus().equals(TenderNoticeStatusEnum.TENDER_ISSUE.getState())){
            throw new ParamValidateException("投标公告状态已变更，不允许撤回");
        }

        //1.删除投标清单报价信息
        biddingListQuotationService.remove(new LambdaQueryWrapper<BiddingListQuotation>()
                .eq(BiddingListQuotation::getBiddingInfoId, biddingInfoId));

        //2.删除投标标书附件
        attachmentService.deleteByBusinessId(AttachmentTypeEnum.BIDING_DOCUMENT, biddingInfoId);

        //3.删除投标单信息
        return biddingInfoService.removeById(biddingInfoId);
    }

    @Override
    public PageResult<TwiceBidListVO> twiceBidPage(TwiceBidPageQueryVO queryDTO) {
        queryDTO.setVendorId(getVendor(SecurityUtils.getUserId()).getId());
        PageResult<TwiceBidListVO> pageResult = tenderNoticeService.selectTwiceBidPage(queryDTO);
        return pageResult;
    }

    private Vendor getVendor(Long userId){
        return vendorService.getByLoginUser(userId);
    }

    private VendorContact getVendorContact(Long userId){
        return vendorContactService.getVendorContactByLoginUser(userId);
    }

    /** 二次报价 (弃用) */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean twiceBid(BidVO bidVO) {
        Vendor vendor = getVendor(SecurityUtils.getUserId());
        BiddingInfo biddingInfoOld = biddingInfoService.getOne(new LambdaQueryWrapper<BiddingInfo>()
                .eq(BiddingInfo::getVendorId, vendor.getId())
                .eq(BiddingInfo::getNoticeId, bidVO.getNoticeId())
                .isNull(BiddingInfo::getParentId));
        if (ObjectUtils.isEmpty(biddingInfoOld)){
            throw new ParamValidateException("请先进行首轮报价");
        }
        if (tenderNoticeService.getCountTenderNoticeStatus(
                bidVO.getNoticeId(), TenderNoticeStatusEnum.EVALUATION_BID.getState()) == 0){
            throw new ParamValidateException("投标公告状态已变更，请确认当前招标公告状态");
        }
        //增加校验报价的价格条件（只有劳务和专业分包可以高于第一次报价，其他的报价控制不能高于第一次报价）
        ProcurementScheme scheme = procurementSchemeService.getById(bidVO.getSchemeId());
        BiddingInfo biddingInfoRecent = null;
        if (!ProcurementPlanTypeEnum.SPECIALTY_SUBCONTRACT.getType().equals(scheme.getProcurementPlanType()) &&
                !ProcurementPlanTypeEnum.SERVICE_SUBCONTRACT.getType().equals(scheme.getProcurementPlanType())){
            //如果不是劳务分包或专业分包，那么报价不能高于最近一次报价
            //查询是否有最新的报价信息
            BiddingInfo newestBiddingInfo = biddingInfoService.getOne(new LambdaQueryWrapper<BiddingInfo>()
                    .eq(BiddingInfo::getParentId, biddingInfoOld.getId())
                    .and(q -> q.eq(BiddingInfo::getPriceChangeState, NumberConstant.ONE)/* 已经调价 */
                            .or().isNull(BiddingInfo::getPriceChangeState))/* 历史数据兼容 */
                    .orderByDesc(BiddingInfo::getCreateTime).last("limit 1"));
            if (ObjectUtils.isNotEmpty(newestBiddingInfo)){
                biddingInfoRecent = newestBiddingInfo;
            } else {
                biddingInfoRecent = biddingInfoOld;
            }

        }
        BiddingInfo biddingInfo = BeanCopierUtil.copyBean(bidVO, BiddingInfo.class);

        TenderNoticeDetailVO detail = tenderNoticeService.detail(bidVO.getNoticeId());
        TenderNotice tenderNotice = detail.getTenderNotice();
        //查询是否有最新的报价信息
        BiddingInfo newestBiddingInfo = biddingInfoService.getOne(new LambdaQueryWrapper<BiddingInfo>()
                .eq(BiddingInfo::getParentId, biddingInfoOld.getId())
                .and(q -> q.eq(BiddingInfo::getPriceChangeState, NumberConstant.ONE)/* 已经调价 */
                        .or().isNull(BiddingInfo::getPriceChangeState))/* 历史数据兼容 */
                .orderByDesc(BiddingInfo::getCreateTime).last("limit 1"));
        /* 如果版本号一样就不再新增数据了。 */
        if(newestBiddingInfo!=null && tenderNotice!=null && tenderNotice.getTwiceQuotVersion().equals(newestBiddingInfo.getTwiceQuotVersion())){
            biddingInfo.setId(newestBiddingInfo.getId());
        }

        biddingInfo.setVendorId(vendor.getId());
        biddingInfo.setVendorName(vendor.getEnterpriseName());
        biddingInfo.setBiddingStatus(BiddingInfoStatusEnum.HAVE_BACK.getState());
        //不用设置保证金
        //设置当前请求的id地址
        biddingInfo.setIpAddress(IpUtils.getIpAddr());
        biddingInfo.setParentId(biddingInfoOld.getId());


        //保存投标单信息
        boolean res = biddingInfoService.save(biddingInfo);

        //转化成投标清单对象
        List<BidQuotationVO> bidQuotationVoS = this.convMaterials2Quotation(bidVO.getMaterialsList());

        //含税总价,不含税总价
        BigDecimal bidTaxPrice = BigDecimal.ZERO;
        BigDecimal bidNotTaxPrice = BigDecimal.ZERO;
        List<BiddingListQuotation> quotations = new ArrayList<>();
        for (BidQuotationVO quotationVO : bidQuotationVoS) {
            BiddingListQuotation quotation = BeanCopierUtil.copyBean(quotationVO, BiddingListQuotation.class);
            quotation.setBiddingInfoId(biddingInfo.getId());

            //物料数量
            BigDecimal amount = quotationVO.getAmount();
            //税率A
            BigDecimal taxRateVal = quotationVO.getTaxRate() == null ? BigDecimal.ZERO : quotationVO.getTaxRate();
            BigDecimal taxRate = taxRateVal.divide(new BigDecimal(100));

            BigDecimal taxPrice;
            BigDecimal notTaxPrice;
            if (PriceTypeEnum.FLOAT_PRICE.getType().equals(scheme.getPriceType())
                    && (scheme.getSubjectMatterType() == 1 || scheme.getSubjectMatterType() == 2)){
                /* 浮动价计算方式 */
                BigDecimal floatingPrice = quotationVO.getFloatingPrice();
                //卸费
                BigDecimal unloadingFee = quotationVO.getUnloadingFee();
                //基价
                BigDecimal basePrice = quotationVO.getBasePrice();
                /*//计算含税总价C（每项（基价±浮动价+运费+卸费）*每项清单数量）
                taxPrice = NumberUtil.multiply(amount, NumberUtil.add(5, basePrice, floatingPrice, unloadingFee),5);
                //计算不含税总价
                notTaxPrice = taxPrice.divide(BigDecimal.ONE.add(taxRate), 5, RoundingMode.HALF_UP);*/
                //含税单价
                BigDecimal taxUnitPrice = NumberUtil.add(4, basePrice, floatingPrice, unloadingFee);
                //计算不含税单价
                BigDecimal notTaxUnitPrice = AmountCalUtil.calUnitPriceExclTax(taxUnitPrice, taxRateVal);
                //计算含税总价C（每项（基价±浮动价+运费+卸费）*每项清单数量）
                taxPrice = AmountCalUtil.calTotalAmountInclTax(amount, taxUnitPrice);
                //计算不含税总价
                notTaxPrice = AmountCalUtil.calTotalAmountExclTax(taxPrice, taxRateVal);
                quotation.setTaxUnitPrice(taxUnitPrice);
                quotation.setNotTaxUnitPrice(notTaxUnitPrice);
            } else {
                /** 固定价计算方式 */
                //含税单价B
                BigDecimal taxUnitPrice = quotationVO.getTaxUnitPrice() == null ? BigDecimal.ZERO : quotationVO.getTaxUnitPrice();
                /*//计算不含税单价
                BigDecimal notTaxUnitPrice = taxUnitPrice.divide(BigDecimal.ONE.add(taxRate), 5, RoundingMode.HALF_UP);
                //计算含税总价C
                taxPrice = taxUnitPrice.multiply(amount);
                //计算不含税总价
                notTaxPrice = taxPrice.divide(BigDecimal.ONE.add(taxRate), 5, RoundingMode.HALF_UP);*/
                //计算不含税单价
                BigDecimal notTaxUnitPrice = AmountCalUtil.calUnitPriceExclTax(taxUnitPrice, taxRateVal);
                //计算含税总价C
                taxPrice = AmountCalUtil.calTotalAmountInclTax(amount, taxUnitPrice);
                //计算不含税总价
                notTaxPrice = AmountCalUtil.calTotalAmountExclTax(taxPrice, taxRateVal);
                quotation.setNotTaxUnitPrice(notTaxUnitPrice);

                quotation.setNotTaxUnitPrice(notTaxUnitPrice);
                //校验含税单价
                checkTaxUnitPrice(newestBiddingInfo==null?biddingInfo:newestBiddingInfo, quotationVO, taxUnitPrice);
            }
            quotation.setTaxPrice(taxPrice);
            quotation.setNotTaxPrice(notTaxPrice);
            quotations.add(quotation);

            bidTaxPrice = bidTaxPrice.add(quotation.getTaxPrice());
            bidNotTaxPrice = bidNotTaxPrice.add(quotation.getNotTaxPrice());

        }
        //保存投标标书附件
        attachmentService.addAttachment(bidVO.getAttachmentList(), AttachmentTypeEnum.BIDING_DOCUMENT,
                biddingInfo.getId());

        //保存投标清单报价信息
        biddingListQuotationService.saveBatch(quotations);

        //更新投标单数据
        biddingInfo.setTaxPrice(bidTaxPrice);
        biddingInfo.setNotTaxPrice(bidNotTaxPrice);
        biddingInfoService.updateById(biddingInfo);

        return res;
    }


    /** 校验 含税单价不能高于最近一次报价 */
    private void checkTaxUnitPrice(BiddingInfo biddingInfoRecent, BidQuotationVO quotationVO, BigDecimal taxUnitPrice){
        if (ObjectUtils.isNotEmpty(biddingInfoRecent)){
            //比较每个清单的含税单价
            BiddingListQuotation quotationRecent = biddingListQuotationService.getOne(new LambdaQueryWrapper<BiddingListQuotation>()
                    .eq(BiddingListQuotation::getBiddingInfoId, biddingInfoRecent.getId())
                    .eq(BiddingListQuotation::getMaterialsCode, quotationVO.getMaterialsCode())
                    /* 如果存在两份拆分清单就会查重,加一个物料id对比上次的。 */
                    .eq(BiddingListQuotation::getMaterialsId, quotationVO.getMaterialsId())
                    .lt(BiddingListQuotation::getTaxUnitPrice, taxUnitPrice));
            if (ObjectUtils.isNotEmpty(quotationRecent)){
                throw new ParamValidateException(quotationVO.getMaterialsName()
                        + "(" + quotationVO.getMaterialsCode() + ")的含税单价不能高于最近一次报价");
            }
        }
    }

    private List<BidQuotationVO> convMaterials2Quotation(List<CompMaterialsVO> materialsList){
        //投标清单信息
        List<BidQuotationVO> bidQuotationVoS = new ArrayList<>();
        BidQuotationVO bidQuotationVO;
        for (CompMaterialsVO compMaterialsVO : materialsList) {
            //循环多个计划
            List<CompContractSplitMaterialsVO> compVOList = compMaterialsVO.getCompVOList();
            for (CompContractSplitMaterialsVO splitMaterialsVO : compVOList) {
                //获取多个物料清单
                List<CompMaterialsContentVO> materialsLists = splitMaterialsVO.getMaterialsLists();
                for (CompMaterialsContentVO compMaterialsContentVO : materialsLists) {
                    if (compMaterialsContentVO.getFloatingPrice() != null || compMaterialsContentVO.getFloatingRate() != null || compMaterialsContentVO.getTaxUnitPrice() != null){
                        //如果投标的时候，物资的浮动价或含税单价不为空（也就是说物资有投标数据），那就保存进投标单
                        bidQuotationVO = BeanCopierUtil.copyBean(compMaterialsContentVO, BidQuotationVO.class);
                        //设置拆分id
                        bidQuotationVO.setSplitId(splitMaterialsVO.getSplitId());
                        //设置物料id
                        bidQuotationVO.setMaterialsId(compMaterialsContentVO.getId());
                        bidQuotationVO.setAmount(compMaterialsContentVO.getCount());
                        bidQuotationVoS.add(bidQuotationVO);
                    }
                }
            }
        }
        return bidQuotationVoS;
    }



    @Override
    public PageResult<WinningNotifiListVO> winningNotifiPage(WinningNotifiPageQueryVO queryDTO) {
        queryDTO.setVendorId(getVendor(SecurityUtils.getUserId()).getId());
        queryDTO.setNoticeStatus(TenderNoticeStatusEnum.COMPLETE.getState());
        PageResult<WinningNotifiListVO> pageResult = tenderNoticeService.selectWinningNotifiPage(queryDTO);
        return pageResult;
    }

}
