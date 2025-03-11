package com.zhaocai.business.procurement.controller;

import com.zhaocai.business.common.base.BladeController;
import com.zhaocai.business.manager.http.dto.req.MinProjectListRequestDTO;
import com.zhaocai.business.procurement.service.IMinProjectService;
import com.zhaocai.business.procurement.vo.res.MinProjectListVO;
import com.zhaocai.business.procurement.vo.res.MinProjectVO;
import com.zhaocai.common.core.bean.PageResult;
import com.zhaocai.common.core.web.bean.ResultData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 最小核算项目 Controller
 *
 * @author chenming
 * @date 2024-07-19
 */
@RestController
@RequestMapping("/minProject")
public class MinProjectController extends BladeController {

    @Autowired
    private IMinProjectService minProjectService;

    @GetMapping("/getMinProject")
    public ResultData<MinProjectVO> getMinProject(@RequestParam String projectCode) {
        return ResultData.data(minProjectService.getMinProjectByMinAccountCode(projectCode));
    }

    @GetMapping("/getMinProjectList")
    public ResultData<List<MinProjectVO>> getMinProjectList(@RequestParam String managementOrgId) {
        return ResultData.data(minProjectService.getMinProjectList(managementOrgId));
    }

    @GetMapping("/getMinProjectListByQuery")
    public ResultData<PageResult<MinProjectListVO>> getProjectListByQuery(MinProjectListRequestDTO requestDTO) {
        return ResultData.data(minProjectService.getProjectListByQuery(requestDTO));
    }

}
