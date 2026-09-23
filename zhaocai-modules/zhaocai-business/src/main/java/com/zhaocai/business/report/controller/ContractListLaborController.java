package com.zhaocai.business.report.controller;

import com.zhaocai.business.common.base.BladeController;
import com.zhaocai.business.report.service.IContractListLaborService;
import io.swagger.annotations.Api;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/contractListLabor")
@Api(value = "合同-劳务合同清单")
public class ContractListLaborController extends BladeController {

    @Autowired
    private IContractListLaborService contractListLaborService;
}
