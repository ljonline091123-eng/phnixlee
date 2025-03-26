package com.zhaocai.archives.main.controller;

import com.github.pagehelper.PageInfo;
import com.zhaocai.archives.common.exception.BusinessException;
import com.zhaocai.archives.main.domain.*;
import com.zhaocai.archives.main.service.*;
import com.zhaocai.archives.main.vo.req.ArchivesDetailQueryVO;
import com.zhaocai.archives.main.vo.res.ArchivesDetail;
import com.zhaocai.archives.pub.ArchivesTypeEnum;
import com.zhaocai.common.core.constant.HttpStatus;
import com.zhaocai.common.core.web.controller.BaseController;
import com.zhaocai.common.core.web.domain.AjaxResult;
import com.zhaocai.common.core.web.page.TableDataInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

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

    @Autowired
    private IArchivesService archivesService;


    /**
     * 树
     *
     * @return
     */
    @GetMapping("/getArchivesTree")
    public AjaxResult getArchivesTree(String type) {
        if(type.equals(ArchivesTypeEnum.Mtr_Class.getType())){
            MtrClass mtrClass = new MtrClass();
            return success(mtrClassService.getMtrClassTree(mtrClass));
        } else if (type.equals(ArchivesTypeEnum.Device_Feature.getType())) {
            return success(deviceClassService.getDeviceClassTree());
        } else if (type.equals(ArchivesTypeEnum.Labor_Services.getType())) {
            return success(laborServicesClassService.getLaborServicesClassTree());
        } else if (type.equals(ArchivesTypeEnum.Major_Subcontracting.getType())) {
            return success(majorSubcontractingClassService.getMajorSubcontractingClassTree());
        }else {
            throw new BusinessException("该物料类别不存在，请检查！");
        }
    }

    @GetMapping("/getArchivesDetailList")
    public AjaxResult getArchivesDetailList(ArchivesDetailQueryVO queryVO) {
//        startPage();
        List<ArchivesDetail> list = archivesService.getArchivesDetailList(queryVO);
//        TableDataInfo rspData = new TableDataInfo();
//        rspData.setCode(HttpStatus.SUCCESS);
//        rspData.setMsg("查询成功");
//        rspData.setRows(list.getList());
//        rspData.setTotal(list.getTotal());
        return success(list);
    }



}
