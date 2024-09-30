package com.zhaocai.business.bidding.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhaocai.business.bidding.domain.TenderApply;
import com.zhaocai.business.bidding.mapper.TenderApplyMapper;
import com.zhaocai.business.bidding.service.ITenderApplyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 招标报名（已弃用）Service业务层处理
 * 
 * @author WH
 * @date 2024-05-24
 */
@Service
public class TenderApplyServiceImpl extends ServiceImpl<TenderApplyMapper,TenderApply> implements ITenderApplyService
        {
    @Autowired
    private TenderApplyMapper tenderApplyMapper;

    /**
     * 查询招标报名（已弃用）
     * 
     * @param id 招标报名（已弃用）主键
     * @return 招标报名（已弃用）
     */
    @Override
    public TenderApply selectTenderApplyById(Long id)
    {
        return tenderApplyMapper.selectTenderApplyById(id);
    }

    /**
     * 查询招标报名（已弃用）列表
     * 
     * @param tenderApply 招标报名（已弃用）
     * @return 招标报名（已弃用）
     */
    @Override
    public List<TenderApply> selectTenderApplyList(TenderApply tenderApply)
    {
        return tenderApplyMapper.selectTenderApplyList(tenderApply);
    }

    /**
     * 新增招标报名（已弃用）
     * 
     * @param tenderApply 招标报名（已弃用）
     * @return 结果
     */
    @Override
    public int insertTenderApply(TenderApply tenderApply)
    {
        return tenderApplyMapper.insertTenderApply(tenderApply);
    }

    /**
     * 修改招标报名（已弃用）
     * 
     * @param tenderApply 招标报名（已弃用）
     * @return 结果
     */
    @Override
    public int updateTenderApply(TenderApply tenderApply)
    {
        return tenderApplyMapper.updateTenderApply(tenderApply);
    }

    /**
     * 批量删除招标报名（已弃用）
     * 
     * @param ids 需要删除的招标报名（已弃用）主键
     * @return 结果
     */
    @Override
    public int deleteTenderApplyByIds(Long[] ids)
    {
        return tenderApplyMapper.deleteTenderApplyByIds(ids);
    }

    /**
     * 删除招标报名（已弃用）信息
     * 
     * @param id 招标报名（已弃用）主键
     * @return 结果
     */
    @Override
    public int deleteTenderApplyById(Long id)
    {
        return tenderApplyMapper.deleteTenderApplyById(id);
    }
}
