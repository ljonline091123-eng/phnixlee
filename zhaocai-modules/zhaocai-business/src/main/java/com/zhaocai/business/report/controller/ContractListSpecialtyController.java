package com.zhaocai.business.report.controller;

import com.zhaocai.business.common.base.BladeController;
import com.zhaocai.business.report.service.IContractListSpecialtyService;
import io.swagger.annotations.Api;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/contractListSpecialty")
@Api(value = "合同-专业分包合同清单")
public class ContractListSpecialtyController extends BladeController {

    @Autowired
    private IContractListSpecialtyService contractListSpecialtyService;
}
