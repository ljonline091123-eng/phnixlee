package com.zhaocai.business.bidding.service.impl;

import com.zhaocai.business.bidding.enums.TenderNoticeStatusEnum;
import com.zhaocai.business.bidding.service.ITenderNoticeService;
import com.zhaocai.business.bidding.service.IVendorPortalService;
import com.zhaocai.business.bidding.vo.req.query.VendorPortalDataStatQueryVO;
import com.zhaocai.business.bidding.vo.req.query.VendorPortalNoticePageQueryVO;
import com.zhaocai.business.bidding.vo.req.query.VendorPortalPublicityPageQueryVO;
import com.zhaocai.business.bidding.vo.res.VendorPortalDataStatVO;
import com.zhaocai.business.bidding.vo.res.VendorPortalNoticeListVO;
import com.zhaocai.business.bidding.vo.res.VendorPortalPublicityListVO;
import com.zhaocai.business.vendor.domain.Vendor;
import com.zhaocai.business.vendor.service.IVendorService;
import com.zhaocai.common.core.bean.PageResult;
import com.zhaocai.common.core.utils.DateUtils;
import com.zhaocai.common.security.utils.SecurityUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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

    @Override
    public PageResult<VendorPortalNoticeListVO> getNotice(VendorPortalNoticePageQueryVO queryDTO) {
        queryDTO.setNowDate(DateUtils.getNowDate());
        queryDTO.setNoticeStatus(TenderNoticeStatusEnum.TENDER_ISSUE.getState());
        //设置只查询采购类型为公开招标的
        queryDTO.setSchemeType(1);
        PageResult<VendorPortalNoticeListVO> pageResult = tenderNoticeService.selectVendorPortalNoticePage(queryDTO);
        return pageResult;
    }

    @Override
    public PageResult<VendorPortalNoticeListVO> getNoticeLogin(VendorPortalNoticePageQueryVO queryDTO) {
        queryDTO.setNowDate(DateUtils.getNowDate());
        queryDTO.setNoticeStatus(TenderNoticeStatusEnum.TENDER_ISSUE.getState());
        queryDTO.setVendorId(getVendor(SecurityUtils.getUserId()).getId());
        PageResult<VendorPortalNoticeListVO> pageResult = tenderNoticeService.selectVendorPortalNoticePage(queryDTO);
        return pageResult;
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
