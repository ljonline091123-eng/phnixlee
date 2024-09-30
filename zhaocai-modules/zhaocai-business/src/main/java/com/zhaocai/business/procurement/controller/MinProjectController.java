package com.zhaocai.business.procurement.controller;

import com.zhaocai.business.common.base.BladeController;
import com.zhaocai.business.procurement.service.IMinProjectService;
import com.zhaocai.business.procurement.vo.res.MinProjectVO;
import com.zhaocai.common.core.web.bean.ResultData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
}
