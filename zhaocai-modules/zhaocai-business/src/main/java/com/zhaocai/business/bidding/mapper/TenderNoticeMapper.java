package com.zhaocai.business.bidding.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhaocai.business.bidding.domain.TenderNotice;
import com.zhaocai.business.bidding.vo.req.query.*;
import com.zhaocai.business.bidding.vo.res.*;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

/**
 * 招标公告Mapper接口
 * 
 * @author WH
 * @date 2024-05-24
 */
public interface TenderNoticeMapper extends BaseMapper<TenderNotice> {

    /**
     * 分页查询招标公告列表
     *
     * @param queryDTO 查询参数
     * @return 招标公告集合
     */
    IPage<TenderNoticeListVO> page(Page toMybatisPage, @Param("queryDTO") TenderNoticeQueryVO queryDTO);

    /**
     * 展示供应商可查看的招标公告列表数据-分页
     *
     * @param queryDTO 查询参数
     * @return 招标公告集合
     */
    IPage<VendorNoticeListVO> findVendorNoticePage(Page toMybatisPage, @Param("queryDTO") VendorNoticePageQueryVO queryDTO);

    /**
     * 门户端-招标公告列表-分页
     *
     * @param queryDTO 查询参数
     * @return 招标公告集合
     */
    IPage<VendorPortalNoticeListVO> findVendorPortalNoticePage(Page toMybatisPage, @Param("queryDTO") VendorPortalNoticePageQueryVO queryDTO);

    /**
     * 门户端-中标公示列表分页-分页
     *
     * @param queryDTO 查询参数
     * @return 招标公告集合
     */
    IPage<VendorPortalPublicityListVO> findVendorPortalPublicityPage(Page toMybatisPage, @Param("queryDTO") VendorPortalPublicityPageQueryVO queryDTO);


    IPage<TwiceBidListVO> findTwiceBidPage(Page toMybatisPage, @Param("queryDTO") TwiceBidPageQueryVO queryDTO);

    /**
     * 查询招标公告状态为‘中标公示’的相关的投标结果
     *
     * @param
     * @return 投标结果集合
     */
    List<NoticeBiddingResultListVO> findNoticeBiddingResultList(@Param("noticeId") Long noticeId, @Param("noticeStatus") Integer noticeStatus, @Param("nowDate") Date nowDate);

    IPage<WinningNotifiListVO> findWinningNotifiPage(Page toMybatisPage, @Param("queryVO") WinningNotifiPageQueryVO queryVO);

    TenderNoticeSchemeInfoVO findTenderNoticeSchemeInfo(Long id);

}
