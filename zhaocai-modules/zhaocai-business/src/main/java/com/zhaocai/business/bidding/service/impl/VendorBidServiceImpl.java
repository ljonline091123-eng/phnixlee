package com.zhaocai.business.bidding.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhaocai.business.bidding.domain.BiddingInfo;
import com.zhaocai.business.bidding.domain.BiddingListQuotation;
import com.zhaocai.business.bidding.domain.TenderNotice;
import com.zhaocai.business.bidding.domain.TenderNoticeRange;
import com.zhaocai.business.bidding.enums.BiddingInfoStatusEnum;
import com.zhaocai.business.bidding.enums.TenderNoticeStatusEnum;
import com.zhaocai.business.bidding.service.IBiddingInfoService;
import com.zhaocai.business.bidding.service.IBiddingListQuotationService;
import com.zhaocai.business.bidding.service.ITenderNoticeService;
import com.zhaocai.business.bidding.service.IVendorBidService;
import com.zhaocai.business.bidding.vo.req.BidQuotationVO;
import com.zhaocai.business.bidding.vo.req.BidVO;
import com.zhaocai.business.bidding.vo.req.query.TwiceBidPageQueryVO;
import com.zhaocai.business.bidding.vo.req.query.VendorNoticePageQueryVO;
import com.zhaocai.business.bidding.vo.req.query.WinningNotifiPageQueryVO;
import com.zhaocai.business.bidding.vo.res.TenderNoticeDetailVO;
import com.zhaocai.business.bidding.vo.res.TwiceBidListVO;
import com.zhaocai.business.bidding.vo.res.VendorNoticeListVO;
import com.zhaocai.business.bidding.vo.res.WinningNotifiListVO;
import com.zhaocai.business.common.enums.AttachmentTypeEnum;
import com.zhaocai.business.common.enums.PriceTypeEnum;
import com.zhaocai.business.common.enums.ProcurementPlanTypeEnum;
import com.zhaocai.business.common.enums.VendorContactStateEnum;
import com.zhaocai.business.common.exception.ParamValidateException;
import com.zhaocai.business.common.utils.AmountCalUtil;
import com.zhaocai.business.procurement.domain.ProcurementScheme;
import com.zhaocai.business.procurement.service.IProcurementSchemeService;
import com.zhaocai.business.procurement.vo.res.CompContractSplitMaterialsVO;
import com.zhaocai.business.procurement.vo.res.CompMaterialsContentVO;
import com.zhaocai.business.procurement.vo.res.CompMaterialsVO;
import com.zhaocai.business.pub.service.IAttachmentService;
import com.zhaocai.business.vendor.domain.Vendor;
import com.zhaocai.business.vendor.domain.VendorContact;
import com.zhaocai.business.vendor.service.IVendorContactService;
import com.zhaocai.business.vendor.service.IVendorService;
import com.zhaocai.common.core.bean.PageResult;
import com.zhaocai.common.core.constant.NumberConstant;
import com.zhaocai.common.core.utils.DateUtils;
import com.zhaocai.common.core.utils.NumberUtil;
import com.zhaocai.common.core.utils.bean.BeanCopierUtil;
import com.zhaocai.common.core.utils.ip.IpUtils;
import com.zhaocai.common.security.utils.SecurityUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
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
    private IProcurementSchemeService procurementSchemeService;


    @Override
    public PageResult<VendorNoticeListVO> page(VendorNoticePageQueryVO queryDTO) {
        queryDTO.setVendorId(getVendor(SecurityUtils.getUserId()).getId());
        PageResult<VendorNoticeListVO> pageResult = tenderNoticeService.selectVendorNoticePage(queryDTO);
        return pageResult;
    }

    @Override
    public TenderNoticeDetailVO detail(Long noticeId) {
        TenderNoticeDetailVO detail = tenderNoticeService.detail(noticeId);
        TenderNotice tenderNotice = detail.getTenderNotice();
        Long biddingInfoId = null;

        Long vendorId = getVendor(SecurityUtils.getUserId()).getId();
        BiddingInfo biddingInfo = biddingInfoService.getOne(new LambdaQueryWrapper<BiddingInfo>()
                .eq(BiddingInfo::getNoticeId, noticeId)
                .eq(BiddingInfo::getVendorId, vendorId)
                .isNull(BiddingInfo::getParentId));
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
                    .orderByDesc(BiddingInfo::getCreateTime).last("limit 1"));
            if (ObjectUtils.isNotEmpty(newestBiddingInfo)){
                biddingInfoId = newestBiddingInfo.getId();
            }
        }
        detail.setBiddingInfoId(biddingInfoId);

        return detail;
    }

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
            biddingInfo.setBiddingStatus(BiddingInfoStatusEnum.HAVE_BACK.getState());
            //采购方案中isReceiveDeposit（0 不收  1 收）
            if (scheme.getIsReceiveDeposit() == NumberConstant.ONE){
                //给一个默认值0
                biddingInfo.setCollectDeposit(NumberConstant.ZERO);
            }
            //设置当前请求的id地址
            biddingInfo.setIpAddress(IpUtils.getIpAddr());

            //保存投标单信息
            biddingInfoService.save(biddingInfo);
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
            //（subject_matter 是 1（钢筋）|| 2（砼））& price_type 浮动价
            if (PriceTypeEnum.FLOAT_PRICE.getType().equals(scheme.getPriceType())
                    && (scheme.getSubjectMatterType() == 1 || scheme.getSubjectMatterType() == 2)){
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
            }
            quotation.setTaxPrice(taxPrice);
            quotation.setNotTaxPrice(notTaxPrice);
            quotations.add(quotation);

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

    /** 校验是否可以投标 */
    private void checkBid(Vendor vendor, VendorContact vendorContact, BidVO bidVO, TenderNoticeDetailVO detail){
        TenderNotice tenderNotice = detail.getTenderNotice();
        List<TenderNoticeRange> rangeList = detail.getRangeList();
        //判断当前供应商联系人账号是否有效
        if (!VendorContactStateEnum.VALID.equalsState(vendorContact.getState())) {
            throw new ParamValidateException("您当前供应商账户状态不是有效状态，请先设置有效状态再操作");
        }
        if (null != bidVO.getBiddingInfoId()){
            //如果是无限次修改投标清单及文件的话，校验投标截止24小时内不可修改
            //0826问题清单（去掉24小时的限制）
            /*Date nextDay = DateUtils.plusDay(DateUtils.getNowDate(), NumberConstant.ONE);
            if (detail.getBidEndTime() != null){
                if (detail.getBidEndTime().before(nextDay)){
                    throw new ParamValidateException("投标时间截止24小时内不可修改投标");
                }
            } else {
                if (tenderNotice.getApplyTime().before(nextDay)){
                    throw new ParamValidateException("投标时间截止24小时内不可修改投标");
                }
            }*/
        } else {
            long count = biddingInfoService.count(new LambdaQueryWrapper<BiddingInfo>()
                    .eq(BiddingInfo::getVendorId, vendor.getId())
                    .eq(BiddingInfo::getSchemeId, bidVO.getSchemeId())
                    .eq(BiddingInfo::getNoticeId, bidVO.getNoticeId())
                    .isNull(BiddingInfo::getParentId));
            if (count > 0){
                throw new ParamValidateException("不允许重复投标");
            }
        }

        if (detail.getBidEndTime() != null){
            if (detail.getBidEndTime().before(DateUtils.getNowDate())){
                throw new ParamValidateException("投标时间已截止，不允许投标");
            }
        } else {
            if (tenderNotice.getApplyTime().before(DateUtils.getNowDate())){
                throw new ParamValidateException("投标时间已截止，不允许投标");
            }
        }
        if (!TenderNoticeStatusEnum.TENDER_ISSUE.getState().equals(tenderNotice.getNoticeStatus())){
            throw new ParamValidateException("投标公告状态已变更，不允许投标");
        }
        if (tenderNotice.getVendorRange() == 1 && detail.getSchemeType() != NumberConstant.ONE && !CollectionUtils.isEmpty(rangeList)){
            List<Long> vendorIds = rangeList.stream().map(TenderNoticeRange::getVendorId).collect(Collectors.toList());
            if (!vendorIds.contains(vendor.getId())){
                throw new ParamValidateException("不在设置的供应商范围内，不允许投标");
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
                    .orderByDesc(BiddingInfo::getCreateTime).last("limit 1"));
            if (ObjectUtils.isNotEmpty(newestBiddingInfo)){
                biddingInfoRecent = newestBiddingInfo;
            } else {
                biddingInfoRecent = biddingInfoOld;
            }

        }

        BiddingInfo biddingInfo = BeanCopierUtil.copyBean(bidVO, BiddingInfo.class);
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
                checkTaxUnitPrice(biddingInfoRecent, quotationVO, taxUnitPrice);
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
                    if (compMaterialsContentVO.getFloatingPrice() != null || compMaterialsContentVO.getTaxUnitPrice() != null){
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
