package com.zhaocai.business.pub.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhaocai.business.pub.domain.IndexStatistic;
import com.zhaocai.business.pub.mapper.IndexStatisticMapper;
import com.zhaocai.business.pub.service.IIndexStatisticService;
import com.zhaocai.common.core.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 首页统计数据Service业务层处理
 * 
 * @author WH
 * @date 2024-05-24
 */
@Service
public class IndexStatisticServiceImpl extends ServiceImpl<IndexStatisticMapper,IndexStatistic> implements IIndexStatisticService
        {
    @Autowired
    private IndexStatisticMapper indexStatisticMapper;

    /**
     * 查询首页统计数据
     * 
     * @param id 首页统计数据主键
     * @return 首页统计数据
     */
    @Override
    public IndexStatistic selectIndexStatisticById(Long id)
    {
        return indexStatisticMapper.selectIndexStatisticById(id);
    }

    /**
     * 查询首页统计数据列表
     * 
     * @param indexStatistic 首页统计数据
     * @return 首页统计数据
     */
    @Override
    public List<IndexStatistic> selectIndexStatisticList(IndexStatistic indexStatistic)
    {
        return indexStatisticMapper.selectIndexStatisticList(indexStatistic);
    }

    /**
     * 新增首页统计数据
     * 
     * @param indexStatistic 首页统计数据
     * @return 结果
     */
    @Override
    public int insertIndexStatistic(IndexStatistic indexStatistic)
    {
        indexStatistic.setCreateTime(DateUtils.getNowDate());
        return indexStatisticMapper.insertIndexStatistic(indexStatistic);
    }

    /**
     * 修改首页统计数据
     * 
     * @param indexStatistic 首页统计数据
     * @return 结果
     */
    @Override
    public int updateIndexStatistic(IndexStatistic indexStatistic)
    {
        indexStatistic.setUpdateTime(DateUtils.getNowDate());
        return indexStatisticMapper.updateIndexStatistic(indexStatistic);
    }

    /**
     * 批量删除首页统计数据
     * 
     * @param ids 需要删除的首页统计数据主键
     * @return 结果
     */
    @Override
    public int deleteIndexStatisticByIds(Long[] ids)
    {
        return indexStatisticMapper.deleteIndexStatisticByIds(ids);
    }

    /**
     * 删除首页统计数据信息
     * 
     * @param id 首页统计数据主键
     * @return 结果
     */
    @Override
    public int deleteIndexStatisticById(Long id)
    {
        return indexStatisticMapper.deleteIndexStatisticById(id);
    }
}
