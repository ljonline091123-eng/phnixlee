package com.zhaocai.business.bidding.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhaocai.business.bidding.domain.BiddingItemGrade;

import java.util.List;

/**
 * 评分项分数Mapper接口
 *
 * @author WH
 * @date 2024-06-18
 */
public interface BiddingItemGradeMapper extends BaseMapper<BiddingItemGrade>
{
    /**
     * 查询评分项分数
     *
     * @param id 评分项分数主键
     * @return 评分项分数
     */
    public BiddingItemGrade selectBiddingItemGradeById(Long id);

    /**
     * 查询评分项分数列表
     *
     * @param biddingItemGrade 评分项分数
     * @return 评分项分数集合
     */
    public List<BiddingItemGrade> selectBiddingItemGradeList(BiddingItemGrade biddingItemGrade);

    /**
     * 新增评分项分数
     *
     * @param biddingItemGrade 评分项分数
     * @return 结果
     */
    public int insertBiddingItemGrade(BiddingItemGrade biddingItemGrade);

    /**
     * 修改评分项分数
     *
     * @param biddingItemGrade 评分项分数
     * @return 结果
     */
    public int updateBiddingItemGrade(BiddingItemGrade biddingItemGrade);

    /**
     * 删除评分项分数
     *
     * @param id 评分项分数主键
     * @return 结果
     */
    public int deleteBiddingItemGradeById(Long id);

    /**
     * 批量删除评分项分数
     *
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteBiddingItemGradeByIds(Long[] ids);
}
