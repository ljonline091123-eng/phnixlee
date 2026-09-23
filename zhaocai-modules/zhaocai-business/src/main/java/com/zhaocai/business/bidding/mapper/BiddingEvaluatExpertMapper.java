package com.zhaocai.business.bidding.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhaocai.business.bidding.domain.BiddingEvaluatExpert;
import com.zhaocai.business.bidding.vo.req.query.EvalTaskPageVO;
import com.zhaocai.business.bidding.vo.res.EvalTaskPageListVO;
import org.apache.ibatis.annotations.Param;

/**
 * 评标专家人员信息Mapper接口
 *
 * @author WH
 * @date 2024-05-24
 */
public interface BiddingEvaluatExpertMapper extends BaseMapper<BiddingEvaluatExpert> {

    /**
     *
     *
     * @param queryVO 查询参数
     * @return
     */
    IPage<EvalTaskPageListVO> findEvalSchemaPage(Page toMybatisPage, @Param("queryVO") EvalTaskPageVO queryVO);

}
