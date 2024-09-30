package com.zhaocai.business.bidding.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhaocai.business.bidding.domain.TenderContent;
import com.zhaocai.business.bidding.mapper.TenderContentMapper;
import com.zhaocai.business.bidding.service.ITenderContentService;
import com.zhaocai.common.core.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 招标内容（已弃用）Service业务层处理
 * 
 * @author WH
 * @date 2024-05-24
 */
@Service
public class TenderContentServiceImpl extends ServiceImpl<TenderContentMapper,TenderContent> implements ITenderContentService
        {
    @Autowired
    private TenderContentMapper tenderContentMapper;

    /**
     * 查询招标内容（已弃用）
     * 
     * @param id 招标内容（已弃用）主键
     * @return 招标内容（已弃用）
     */
    @Override
    public TenderContent selectTenderContentById(Long id)
    {
        return tenderContentMapper.selectTenderContentById(id);
    }

    /**
     * 查询招标内容（已弃用）列表
     * 
     * @param tenderContent 招标内容（已弃用）
     * @return 招标内容（已弃用）
     */
    @Override
    public List<TenderContent> selectTenderContentList(TenderContent tenderContent)
    {
        return tenderContentMapper.selectTenderContentList(tenderContent);
    }

    /**
     * 新增招标内容（已弃用）
     * 
     * @param tenderContent 招标内容（已弃用）
     * @return 结果
     */
    @Override
    public int insertTenderContent(TenderContent tenderContent)
    {
        tenderContent.setCreateTime(DateUtils.getNowDate());
        return tenderContentMapper.insertTenderContent(tenderContent);
    }

    /**
     * 修改招标内容（已弃用）
     * 
     * @param tenderContent 招标内容（已弃用）
     * @return 结果
     */
    @Override
    public int updateTenderContent(TenderContent tenderContent)
    {
        tenderContent.setUpdateTime(DateUtils.getNowDate());
        return tenderContentMapper.updateTenderContent(tenderContent);
    }

    /**
     * 批量删除招标内容（已弃用）
     * 
     * @param ids 需要删除的招标内容（已弃用）主键
     * @return 结果
     */
    @Override
    public int deleteTenderContentByIds(Long[] ids)
    {
        return tenderContentMapper.deleteTenderContentByIds(ids);
    }

    /**
     * 删除招标内容（已弃用）信息
     * 
     * @param id 招标内容（已弃用）主键
     * @return 结果
     */
    @Override
    public int deleteTenderContentById(Long id)
    {
        return tenderContentMapper.deleteTenderContentById(id);
    }
}
