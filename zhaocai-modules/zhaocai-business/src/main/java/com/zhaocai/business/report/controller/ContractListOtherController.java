package com.zhaocai.business.report.controller;

import com.zhaocai.business.common.base.BladeController;
import com.zhaocai.business.report.service.IContractListOtherService;
import io.swagger.annotations.Api;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/contractListOther")
@Api(value = "合同-其他合同清单")
public class ContractListOtherController extends BladeController {

    @Autowired
    private IContractListOtherService contractListOtherService;
}
