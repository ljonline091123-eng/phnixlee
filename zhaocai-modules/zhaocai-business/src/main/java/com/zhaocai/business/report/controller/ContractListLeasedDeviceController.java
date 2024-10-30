package com.zhaocai.business.report.controller;

import com.zhaocai.business.common.base.BladeController;
import com.zhaocai.business.report.service.IContractListLeasedDeviceService;
import io.swagger.annotations.Api;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/contractListLeasedDevice")
@Api(value = "合同-租赁设备合同清单")
public class ContractListLeasedDeviceController extends BladeController {

    @Autowired
    private IContractListLeasedDeviceService contractListLeasedDeviceService;
}
