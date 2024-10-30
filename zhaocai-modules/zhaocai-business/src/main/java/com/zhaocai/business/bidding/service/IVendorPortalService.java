package com.zhaocai.business.bidding.service;

import com.zhaocai.business.bidding.vo.req.query.VendorPortalDataStatQueryVO;
import com.zhaocai.business.bidding.vo.req.query.VendorPortalNoticePageQueryVO;
import com.zhaocai.business.bidding.vo.req.query.VendorPortalPublicityPageQueryVO;
import com.zhaocai.business.bidding.vo.res.VendorPortalDataStatVO;
import com.zhaocai.business.bidding.vo.res.VendorPortalMsgListVO;
import com.zhaocai.business.bidding.vo.res.VendorPortalNoticeListVO;
import com.zhaocai.business.bidding.vo.res.VendorPortalPublicityListVO;
import com.zhaocai.common.core.bean.PageResult;

import java.util.List;

/**
 * @author ssy
 * @date 2024/6/25 9:52
 */
public interface IVendorPortalService {

    /**
     * 门户端-招标公告列表-分页
     */
    PageResult<VendorPortalNoticeListVO> getNotice(VendorPortalNoticePageQueryVO queryDTO);

    PageResult<VendorPortalNoticeListVO> getNoticeLogin(VendorPortalNoticePageQueryVO queryDTO);

    /**
     * 工作台首页 消息列表
     * @param queryDTO
     * @return
     */
    List<VendorPortalMsgListVO> msgList(VendorPortalNoticePageQueryVO queryDTO);

    /**
     * 门户端-中标公示列表分页-分页
     */
    PageResult<VendorPortalPublicityListVO> getPublicity(VendorPortalPublicityPageQueryVO queryDTO);

    /**
     * 门户端数据统计接口
     */
    VendorPortalDataStatVO dataStat(VendorPortalDataStatQueryVO queryDTO);



}
