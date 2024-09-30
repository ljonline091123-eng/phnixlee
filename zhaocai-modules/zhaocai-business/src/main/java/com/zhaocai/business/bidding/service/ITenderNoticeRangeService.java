package com.zhaocai.business.bidding.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhaocai.business.bidding.domain.TenderNoticeRange;

import java.util.List;

/**
 * 招标公告供应商范围Service接口
 * 
 * @author WH
 * @date 2024-05-24
 */
public interface ITenderNoticeRangeService  extends IService<TenderNoticeRange>
{
    /**
     * 查询招标公告供应商范围
     * 
     * @param id 招标公告供应商范围主键
     * @return 招标公告供应商范围
     */
    public TenderNoticeRange selectTenderNoticeRangeById(Long id);

    /**
     * 查询招标公告供应商范围列表
     * 
     * @param tenderNoticeRange 招标公告供应商范围
     * @return 招标公告供应商范围集合
     */
    public List<TenderNoticeRange> selectTenderNoticeRangeList(TenderNoticeRange tenderNoticeRange);

    /**
     * 新增招标公告供应商范围
     * 
     * @param tenderNoticeRange 招标公告供应商范围
     * @return 结果
     */
    public int insertTenderNoticeRange(TenderNoticeRange tenderNoticeRange);

    /**
     * 修改招标公告供应商范围
     * 
     * @param tenderNoticeRange 招标公告供应商范围
     * @return 结果
     */
    public int updateTenderNoticeRange(TenderNoticeRange tenderNoticeRange);

    /**
     * 批量删除招标公告供应商范围
     * 
     * @param ids 需要删除的招标公告供应商范围主键集合
     * @return 结果
     */
    public int deleteTenderNoticeRangeByIds(Long[] ids);

    /**
     * 删除招标公告供应商范围信息
     * 
     * @param id 招标公告供应商范围主键
     * @return 结果
     */
    public int deleteTenderNoticeRangeById(Long id);
}
