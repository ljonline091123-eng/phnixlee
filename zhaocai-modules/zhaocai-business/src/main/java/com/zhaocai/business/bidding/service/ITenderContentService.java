package com.zhaocai.business.bidding.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhaocai.business.bidding.domain.TenderContent;

import java.util.List;

/**
 * 招标内容（已弃用）Service接口
 * 
 * @author WH
 * @date 2024-05-24
 */
public interface ITenderContentService  extends IService<TenderContent>
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
     * 批量删除招标内容（已弃用）
     * 
     * @param ids 需要删除的招标内容（已弃用）主键集合
     * @return 结果
     */
    public int deleteTenderContentByIds(Long[] ids);

    /**
     * 删除招标内容（已弃用）信息
     * 
     * @param id 招标内容（已弃用）主键
     * @return 结果
     */
    public int deleteTenderContentById(Long id);
}
