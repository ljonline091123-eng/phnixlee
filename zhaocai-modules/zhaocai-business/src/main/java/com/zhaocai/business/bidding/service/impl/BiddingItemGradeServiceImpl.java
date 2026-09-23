package com.zhaocai.business.bidding.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhaocai.business.bidding.domain.BiddingItemGrade;
import com.zhaocai.business.bidding.mapper.BiddingItemGradeMapper;
import com.zhaocai.business.bidding.service.IBiddingItemGradeService;
import com.zhaocai.common.core.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 评分项分数Service业务层处理
 *
 * @author WH
 * @date 2024-06-18
 */
@Service
public class BiddingItemGradeServiceImpl extends ServiceImpl<BiddingItemGradeMapper,BiddingItemGrade> implements IBiddingItemGradeService
        {
    @Autowired
    private BiddingItemGradeMapper biddingItemGradeMapper;

    /**
     * 查询评分项分数
     *
     * @param id 评分项分数主键
     * @return 评分项分数
     */
    @Override
    public BiddingItemGrade selectBiddingItemGradeById(Long id)
    {
        return biddingItemGradeMapper.selectBiddingItemGradeById(id);
    }

    /**
     * 查询评分项分数列表
     *
     * @param biddingItemGrade 评分项分数
     * @return 评分项分数
     */
    @Override
    public List<BiddingItemGrade> selectBiddingItemGradeList(BiddingItemGrade biddingItemGrade)
    {
        return biddingItemGradeMapper.selectBiddingItemGradeList(biddingItemGrade);
    }

    /**
     * 新增评分项分数
     *
     * @param biddingItemGrade 评分项分数
     * @return 结果
     */
    @Override
    public int insertBiddingItemGrade(BiddingItemGrade biddingItemGrade)
    {
        biddingItemGrade.setCreateTime(DateUtils.getNowDate());
        return biddingItemGradeMapper.insertBiddingItemGrade(biddingItemGrade);
    }

    /**
     * 修改评分项分数
     *
     * @param biddingItemGrade 评分项分数
     * @return 结果
     */
    @Override
    public int updateBiddingItemGrade(BiddingItemGrade biddingItemGrade)
    {
        biddingItemGrade.setUpdateTime(DateUtils.getNowDate());
        return biddingItemGradeMapper.updateBiddingItemGrade(biddingItemGrade);
    }

    /**
     * 批量删除评分项分数
     *
     * @param ids 需要删除的评分项分数主键
     * @return 结果
     */
    @Override
    public int deleteBiddingItemGradeByIds(Long[] ids)
    {
        return biddingItemGradeMapper.deleteBiddingItemGradeByIds(ids);
    }

    /**
     * 删除评分项分数信息
     *
     * @param id 评分项分数主键
     * @return 结果
     */
    @Override
    public int deleteBiddingItemGradeById(Long id)
    {
        return biddingItemGradeMapper.deleteBiddingItemGradeById(id);
    }
}
