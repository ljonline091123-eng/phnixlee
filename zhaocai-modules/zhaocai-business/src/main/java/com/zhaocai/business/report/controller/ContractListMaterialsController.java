package com.zhaocai.business.report.controller;

import com.zhaocai.business.common.base.BladeController;
import com.zhaocai.business.report.service.IContractListMaterialsService;
import io.swagger.annotations.Api;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/contractListMaterials")
@Api(value = "合同-购买材料合同清单")
public class ContractListMaterialsController extends BladeController {

    @Autowired
    private IContractListMaterialsService contractListMaterialsService;
}
