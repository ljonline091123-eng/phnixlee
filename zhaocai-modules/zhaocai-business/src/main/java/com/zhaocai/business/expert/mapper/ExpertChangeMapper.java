package com.zhaocai.business.expert.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhaocai.business.expert.domain.Expert;
import com.zhaocai.business.expert.domain.ExpertChange;
import com.zhaocai.business.expert.vo.req.query.ExpertQueryVO;
import com.zhaocai.business.expert.vo.res.ExpertListVO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 专家修改Mapper接口
 *
 * @author WH
 * @date 2024-05-24
 */
public interface ExpertChangeMapper extends BaseMapper<ExpertChange> {

    /**
     * 分页查询专家列表
     *
     * @param queryDTO 查询参数
     * @return 专家集合
     */
    IPage<ExpertListVO> page(Page toMybatisPage, @Param("query") ExpertQueryVO queryDTO);

    List<ExpertListVO> listExpert(@Param("query") ExpertQueryVO queryDTO);

    /**
     * 查询专家
     *
     * @param id 专家主键
     * @return 专家
     */
    public ExpertChange selectExpertChangeById(Long id);

}
