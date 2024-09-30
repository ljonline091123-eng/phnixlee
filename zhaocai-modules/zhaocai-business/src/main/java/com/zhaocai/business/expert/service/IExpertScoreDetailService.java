package com.zhaocai.business.expert.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhaocai.business.expert.domain.ExpertScoreDetail;

import java.util.List;

/**
 * 专家评分明细Service接口
 * 
 * @author WH
 * @date 2024-05-24
 */
public interface IExpertScoreDetailService  extends IService<ExpertScoreDetail>
{
    /**
     * 查询专家评分明细
     * 
     * @param id 专家评分明细主键
     * @return 专家评分明细
     */
    public ExpertScoreDetail selectExpertScoreDetailById(Long id);

    /**
     * 查询专家评分明细列表
     * 
     * @param expertScoreDetail 专家评分明细
     * @return 专家评分明细集合
     */
    public List<ExpertScoreDetail> selectExpertScoreDetailList(ExpertScoreDetail expertScoreDetail);

    /**
     * 新增专家评分明细
     * 
     * @param expertScoreDetail 专家评分明细
     * @return 结果
     */
    public int insertExpertScoreDetail(ExpertScoreDetail expertScoreDetail);

    /**
     * 修改专家评分明细
     * 
     * @param expertScoreDetail 专家评分明细
     * @return 结果
     */
    public int updateExpertScoreDetail(ExpertScoreDetail expertScoreDetail);

    /**
     * 批量删除专家评分明细
     * 
     * @param ids 需要删除的专家评分明细主键集合
     * @return 结果
     */
    public int deleteExpertScoreDetailByIds(Long[] ids);

    /**
     * 删除专家评分明细信息
     * 
     * @param id 专家评分明细主键
     * @return 结果
     */
    public int deleteExpertScoreDetailById(Long id);
}
