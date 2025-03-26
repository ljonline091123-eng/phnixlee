package com.zhaocai.archives.main.service.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.zhaocai.archives.common.exception.BusinessException;
import com.zhaocai.archives.main.domain.*;
import com.zhaocai.archives.main.service.*;
import com.zhaocai.archives.main.vo.req.ArchivesDetailQueryVO;
import com.zhaocai.archives.main.vo.res.ArchivesDetail;
import com.zhaocai.archives.pub.ArchivesTypeEnum;
import com.zhaocai.common.core.web.page.PageDomain;
import com.zhaocai.common.core.web.page.TableSupport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 设备分类主Service业务层处理
 *
 * @author lzq
 * @date 2025-01-06
 */
@Service
public class archivesServiceImpl  implements IArchivesService {
    @Autowired
    private IDeviceArchivesService deviceArchivesService;

    @Autowired
    private IMtrArchivesService mtrArchivesService;

    @Autowired
    private ILaborServicesArchivesService laborServicesArchivesService;

    @Autowired
    private IMajorSubcontractingArchivesService majorSubcontractingArchivesService;


    /**
     * 查询物料详情列表-根据不同类别
     *
     * @param
     * @return 设备档案主
     */
    @Override
    public List<ArchivesDetail> getArchivesDetailList(ArchivesDetailQueryVO queryVO) {
//        /** 获取分页*/
//        PageDomain jsonPageDomain = TableSupport.buildPageRequest();
//        Integer  pageNum = jsonPageDomain.getPageNum();
//        Integer  pageSize = jsonPageDomain.getPageSize();
//        if(pageNum==null || pageSize==null){
//            throw new RuntimeException("未获取到分页参数值");
//        }
//        PageHelper.startPage(pageNum,pageSize);
//        PageInfo archivesInfo ;
        List<ArchivesDetail> resultList = new ArrayList<>();
        if(queryVO.getType().equals(ArchivesTypeEnum.Mtr_Class.getType())){
            MtrArchives mtrArchives = new MtrArchives();
            mtrArchives.setMtrClassId(queryVO.getClassId());
            List<MtrArchives> list = mtrArchivesService.selectMtrArchivesList(mtrArchives);
//            archivesInfo = new PageInfo<>(list);
            resultList.addAll(convertMtrArchivesToArchivesDetail(list)); // 调用转换方法
        } else if (queryVO.getType().equals(ArchivesTypeEnum.Device_Feature.getType())) {
            DeviceArchives deviceArchives = new DeviceArchives();
            deviceArchives.setDeviceClassId(queryVO.getClassId());
            List<DeviceArchives> list = deviceArchivesService.selectDeviceArchivesList(deviceArchives);
//            archivesInfo = new PageInfo<>(list);
            resultList.addAll(convertDeviceArchivesToArchivesDetail(list));
        } else if (queryVO.getType().equals(ArchivesTypeEnum.Labor_Services.getType())) {
            LaborServicesArchives laborServicesArchives = new LaborServicesArchives();
            laborServicesArchives.setLaborServicesClassId(queryVO.getClassId());
            List<LaborServicesArchives> list = laborServicesArchivesService.selectLaborServicesArchivesList(laborServicesArchives);
//            archivesInfo = new PageInfo<>(list);
            resultList.addAll(convertLaborServicesArchivesToArchivesDetail(list));
        } else if (queryVO.getType().equals(ArchivesTypeEnum.Major_Subcontracting.getType())) {
            MajorSubcontractingArchives majorSubcontractingArchives = new MajorSubcontractingArchives();
            majorSubcontractingArchives.setMajorSubcontractingClassId(queryVO.getClassId());
            List<MajorSubcontractingArchives> list = majorSubcontractingArchivesService.selectMajorSubcontractingArchivesList(majorSubcontractingArchives);
//            archivesInfo = new PageInfo<>(list);
            resultList.addAll(convertMajorSubcontractingArchivesToArchivesDetail(list));
        }else {
            throw new BusinessException("该物料类别不存在，请检查！");
        }
//        archivesInfo.setList(resultList);
        return resultList;

    }

    // 将 List<MtrArchives> 转换为 List<ArchivesDetail>
    private List<ArchivesDetail> convertMtrArchivesToArchivesDetail(List<MtrArchives> list) {
        List<ArchivesDetail> result = new ArrayList<>();
        for (MtrArchives mtr : list) {
            ArchivesDetail detail = new ArchivesDetail();
            // 将 MtrArchives 的属性映射到 ArchivesDetail
            detail.setId(mtr.getId());
            detail.setMaterialsCode(mtr.getMtrCode());
            detail.setMaterialsName(mtr.getMtrName());
            detail.setUnitMeasurement(mtr.getMeasureUnit());
            detail.setCreateId(mtr.getCreateId());
            detail.setCreateBy(mtr.getCreateBy());
            detail.setCreateTime(mtr.getCreateTime());
            detail.setUpdateBy(mtr.getUpdateBy());
            detail.setUpdateTime(mtr.getUpdateTime());
            detail.setValid(mtr.getValid());
            result.add(detail);
        }
        return result;
    }

    // 将 List<DeviceArchives> 转换为 List<ArchivesDetail>
    private List<ArchivesDetail> convertDeviceArchivesToArchivesDetail(List<DeviceArchives> list) {
        List<ArchivesDetail> result = new ArrayList<>();
        for (DeviceArchives device : list) {
            ArchivesDetail detail = new ArchivesDetail();
            // 将 DeviceArchives 的属性映射到 ArchivesDetail
            detail.setId(device.getId());
            detail.setMaterialsName(device.getDeviceName());
            detail.setMaterialsCode(device.getDeviceCode());
            detail.setSpecification(device.getFeature());
            detail.setUnitMeasurement(device.getMeasureUnit());
            detail.setCreateId(device.getCreateId());
            detail.setCreateBy(device.getCreateBy());
            detail.setCreateTime(device.getCreateTime());
            detail.setUpdateBy(device.getUpdateBy());
            detail.setUpdateTime(device.getUpdateTime());
            detail.setValid(device.getValid());

            result.add(detail);
        }
        return result;
    }

    // 将 List<LaborServicesArchives> 转换为 List<ArchivesDetail>
    private List<ArchivesDetail> convertLaborServicesArchivesToArchivesDetail(List<LaborServicesArchives> list) {
        List<ArchivesDetail> result = new ArrayList<>();
        for (LaborServicesArchives labor : list) {
            ArchivesDetail detail = new ArchivesDetail();
            // 将 LaborServicesArchives 的属性映射到 ArchivesDetail
            detail.setId(labor.getId());
            detail.setMaterialsName(labor.getLaborServicesName());
            detail.setMaterialsCode(labor.getLaborServicesCode());
            detail.setSpecification(labor.getFeature());
            detail.setUnitMeasurement(labor.getMeasureUnit());
            detail.setMeasurementRules(labor.getMetrologicalRules());
            detail.setWorkContent(labor.getBasicJob());
            detail.setCreateId(labor.getCreateId());
            detail.setCreateBy(labor.getCreateBy());
            detail.setCreateTime(labor.getCreateTime());
            detail.setUpdateBy(labor.getUpdateBy());
            detail.setUpdateTime(labor.getUpdateTime());
            detail.setValid(labor.getValid());
            // 其他属性的映射
            result.add(detail);
        }
        return result;
    }

    // 将 List<MajorSubcontractingArchives> 转换为 List<ArchivesDetail>
    private List<ArchivesDetail> convertMajorSubcontractingArchivesToArchivesDetail(List<MajorSubcontractingArchives> list) {
        List<ArchivesDetail> result = new ArrayList<>();
        for (MajorSubcontractingArchives major : list) {
            ArchivesDetail detail = new ArchivesDetail();
            // 将 MajorSubcontractingArchives 的属性映射到 ArchivesDetail
            detail.setId(major.getId());
            detail.setMaterialsName(major.getMajorSubcontractingName());
            detail.setMaterialsCode(major.getMajorSubcontractingCode());
            detail.setSpecification(major.getFeature());
            detail.setUnitMeasurement(major.getMeasureUnit());
            detail.setMeasurementRules(major.getMetrologicalRules());
            detail.setWorkContent(major.getBasicJob());
            detail.setCreateId(major.getCreateId());
            detail.setCreateBy(major.getCreateBy());
            detail.setCreateTime(major.getCreateTime());
            detail.setUpdateBy(major.getUpdateBy());
            detail.setUpdateTime(major.getUpdateTime());
            detail.setValid(major.getValid());
            result.add(detail);
        }
        return result;
    }

}
