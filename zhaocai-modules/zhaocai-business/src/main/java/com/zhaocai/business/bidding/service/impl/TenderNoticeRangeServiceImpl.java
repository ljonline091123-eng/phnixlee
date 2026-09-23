package com.zhaocai.business.bidding.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhaocai.business.bidding.domain.TenderNoticeRange;
import com.zhaocai.business.bidding.mapper.TenderNoticeRangeMapper;
import com.zhaocai.business.bidding.service.ITenderNoticeRangeService;
import com.zhaocai.common.core.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 招标公告供应商范围Service业务层处理
 * 
 * @author WH
 * @date 2024-05-24
 */
@Service
public class TenderNoticeRangeServiceImpl extends ServiceImpl<TenderNoticeRangeMapper,TenderNoticeRange> implements ITenderNoticeRangeService
        {
    @Autowired
    private TenderNoticeRangeMapper tenderNoticeRangeMapper;

    /**
     * 查询招标公告供应商范围
     * 
     * @param id 招标公告供应商范围主键
     * @return 招标公告供应商范围
     */
    @Override
    public TenderNoticeRange selectTenderNoticeRangeById(Long id)
    {
        return tenderNoticeRangeMapper.selectTenderNoticeRangeById(id);
    }

    /**
     * 查询招标公告供应商范围列表
     * 
     * @param tenderNoticeRange 招标公告供应商范围
     * @return 招标公告供应商范围
     */
    @Override
    public List<TenderNoticeRange> selectTenderNoticeRangeList(TenderNoticeRange tenderNoticeRange)
    {
        return tenderNoticeRangeMapper.selectTenderNoticeRangeList(tenderNoticeRange);
    }

    /**
     * 新增招标公告供应商范围
     * 
     * @param tenderNoticeRange 招标公告供应商范围
     * @return 结果
     */
    @Override
    public int insertTenderNoticeRange(TenderNoticeRange tenderNoticeRange)
    {
        tenderNoticeRange.setCreateTime(DateUtils.getNowDate());
        return tenderNoticeRangeMapper.insertTenderNoticeRange(tenderNoticeRange);
    }

    /**
     * 修改招标公告供应商范围
     * 
     * @param tenderNoticeRange 招标公告供应商范围
     * @return 结果
     */
    @Override
    public int updateTenderNoticeRange(TenderNoticeRange tenderNoticeRange)
    {
        tenderNoticeRange.setUpdateTime(DateUtils.getNowDate());
        return tenderNoticeRangeMapper.updateTenderNoticeRange(tenderNoticeRange);
    }

    /**
     * 批量删除招标公告供应商范围
     * 
     * @param ids 需要删除的招标公告供应商范围主键
     * @return 结果
     */
    @Override
    public int deleteTenderNoticeRangeByIds(Long[] ids)
    {
        return tenderNoticeRangeMapper.deleteTenderNoticeRangeByIds(ids);
    }

    /**
     * 删除招标公告供应商范围信息
     * 
     * @param id 招标公告供应商范围主键
     * @return 结果
     */
    @Override
    public int deleteTenderNoticeRangeById(Long id)
    {
        return tenderNoticeRangeMapper.deleteTenderNoticeRangeById(id);
    }
}
