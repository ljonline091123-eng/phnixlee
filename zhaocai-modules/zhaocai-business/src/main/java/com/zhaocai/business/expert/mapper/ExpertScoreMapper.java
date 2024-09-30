package com.zhaocai.business.expert.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhaocai.business.bidding.vo.req.EvalVendorCountVO;
import com.zhaocai.business.expert.domain.ExpertScore;
import org.apache.ibatis.annotations.Param;

/**
 * 专家评分Mapper接口
 *
 * @author WH
 * @date 2024-05-24
 */
public interface ExpertScoreMapper extends BaseMapper<ExpertScore> {

    /**
     * 获取未评标的供应商条数
     *
     * @param queryDTO
     * @return
     */
    Integer findNotEvalVendorCount(@Param("queryDTO") EvalVendorCountVO queryDTO);
}
