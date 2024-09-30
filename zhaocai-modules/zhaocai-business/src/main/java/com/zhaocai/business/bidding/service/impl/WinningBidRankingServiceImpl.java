package com.zhaocai.business.bidding.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhaocai.business.bidding.domain.WinningBidRanking;
import com.zhaocai.business.bidding.mapper.WinningBidRankingMapper;
import com.zhaocai.business.bidding.service.IWinningBidRankingService;
import com.zhaocai.common.core.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 中标排行榜Service业务层处理
 * 
 * @author WH
 * @date 2024-05-24
 */
@Service
public class WinningBidRankingServiceImpl extends ServiceImpl<WinningBidRankingMapper,WinningBidRanking> implements IWinningBidRankingService
        {
    @Autowired
    private WinningBidRankingMapper winningBidRankingMapper;

    /**
     * 查询中标排行榜
     * 
     * @param id 中标排行榜主键
     * @return 中标排行榜
     */
    @Override
    public WinningBidRanking selectWinningBidRankingById(Long id)
    {
        return winningBidRankingMapper.selectWinningBidRankingById(id);
    }

    /**
     * 查询中标排行榜列表
     * 
     * @param winningBidRanking 中标排行榜
     * @return 中标排行榜
     */
    @Override
    public List<WinningBidRanking> selectWinningBidRankingList(WinningBidRanking winningBidRanking)
    {
        return winningBidRankingMapper.selectWinningBidRankingList(winningBidRanking);
    }

    /**
     * 新增中标排行榜
     * 
     * @param winningBidRanking 中标排行榜
     * @return 结果
     */
    @Override
    public int insertWinningBidRanking(WinningBidRanking winningBidRanking)
    {
        winningBidRanking.setCreateTime(DateUtils.getNowDate());
        return winningBidRankingMapper.insertWinningBidRanking(winningBidRanking);
    }

    /**
     * 修改中标排行榜
     * 
     * @param winningBidRanking 中标排行榜
     * @return 结果
     */
    @Override
    public int updateWinningBidRanking(WinningBidRanking winningBidRanking)
    {
        winningBidRanking.setUpdateTime(DateUtils.getNowDate());
        return winningBidRankingMapper.updateWinningBidRanking(winningBidRanking);
    }

    /**
     * 批量删除中标排行榜
     * 
     * @param ids 需要删除的中标排行榜主键
     * @return 结果
     */
    @Override
    public int deleteWinningBidRankingByIds(Long[] ids)
    {
        return winningBidRankingMapper.deleteWinningBidRankingByIds(ids);
    }

    /**
     * 删除中标排行榜信息
     * 
     * @param id 中标排行榜主键
     * @return 结果
     */
    @Override
    public int deleteWinningBidRankingById(Long id)
    {
        return winningBidRankingMapper.deleteWinningBidRankingById(id);
    }
}
