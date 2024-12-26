package com.zhaocai.business.bidding.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhaocai.business.bidding.domain.TenderNotice;
import com.zhaocai.business.bidding.vo.req.TenderNoticeVO;
import com.zhaocai.business.bidding.vo.req.UnderlingTenderNoticeQueryVO;
import com.zhaocai.business.bidding.vo.req.query.*;
import com.zhaocai.business.bidding.vo.res.*;
import com.zhaocai.business.procurement.vo.req.ContractPlanningQueryVO;
import com.zhaocai.common.core.bean.PageResult;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 招标公告Service接口
 *
 * @author WH
 * @date 2024-05-24
 */
public interface ITenderNoticeService  extends IService<TenderNotice> {

    /**
     * 分页查询
     */
    PageResult<TenderNoticeListVO> page(TenderNoticeQueryVO queryDTO);

    /**
     * 发布招标公告
     *
     * @param tenderNoticeVO 招标公告
     * @return 结果
     */
    boolean addNotice(TenderNoticeVO tenderNoticeVO);

    /**
     * 保存报名情况
     *
     * @param tenderNoticeVO 保存报名情况
     * @return 结果
     */
    boolean registerStatus(TenderNoticeVO tenderNoticeVO);


    /**
     * 采购方案 进来的 发布招标文件
     *
     * @param tenderNoticeVO 招标公告文件
     * @return 结果
     */
    boolean add(TenderNoticeVO tenderNoticeVO);

    /**
     * 重新发布招标公告
     *
     * @param tenderNoticeVO 招标公告
     * @return 结果
     */
     boolean aNewAdd(TenderNoticeVO tenderNoticeVO);

    /**
     * 查询已发布的招标公告并按照投标截止时间更改状态
     *
     * @param
     * @return 结果
     */
     boolean handleTenderNoticeIssueStatus();

    /**
     * 查询已公示的招标公告并按照公示期截止时间更改状态
     *
     * @param
     * @return 结果
     */
     boolean handleTenderNoticePublicityStatus();


    List<NoticeBiddingResultListVO> findNoticeBiddingResultList(Long noticeId, Integer noticeStatus, Date date);

    /**
     * 获取投标单详情信息
     *
     * @param schemeId 采购方案id
     * @return 结果
     */
    TenderNoticeDetailVO getInfo(Long schemeId, Long noticeId);

    /**
     * 查询招标公告详情
     *
     * @param id 招标公告主键
     * @return 招标公告
     */
    TenderNoticeDetailVO detail(Long id);

    /**
     * 查询招标公告详情
     *
     * @param id 招标公告主键
     * @return 招标公告
     */
    TenderNoticeSchemeInfoVO getTenderNoticeSchemeInfo(Long id);

    /**
     * 查询招标公告信息
     *
     * @param id 招标公告主键
     * @return 招标公告
     */
    TenderNotice getTenderNotice(Long id);

    /**
     * 查询当前招标公告状态
     *
     * @param id 招标公告主键
     * @return 招标公告
     */
    long getCountTenderNoticeStatus(Long id, Integer noticeStatus);

    /**
     * 展示供应商可查看的招标公告列表数据-分页
     *
     * @param queryDTO
     * @return
     */
    PageResult<VendorNoticeListVO> selectVendorNoticePageNotice(VendorNoticePageQueryVO queryDTO);


    Map<String, Integer> numNotice(VendorNoticePageQueryVO queryDTO);

    /**
     * 展示供应商可查看的招标公告列表数据-分页
     *
     * @param queryDTO
     * @return
     */
    PageResult<VendorNoticeListVO> selectVendorNoticePage(VendorNoticePageQueryVO queryDTO);

    /**
     * 门户端-招标公告列表-分页
     *
     * @param queryDTO
     * @return
     */
    PageResult<VendorPortalNoticeListVO> selectVendorPortalNoticePage(VendorPortalNoticePageQueryVO queryDTO);

    /**
     * 门户端-中标公示列表分页-分页
     *
     * @param queryDTO
     * @return
     */
    PageResult<VendorPortalPublicityListVO> selectVendorPortalPublicityPage(VendorPortalPublicityPageQueryVO queryDTO);

    /**
     * 门户端数据统计接口
     *
     * @param queryDTO
     * @return
     */
    VendorPortalDataStatVO selectVendorPortalDataStat(VendorPortalDataStatQueryVO queryDTO);


    /**
     * 展示供应商可查看的招标公告列表数据-分页
     *
     * @param queryDTO
     * @return
     */
    PageResult<TwiceBidListVO> selectTwiceBidPage(TwiceBidPageQueryVO queryDTO);

    /**
     * 修改状态
     *
     * @param id
     * @param noticeStatus
     * @return
     */
    boolean updateStatus(Long id, Integer noticeStatus);

    PageResult<WinningNotifiListVO> selectWinningNotifiPage(WinningNotifiPageQueryVO queryDTO);

    Integer nextTenderNoticeStatus(Integer schemeType, Integer noticeStatus);

    /**
     * 获取招标公告列表-(第三方-招标公告接口)
     * @param queryVO
     * @return
     */
    PageResult<TenderNoticeVO> listTenderNoticePage(UnderlingTenderNoticeQueryVO queryVO);
    /**
     * 根据 合约规划id 获取 招标对象
     * @param requestDTO
     * @return
     */
    List<ContractPlanningNoticeVO> getListByContractPlanningId(ContractPlanningQueryVO requestDTO);


    PageResult<VendorPortalNoticeListVO> selectVendorPortalNoticePageTwo(VendorPortalNoticePageQueryVO queryDTO);
}
