package com.zhaocai.business.bidding.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhaocai.business.bidding.domain.TenderApply;

import java.util.List;

/**
 * 招标报名（已弃用）Mapper接口
 * 
 * @author WH
 * @date 2024-05-24
 */
public interface TenderApplyMapper extends BaseMapper<TenderApply>
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
     * 删除招标报名（已弃用）
     * 
     * @param id 招标报名（已弃用）主键
     * @return 结果
     */
    public int deleteTenderApplyById(Long id);

    /**
     * 批量删除招标报名（已弃用）
     * 
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteTenderApplyByIds(Long[] ids);
}
