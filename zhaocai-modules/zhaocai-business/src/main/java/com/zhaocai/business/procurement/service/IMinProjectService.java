package com.zhaocai.business.procurement.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhaocai.business.manager.http.dto.req.MinProjectListRequestDTO;
import com.zhaocai.business.manager.http.dto.res.MinProjectDetailResponseDTO;
import com.zhaocai.business.procurement.domain.MinProject;
import com.zhaocai.business.procurement.vo.res.MinProjectListVO;
import com.zhaocai.business.procurement.vo.res.MinProjectVO;
import com.zhaocai.common.core.bean.PageResult;

import java.util.List;

/**
 * 最小核算项目信息Service接口
 *
 * @author WH
 * @date 2024-07-16
 */
public interface IMinProjectService  extends IService<MinProject> {

    /**
     * 保存最小核算项目信息
     * @param projectDetail
     */
    MinProject saveMinProject(MinProjectDetailResponseDTO projectDetail);

    /**
     * 根据最小核算项目编码获取最小核算项目
     * @param projectCode
     * @return
     */
    MinProjectVO getMinProjectByMinAccountCode(String projectCode);


    List<MinProjectVO> getMinProjectList(String managementOrgId);

    /**
     * 根据查询参数获取最小核算项目列表
     * @param requestDTO
     * @return
     */
    PageResult<MinProjectListVO> getProjectListByQuery(MinProjectListRequestDTO requestDTO);
}
