package com.zhaocai.business.bidding.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhaocai.business.bidding.domain.TenderApply;

import java.util.List;

/**
 * 招标报名（已弃用）Service接口
 * 
 * @author WH
 * @date 2024-05-24
 */
public interface ITenderApplyService  extends IService<TenderApply>
{
    /**
     * 查询招标报名（已弃用）
     * 
     * @param id 招标报名（已弃用）主键
     * @return 招标报名（已弃用）
     */
    public TenderApply selectTenderApplyById(Long id);

    /**
     * 查询招标报名（已弃用）列表
     * 
     * @param tenderApply 招标报名（已弃用）
     * @return 招标报名（已弃用）集合
     */
    public List<TenderApply> selectTenderApplyList(TenderApply tenderApply);

    /**
     * 新增招标报名（已弃用）
     * 
     * @param tenderApply 招标报名（已弃用）
     * @return 结果
     */
    public int insertTenderApply(TenderApply tenderApply);

    /**
     * 修改招标报名（已弃用）
     * 
     * @param tenderApply 招标报名（已弃用）
     * @return 结果
     */
    public int updateTenderApply(TenderApply tenderApply);

    /**
     * 批量删除招标报名（已弃用）
     * 
     * @param ids 需要删除的招标报名（已弃用）主键集合
     * @return 结果
     */
    public int deleteTenderApplyByIds(Long[] ids);

    /**
     * 删除招标报名（已弃用）信息
     * 
     * @param id 招标报名（已弃用）主键
     * @return 结果
     */
    public int deleteTenderApplyById(Long id);
}
