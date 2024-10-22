package com.zhaocai.business.report.controller;

import com.zhaocai.business.common.base.BladeController;
import com.zhaocai.business.report.service.IContractListLeasedMaterialsService;
import io.swagger.annotations.Api;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/contractListLeasedMaterials")
@Api(value = "合同-租赁材料合同清单")
public class ContractListLeasedMaterialsController extends BladeController {

    @Autowired
    private IContractListLeasedMaterialsService contractListLeasedMaterialsService;
}
