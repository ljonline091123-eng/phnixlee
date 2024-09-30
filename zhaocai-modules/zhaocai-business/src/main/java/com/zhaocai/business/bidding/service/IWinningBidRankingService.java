package com.zhaocai.business.bidding.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhaocai.business.bidding.domain.WinningBidRanking;

import java.util.List;

/**
 * 中标排行榜Service接口
 * 
 * @author WH
 * @date 2024-05-24
 */
public interface IWinningBidRankingService  extends IService<WinningBidRanking>
{
    /**
     * 查询中标排行榜
     * 
     * @param id 中标排行榜主键
     * @return 中标排行榜
     */
    public WinningBidRanking selectWinningBidRankingById(Long id);

    /**
     * 查询中标排行榜列表
     * 
     * @param winningBidRanking 中标排行榜
     * @return 中标排行榜集合
     */
    public List<WinningBidRanking> selectWinningBidRankingList(WinningBidRanking winningBidRanking);

    /**
     * 新增中标排行榜
     * 
     * @param winningBidRanking 中标排行榜
     * @return 结果
     */
    public int insertWinningBidRanking(WinningBidRanking winningBidRanking);

    /**
     * 修改中标排行榜
     * 
     * @param winningBidRanking 中标排行榜
     * @return 结果
     */
    public int updateWinningBidRanking(WinningBidRanking winningBidRanking);

    /**
     * 批量删除中标排行榜
     * 
     * @param ids 需要删除的中标排行榜主键集合
     * @return 结果
     */
    public int deleteWinningBidRankingByIds(Long[] ids);

    /**
     * 删除中标排行榜信息
     * 
     * @param id 中标排行榜主键
     * @return 结果
     */
    public int deleteWinningBidRankingById(Long id);
}
