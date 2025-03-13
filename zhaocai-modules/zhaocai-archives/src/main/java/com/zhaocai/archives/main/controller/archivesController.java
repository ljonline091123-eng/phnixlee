package com.zhaocai.archives.main.controller;

import com.zhaocai.archives.common.exception.BusinessException;
import com.zhaocai.archives.main.domain.DeviceClass;
import com.zhaocai.archives.main.domain.MtrClass;
import com.zhaocai.archives.main.service.IDeviceClassService;
import com.zhaocai.archives.main.service.ILaborServicesClassService;
import com.zhaocai.archives.main.service.IMajorSubcontractingClassService;
import com.zhaocai.archives.main.service.IMtrClassService;
import com.zhaocai.archives.pub.ArchivesTypeEnum;
import com.zhaocai.common.core.web.controller.BaseController;
import com.zhaocai.common.core.web.domain.AjaxResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 设备分类主Controller
 *
 * @author lzq
 * @date 2025-01-06
 */
@RestController
@RequestMapping("/archivesClass")
public class archivesController extends BaseController {
    @Autowired
    private IDeviceClassService deviceClassService;

    @Autowired
    private IMtrClassService mtrClassService;

    @Autowired
    private ILaborServicesClassService laborServicesClassService;

    @Autowired
    private IMajorSubcontractingClassService majorSubcontractingClassService;


    /**
     * 树
     *
     * @return
     */
    @GetMapping("/getArchivesTree")
    public AjaxResult getArchivesTree(String type) {
        if(type.equals(ArchivesTypeEnum.Mtr_Class)){
            MtrClass mtrClass = new MtrClass();
            return success(mtrClassService.getMtrClassTree(mtrClass));
        } else if (type.equals(ArchivesTypeEnum.Device_Feature)) {
            return success(deviceClassService.getDeviceClassTree());
        } else if (type.equals(ArchivesTypeEnum.Labor_Services)) {
            return success(laborServicesClassService.getLaborServicesClassTree());
        } else if (type.equals(ArchivesTypeEnum.Major_Subcontracting)) {
            return success(majorSubcontractingClassService.getMajorSubcontractingClassTree());
        }else {
            throw new BusinessException("该物料类别不存在，请检查！");
        }
    }



}
