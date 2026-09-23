package com.zhaocai.business.expert.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhaocai.business.expert.domain.ExpertScoreDetail;
import com.zhaocai.business.expert.mapper.ExpertScoreDetailMapper;
import com.zhaocai.business.expert.service.IExpertScoreDetailService;
import com.zhaocai.common.core.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 专家评分明细Service业务层处理
 * 
 * @author WH
 * @date 2024-05-24
 */
@Service
public class ExpertScoreDetailServiceImpl extends ServiceImpl<ExpertScoreDetailMapper,ExpertScoreDetail> implements IExpertScoreDetailService
        {
    @Autowired
    private ExpertScoreDetailMapper expertScoreDetailMapper;

    /**
     * 查询专家评分明细
     * 
     * @param id 专家评分明细主键
     * @return 专家评分明细
     */
    @Override
    public ExpertScoreDetail selectExpertScoreDetailById(Long id)
    {
        return expertScoreDetailMapper.selectExpertScoreDetailById(id);
    }

    /**
     * 查询专家评分明细列表
     * 
     * @param expertScoreDetail 专家评分明细
     * @return 专家评分明细
     */
    @Override
    public List<ExpertScoreDetail> selectExpertScoreDetailList(ExpertScoreDetail expertScoreDetail)
    {
        return expertScoreDetailMapper.selectExpertScoreDetailList(expertScoreDetail);
    }

    /**
     * 新增专家评分明细
     * 
     * @param expertScoreDetail 专家评分明细
     * @return 结果
     */
    @Override
    public int insertExpertScoreDetail(ExpertScoreDetail expertScoreDetail)
    {
        expertScoreDetail.setCreateTime(DateUtils.getNowDate());
        return expertScoreDetailMapper.insertExpertScoreDetail(expertScoreDetail);
    }

    /**
     * 修改专家评分明细
     * 
     * @param expertScoreDetail 专家评分明细
     * @return 结果
     */
    @Override
    public int updateExpertScoreDetail(ExpertScoreDetail expertScoreDetail)
    {
        expertScoreDetail.setUpdateTime(DateUtils.getNowDate());
        return expertScoreDetailMapper.updateExpertScoreDetail(expertScoreDetail);
    }

    /**
     * 批量删除专家评分明细
     * 
     * @param ids 需要删除的专家评分明细主键
     * @return 结果
     */
    @Override
    public int deleteExpertScoreDetailByIds(Long[] ids)
    {
        return expertScoreDetailMapper.deleteExpertScoreDetailByIds(ids);
    }

    /**
     * 删除专家评分明细信息
     * 
     * @param id 专家评分明细主键
     * @return 结果
     */
    @Override
    public int deleteExpertScoreDetailById(Long id)
    {
        return expertScoreDetailMapper.deleteExpertScoreDetailById(id);
    }
}
