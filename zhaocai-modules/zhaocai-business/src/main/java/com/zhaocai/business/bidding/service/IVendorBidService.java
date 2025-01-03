package com.zhaocai.business.bidding.service;

import com.zhaocai.business.bidding.vo.req.BidVO;
import com.zhaocai.business.bidding.vo.req.query.TwiceBidPageQueryVO;
import com.zhaocai.business.bidding.vo.req.query.VendorBidPdfFileRequstVO;
import com.zhaocai.business.bidding.vo.req.query.VendorNoticePageQueryVO;
import com.zhaocai.business.bidding.vo.req.query.WinningNotifiPageQueryVO;
import com.zhaocai.business.bidding.vo.res.TenderNoticeDetailVO;
import com.zhaocai.business.bidding.vo.res.TwiceBidListVO;
import com.zhaocai.business.bidding.vo.res.VendorNoticeListVO;
import com.zhaocai.business.bidding.vo.res.WinningNotifiListVO;
import com.zhaocai.business.pub.vo.res.AttachmentVO;
import com.zhaocai.common.core.bean.PageResult;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * @author ssy
 * @date 2024/5/27 18:20
 */
public interface IVendorBidService {

    /**
     * 在线投标报名列表-分页
     *
     * @param queryDTO 在线投标列表查询条件
     * @return 结果
     */
    PageResult<VendorNoticeListVO> pageNotice(VendorNoticePageQueryVO queryDTO);

    /**
     * 在线投标文件列表-分页（投标展示招标公告列表）
     *
     * @param queryDTO 在线投标列表查询条件
     * @return 结果
     */
    PageResult<VendorNoticeListVO> page(VendorNoticePageQueryVO queryDTO);

    /**
     * 把招标文件的转换为pdf文件，返回pdf文件列表
     */
    List<AttachmentVO> getVendorBidPdfFileList(VendorBidPdfFileRequstVO requstVO) throws IOException;

    /**
     * 投标数据详情
     *
     * @param noticeId 招标公告主键id
     * @return
     */
    TenderNoticeDetailVO detail(Long noticeId);

    /**
     * 投标报名 招标报名
     *
     * @param bidVO
     * @return
     */
    boolean bidNotice(BidVO bidVO);

    /**
     * 投标
     *
     * @param bidVO
     * @return
     */
    boolean bid(BidVO bidVO);

    /**
     * 撤回投标
     *
     * @param biddingInfoId
     * @return
     */
    boolean withdrawBid(Long biddingInfoId);

    /**
     * 二次报价列表-分页（投标展示招标公告列表）
     *
     * @param queryDTO 二次报价列表查询条件
     * @return 结果
     */
    PageResult<TwiceBidListVO> twiceBidPage(TwiceBidPageQueryVO queryDTO);

    /**
     * 二次报价
     *
     * @param bidVO
     * @return
     */
    boolean twiceBid(BidVO bidVO);

    PageResult<WinningNotifiListVO> winningNotifiPage(WinningNotifiPageQueryVO queryDTO);

    Map<String, Integer> numNotice(VendorNoticePageQueryVO queryDTO);
}
