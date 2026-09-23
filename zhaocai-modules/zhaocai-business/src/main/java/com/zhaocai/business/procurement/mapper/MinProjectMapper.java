package com.zhaocai.business.procurement.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhaocai.business.manager.http.dto.req.MinProjectListRequestDTO;
import com.zhaocai.business.procurement.domain.MinProject;
import com.zhaocai.business.procurement.vo.req.ProcurementPlanListQueryVO;
import com.zhaocai.business.procurement.vo.res.ProcurementPlanListVO;
import org.apache.ibatis.annotations.Param;

/**
 * 最小核算项目信息Mapper接口
 *
 * @author WH
 * @date 2024-07-16
 */
public interface MinProjectMapper extends BaseMapper<MinProject> {
    /**
     * 分页查询
     * @param mybatisPage
     * @param requestDTO
     * @return
     */
    IPage<MinProject> selectListPage(Page mybatisPage, @Param("requestDTO") MinProjectListRequestDTO requestDTO);

    /**
     * 通过ID删除项目
     *
     * @param id 用户ID
     * @return 结果
     */
    public int deleteMinProjectById(Long id);

}
