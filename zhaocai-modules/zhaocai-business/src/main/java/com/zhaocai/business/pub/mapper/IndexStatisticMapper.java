package com.zhaocai.business.pub.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhaocai.business.pub.domain.IndexStatistic;

import java.util.List;

/**
 * 首页统计数据Mapper接口
 * 
 * @author WH
 * @date 2024-05-24
 */
public interface IndexStatisticMapper extends BaseMapper<IndexStatistic>
{
    /**
     * 查询首页统计数据
     * 
     * @param id 首页统计数据主键
     * @return 首页统计数据
     */
    public IndexStatistic selectIndexStatisticById(Long id);

    /**
     * 查询首页统计数据列表
     * 
     * @param indexStatistic 首页统计数据
     * @return 首页统计数据集合
     */
    public List<IndexStatistic> selectIndexStatisticList(IndexStatistic indexStatistic);

    /**
     * 新增首页统计数据
     * 
     * @param indexStatistic 首页统计数据
     * @return 结果
     */
    public int insertIndexStatistic(IndexStatistic indexStatistic);

    /**
     * 修改首页统计数据
     * 
     * @param indexStatistic 首页统计数据
     * @return 结果
     */
    public int updateIndexStatistic(IndexStatistic indexStatistic);

    /**
     * 删除首页统计数据
     * 
     * @param id 首页统计数据主键
     * @return 结果
     */
    public int deleteIndexStatisticById(Long id);

    /**
     * 批量删除首页统计数据
     * 
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteIndexStatisticByIds(Long[] ids);
}
