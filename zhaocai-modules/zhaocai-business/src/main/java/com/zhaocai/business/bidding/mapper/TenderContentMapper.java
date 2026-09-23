package com.zhaocai.business.bidding.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhaocai.business.bidding.domain.TenderContent;

import java.util.List;

/**
 * 招标内容（已弃用）Mapper接口
 * 
 * @author WH
 * @date 2024-05-24
 */
public interface TenderContentMapper extends BaseMapper<TenderContent>
{
    /**
     * 查询招标内容（已弃用）
     * 
     * @param id 招标内容（已弃用）主键
     * @return 招标内容（已弃用）
     */
    public TenderContent selectTenderContentById(Long id);

    /**
     * 查询招标内容（已弃用）列表
     * 
     * @param tenderContent 招标内容（已弃用）
     * @return 招标内容（已弃用）集合
     */
    public List<TenderContent> selectTenderContentList(TenderContent tenderContent);

    /**
     * 新增招标内容（已弃用）
     * 
     * @param tenderContent 招标内容（已弃用）
     * @return 结果
     */
    public int insertTenderContent(TenderContent tenderContent);

    /**
     * 修改招标内容（已弃用）
     * 
     * @param tenderContent 招标内容（已弃用）
     * @return 结果
     */
    public int updateTenderContent(TenderContent tenderContent);

    /**
     * 删除招标内容（已弃用）
     * 
     * @param id 招标内容（已弃用）主键
     * @return 结果
     */
    public int deleteTenderContentById(Long id);

    /**
     * 批量删除招标内容（已弃用）
     * 
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteTenderContentByIds(Long[] ids);
}
