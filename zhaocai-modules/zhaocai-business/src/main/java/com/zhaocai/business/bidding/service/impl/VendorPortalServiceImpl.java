package com.zhaocai.business.bidding.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhaocai.business.bidding.enums.TenderNoticeStatusEnum;
import com.zhaocai.business.bidding.enums.VendorMsgStatusEnum;
import com.zhaocai.business.bidding.service.ITenderNoticeService;
import com.zhaocai.business.bidding.service.IVendorPortalService;
import com.zhaocai.business.bidding.vo.req.query.VendorPortalDataStatQueryVO;
import com.zhaocai.business.bidding.vo.req.query.VendorPortalNoticePageQueryVO;
import com.zhaocai.business.bidding.vo.req.query.VendorPortalPublicityPageQueryVO;
import com.zhaocai.business.bidding.vo.res.VendorPortalDataStatVO;
import com.zhaocai.business.bidding.vo.res.VendorPortalMsgListVO;
import com.zhaocai.business.bidding.vo.res.VendorPortalNoticeListVO;
import com.zhaocai.business.bidding.vo.res.VendorPortalPublicityListVO;
import com.zhaocai.business.common.cache.DictBizCache;
import com.zhaocai.business.common.enums.CertificationTypeEnum;
import com.zhaocai.business.common.enums.DictBizEnum;
import com.zhaocai.business.vendor.domain.Vendor;
import com.zhaocai.business.vendor.domain.VendorCertification;
import com.zhaocai.business.vendor.service.IVendorCertificationService;
import com.zhaocai.business.vendor.service.IVendorContactService;
import com.zhaocai.business.vendor.service.IVendorService;
import com.zhaocai.business.vendor.vo.res.VendorCertificationListVO;
import com.zhaocai.business.vendor.vo.res.VendorMainContactVO;
import com.zhaocai.common.core.bean.PageResult;
import com.zhaocai.common.core.utils.DateUtils;
import com.zhaocai.common.core.web.bean.ResultData;
import com.zhaocai.common.security.utils.SecurityUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * @author ssy
 * @date 2024/6/25 9:53
 */
@Slf4j
@Service
public class VendorPortalServiceImpl implements IVendorPortalService {

    @Autowired
    private ITenderNoticeService tenderNoticeService;
    @Autowired
    private IVendorService vendorService;
    @Autowired
    private IVendorContactService vendorContactService;
    @Autowired
    private IVendorCertificationService vendorCertificationService;

    @Override
    public PageResult<VendorPortalNoticeListVO> getNotice(VendorPortalNoticePageQueryVO queryDTO) {
        queryDTO.setNowDate(DateUtils.getNowDate());
        //设置只查询采购类型为公开招标的
        queryDTO.setSchemeType(1);
        PageResult<VendorPortalNoticeListVO> pageResult = tenderNoticeService.selectVendorPortalNoticePage(queryDTO);
        return pageResult;
    }

    @Override
    public PageResult<VendorPortalNoticeListVO> getNoticeLogin(VendorPortalNoticePageQueryVO queryDTO) {
        queryDTO.setNowDate(DateUtils.getNowDate());
        queryDTO.setVendorId(getVendor(SecurityUtils.getUserId()).getId());
        PageResult<VendorPortalNoticeListVO> pageResult = tenderNoticeService.selectVendorPortalNoticePage(queryDTO);
        return pageResult;
    }

    @Override
    public List<VendorPortalMsgListVO>  msgList(VendorPortalNoticePageQueryVO queryDTO) {
        queryDTO.setNowDate(DateUtils.getNowDate());
//        queryDTO.setVendorId(getVendor(SecurityUtils.getUserId()).getId());
        /* 获取供应商信息 */
        Vendor vendor = getVendor(SecurityUtils.getUserId());
        queryDTO.setVendorId(vendor.getId());
        queryDTO.setRegisterApprovalTime(vendor.getRegisterApprovalTime());
        PageResult<VendorPortalNoticeListVO> pageResult = tenderNoticeService.selectVendorPortalNoticePage(queryDTO);
        List<VendorPortalMsgListVO> list = new ArrayList<>();
        if(pageResult!=null && pageResult.getTotal()>0){
            for (int i = 0; i < pageResult.getRows().size(); i++) {
                list.add(VendorPortalMsgListVO.builder()
                        .data(pageResult.getRows().get(i))
                        .title(pageResult.getRows().get(i).getProcurementSchemeName())
                        .msgType(VendorMsgStatusEnum.TENDER.getState())
                        .msgTypeText(DictBizCache.getValue(DictBizEnum.PROCUREMENT_PLAN_TYPE,pageResult.getRows().get(i).getProcurementPlanType().toString()))
                        .build());
            }
        }
        /* 获取供应商信息 */
//        Vendor vendor = vendorService.getByLoginUser(SecurityUtils.getUserId());
        /* 获取该企业的 法人授权书 */
        Date date30 = DateUtils.plusDay(new Date(),30);
        List<VendorCertification> attachments = vendorCertificationService.list(new LambdaQueryWrapper<VendorCertification>()
                .eq(VendorCertification::getVendorId,vendor.getId())/* 该供应商名下 */
                .eq(VendorCertification::getDelFlag,0)/* 有效 */
                .lt(VendorCertification::getEffectiveEndDate,date30)/* 小于30天 */
                .eq(VendorCertification::getBusinessCode,CertificationTypeEnum.LEGAL_AUTHORIZATION.getType()));/* 法人授权书 */

        if(attachments!=null && attachments.size()>0){
            for (int i = 0; i < attachments.size(); i++) {
                list.add(VendorPortalMsgListVO.builder()
                        .data(attachments.get(i))
                        .title("法人授权书将于"+DateUtils.dateTime(attachments.get(i).getEffectiveEndDate())+"过期，届时不能进行投标工作")
                        .msgType(VendorMsgStatusEnum.CERT.getState())
                        .msgTypeText(VendorMsgStatusEnum.CERT.getDesc())
                        .build());
            }
        }

        return list;
    }

    @Override
    public PageResult<VendorPortalPublicityListVO> getPublicity(VendorPortalPublicityPageQueryVO queryDTO) {
        queryDTO.setNoticeStatus(TenderNoticeStatusEnum.WINNING_BID.getState());
        PageResult<VendorPortalPublicityListVO> pageResult = tenderNoticeService.selectVendorPortalPublicityPage(queryDTO);
        return pageResult;
    }

    @Override
    public VendorPortalDataStatVO dataStat(VendorPortalDataStatQueryVO queryDTO) {
        return tenderNoticeService.selectVendorPortalDataStat(queryDTO);
    }



    private Vendor getVendor(Long userId){
        return vendorService.getByLoginUser(userId);
    }


}
