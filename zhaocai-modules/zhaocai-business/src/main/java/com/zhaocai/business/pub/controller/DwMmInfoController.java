package com.zhaocai.business.pub.controller;

import com.zhaocai.business.common.base.BladeController;
import com.zhaocai.business.pub.service.IDwMmAssetInfService;
import com.zhaocai.business.pub.service.IDwMmServiceInfService;
import com.zhaocai.business.pub.vo.res.DwMmServiceInfoVO;
import com.zhaocai.common.core.web.bean.ResultData;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;


/**
 * 工机料清单
 *
 * @author chenming
 * @date 2024-08-29
 */
@RestController
@RequestMapping("/dwMmInfo")
@Api(value = "工机料清单")
public class DwMmInfoController extends BladeController {

    @Autowired
    private IDwMmAssetInfService dwMmAssetInfService;

    @Autowired
    private IDwMmServiceInfService dwMmServiceInfService;

    /**
     * 同步 dm071 数据
     */
    @GetMapping("/synchronizeDm071Data")
    @ApiOperation(value = "同步 dm071 数据")
    public ResultData<Boolean> synchronizeDm071Data() {
//        dwMmAssetInfService.synchronizeData();
        return ResultData.data(true);
    }

    /**
     * 同步 dm073 数据
     */
    @GetMapping("/synchronizeDm073Data")
    @ApiOperation(value = "同步 dm073 数据")
    public ResultData<Boolean> synchronizeDm073Data() {
        dwMmServiceInfService.synchronizeData();
        return ResultData.data(true);
    }

    /**
     * 获取服务类数据
     */
    @GetMapping("/listDwMmServiceSubjectMatter")
    public ResultData<List<DwMmServiceInfoVO>> listDwMmServiceSubjectMatter() {
        return ResultData.data(dwMmServiceInfService.listDwMmServiceSubjectMatter());
    }
}
