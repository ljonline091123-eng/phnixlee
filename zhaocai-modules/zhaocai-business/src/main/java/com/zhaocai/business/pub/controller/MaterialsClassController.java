package com.zhaocai.business.pub.controller;

import com.zhaocai.business.common.base.BladeController;
import com.zhaocai.business.manager.http.service.UnderlingSystemService;
import com.zhaocai.business.pub.vo.req.DeviceQueryVO;
import com.zhaocai.business.pub.vo.req.MaterialsQueryVO;
import com.zhaocai.business.pub.vo.res.*;
import com.zhaocai.common.core.web.bean.ResultData;
import io.swagger.annotations.Api;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;


/**
 * 物资分类
 *
 * @author chenming
 * @date 2024-08-11
 */
@Api(value = "物质类接口")
@RestController
@RequestMapping("/materialsClass")
public class MaterialsClassController extends BladeController {

    @Autowired
    private UnderlingSystemService underlingSystemService;


    /**
     * 获取设备分类
     */
    @GetMapping("/listDeviceClass")
    public ResultData<List<DeviceClassVO>> listDeviceClass() {
        return ResultData.data(underlingSystemService.listDeviceClass());
    }

    /**
     * 设备特征项列表
     */
    @GetMapping("/deviceFeatureList")
    public ResultData<List<DeviceFeatureVO>> deviceFeatureList(DeviceQueryVO queryVO) {
        return ResultData.data(underlingSystemService.deviceFeatureList(queryVO));
    }

    /**
     * 设备特征值列表
     */
    @GetMapping("/deviceFeatureValueList")
    public ResultData<List<DeviceFeatureValueVO>> deviceFeatureValueList(DeviceQueryVO queryVO) {
        return ResultData.data(underlingSystemService.deviceFeatureValueList(queryVO));
    }

    /**
     * 获取物资分类列表
     */
    @GetMapping("/listMaterialsClass")
    public ResultData<List<MaterialsClassVO>> listMaterialsClass() {
        return ResultData.data(underlingSystemService.listMaterialsClass());
    }

    /**
     * 材料特征项列表
     */
    @GetMapping("/listMaterialsFeature")
    public ResultData<List<MaterialsFeatureVO>> listMaterialsFeature(MaterialsQueryVO queryVO) {
        return ResultData.data(underlingSystemService.listMaterialsFeature(queryVO));
    }

    /**
     * 获取材料特征值
     */
    @GetMapping("/listMaterialsFeatureValue")
    public ResultData<List<MaterialsFeatureValueVO>> listMaterialsFeatureValue(MaterialsQueryVO queryVO) {
        return ResultData.data(underlingSystemService.listMaterialsFeatureValue(queryVO));
    }
}
