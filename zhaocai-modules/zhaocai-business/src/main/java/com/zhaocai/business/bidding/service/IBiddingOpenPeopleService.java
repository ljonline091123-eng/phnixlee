package com.zhaocai.business.bidding.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhaocai.business.bidding.domain.BiddingOpenPeople;
import com.zhaocai.business.bidding.vo.req.BiddingOpenPeopleVO;
import com.zhaocai.business.bidding.vo.req.OpenBidVO;
import com.zhaocai.business.bidding.vo.req.query.BiddingOpenPeopleQueryVO;
import com.zhaocai.business.bidding.vo.res.BiddingOpenPeopleListVO;

import java.util.List;

/**
 * 开标人员信息Service接口
 * 
 * @author WH
 * @date 2024-05-24
 */
public interface IBiddingOpenPeopleService  extends IService<BiddingOpenPeople> {

    /**
     * 新增招标公告答疑
     *
     * @param biddingOpenPeopleVos 开标人员
     * @return 结果
     */
    boolean add(List<BiddingOpenPeopleVO> biddingOpenPeopleVos);

    /**
     * 提交
     *
     * @param noticeId 公告id
     * @return 结果
     */
    boolean submit(Long noticeId);

    /**
     * 查询开标人员信息列表
     *
     * @param queryVO 查询参数
     * @return 结果
     */
    List<BiddingOpenPeopleListVO> getList(BiddingOpenPeopleQueryVO queryVO);


    /**
     * 查询开标人员待办信息列表
     *
     * @param queryVO 查询参数
     * @return 结果
     */
    List<BiddingOpenPeopleListVO> getTodoBiddingOpenList(BiddingOpenPeopleQueryVO queryVO);

    /**
     * 开标人员开标
     *
     * @param openBidVO 开标
     * @return 结果
     */
    boolean openBid(OpenBidVO openBidVO);

}
