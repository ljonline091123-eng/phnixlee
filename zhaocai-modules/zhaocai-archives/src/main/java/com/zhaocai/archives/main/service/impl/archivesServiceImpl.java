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
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

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

    @Autowired
    private IMtrFeatureService mtrFeatureService;

    @Autowired
    private IMtrFeatureValueService mtrFeatureValueService;

    @Autowired
    private IDeviceFeatureService deviceFeatureService;

    @Autowired
    private IDeviceFeatureValueService deviceFeatureValueService;

    @Autowired
    private ILaborServicesFeatureService laborServicesFeatureService;

    @Autowired
    private ILaborServicesFeatureValueService laborServicesFeatureValueService;

    @Autowired
    private IMajorSubcontractingFeatureService majorSubcontractingFeatureService;

    @Autowired
    private IMajorSubcontractingFeatureValueService majorSubcontractingFeatureValueService;


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
        if(queryVO.getClassId() == null || queryVO.getClassId().isEmpty()){
            queryVO.setClassId(Collections.singletonList("-1"));
        }
        List<ArchivesDetail> resultList = new ArrayList<>();
        if(queryVO.getType().equals(ArchivesTypeEnum.Mtr_Class.getType())){
            MtrArchives mtrArchives = new MtrArchives();
            mtrArchives.setIds(queryVO.getClassId());
            List<MtrArchives> list = mtrArchivesService.selectMtrArchivesList(mtrArchives);
//            archivesInfo = new PageInfo<>(list);
            resultList.addAll(convertMtrArchivesToArchivesDetail(list)); // 调用转换方法
        } else if (queryVO.getType().equals(ArchivesTypeEnum.Device_Feature.getType())) {
            DeviceArchives deviceArchives = new DeviceArchives();
            deviceArchives.setIds(queryVO.getClassId());
            List<DeviceArchives> list = deviceArchivesService.selectDeviceArchivesList(deviceArchives);
//            archivesInfo = new PageInfo<>(list);
            resultList.addAll(convertDeviceArchivesToArchivesDetail(list));
        } else if (queryVO.getType().equals(ArchivesTypeEnum.Labor_Services.getType())) {
            LaborServicesArchives laborServicesArchives = new LaborServicesArchives();
            laborServicesArchives.setIds(queryVO.getClassId());
            List<LaborServicesArchives> list = laborServicesArchivesService.selectLaborServicesArchivesList(laborServicesArchives);
//            archivesInfo = new PageInfo<>(list);
            resultList.addAll(convertLaborServicesArchivesToArchivesDetail(list));
        } else if (queryVO.getType().equals(ArchivesTypeEnum.Major_Subcontracting.getType())) {
            MajorSubcontractingArchives majorSubcontractingArchives = new MajorSubcontractingArchives();
            majorSubcontractingArchives.setIds(queryVO.getClassId());
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
            detail.setMaterialsId(mtr.getId());
            detail.setClassId(mtr.getMtrClassId());
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
            detail.setMaterialsId(device.getId());
            detail.setClassId(device.getDeviceClassId());
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
            detail.setMaterialsId(labor.getId());
            detail.setClassId(labor.getLaborServicesClassId());
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
            detail.setMaterialsId(major.getId());
            detail.setClassId(major.getMajorSubcontractingClassId());
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

    @Override
    public void matchList(List<MaterialsVO> list) {
        if(list != null && list.size() > 0){
            Map<String, List<MtrFeature>> mtrFeatureMap = new HashMap<>();
            Map<String, List<MtrFeatureValue>> mtrFeatureValueMap = new HashMap<>();
            Map<String, List<DeviceFeature>> deviceFeatureMap = new HashMap<>();
            Map<String, List<DeviceFeatureValue>> deviceFeatureValueMap = new HashMap<>();
            if("1".equals(list.get(0).getMaterialsUniqueId())){
                MtrFeature mtrFeature = new MtrFeature();
                mtrFeature.setValid(0L);
                List<MtrFeature> mtrFeatures = mtrFeatureService.selectMtrFeatureList(mtrFeature);
                mtrFeatureMap = mtrFeatures.stream().collect(Collectors.groupingBy(MtrFeature::getMtrClassId));
                MtrFeatureValue mtrFeatureValue = new MtrFeatureValue();
                mtrFeatureValue.setValid(0L);
                List<MtrFeatureValue> mtrFeatureValues = mtrFeatureValueService.selectMtrFeatureValueList(mtrFeatureValue);
                mtrFeatureValueMap = mtrFeatureValues.stream().collect(Collectors.groupingBy(MtrFeatureValue::getMtrFeatureId));

            }else if("2".equals(list.get(0).getMaterialsUniqueId())){
                DeviceFeature deviceFeature = new DeviceFeature();
                deviceFeature.setValid(0L);
                List<DeviceFeature> deviceFeatures = deviceFeatureService.selectDeviceFeatureList(deviceFeature);
                deviceFeatureMap = deviceFeatures.stream().collect(Collectors.groupingBy(DeviceFeature::getDeviceClassId));
                DeviceFeatureValue deviceFeatureValue = new DeviceFeatureValue();
                deviceFeatureValue.setValid(0L);
                List<DeviceFeatureValue> deviceFeatureValues = deviceFeatureValueService.selectDeviceFeatureValueList(deviceFeatureValue);
                deviceFeatureValueMap = deviceFeatureValues.stream().collect(Collectors.groupingBy(DeviceFeatureValue::getDeviceFeatureId));

            }
            Map<String, List<MtrFeature>> finalMtrFeatureMap = mtrFeatureMap;
            Map<String, List<MtrFeatureValue>> finalMtrFeatureValueMap = mtrFeatureValueMap;
            Map<String, List<DeviceFeature>> finalDeviceFeatureMap = deviceFeatureMap;
            Map<String, List<DeviceFeatureValue>> finalDeviceFeatureValueMap = deviceFeatureValueMap;
            list.stream().filter(x -> StringUtils.isEmpty(x.getMaterialsId())).forEach(p->{
                String type = p.getMaterialsUniqueId();
                if("1".equals(type)){
                    MtrArchives mtr = new MtrArchives();
                    mtr.setValid(0L);
                    mtr.setMtrName(p.getMaterialsNameImport());
                    List<MtrArchives> mtrArchives = mtrArchivesService.selectMtrArchivesList(mtr);
                    if(mtrArchives !=null && mtrArchives.size() == 1){
                        p.setMaterialsId(mtrArchives.get(0).getId());
                        p.setMaterialsCode(mtrArchives.get(0).getMtrCode());
                        p.setMaterialsName(mtrArchives.get(0).getMtrName());
                        p.setUnitMeasurement(mtrArchives.get(0).getMeasureUnit());
                        String specification = p.getSpecification();
                        if(StringUtils.isNotEmpty(specification)){
                            String[] split = specification.split("-");
                            List<MtrFeature> mtrFeatures = finalMtrFeatureMap.get(mtrArchives.get(0).getMtrClassId());
                            if(mtrFeatures != null && mtrFeatures.size() > 0 && split.length == mtrFeatures.size()){
                                for (int i = 0; i < mtrFeatures.size(); i++) {
                                    List<MtrFeatureValue> mtrFeatureValues = finalMtrFeatureValueMap.get(mtrFeatures.get(i).getId());
                                    List<String> valueList = mtrFeatureValues.stream().map(MtrFeatureValue::getFeatureValueName).collect(Collectors.toList());
                                    if(!valueList.contains(split[i])){
                                        p.setSpecification(null);
                                    }
                                }
                            }else{
                                p.setSpecification(null);
                            }
                        }else{
                            p.setSpecification(null);
                        }

                    }
                }else if("2".equals(type)){
                    DeviceArchives mtr = new DeviceArchives();
                    mtr.setValid(0L);
                    mtr.setDeviceName(p.getMaterialsNameImport());
                    List<DeviceArchives> mtrArchives = deviceArchivesService.selectDeviceArchivesList(mtr);
                    if(mtrArchives !=null && mtrArchives.size() == 1){
                        p.setMaterialsId(mtrArchives.get(0).getId());
                        p.setMaterialsCode(mtrArchives.get(0).getDeviceCode());
                        p.setMaterialsName(mtrArchives.get(0).getDeviceName());
                        p.setUnitMeasurement(mtrArchives.get(0).getMeasureUnit());
                        String specification = p.getSpecification();
                        if(StringUtils.isNotEmpty(specification)){
                            String[] split = specification.split("-");
                            List<DeviceFeature> mtrFeatures = finalDeviceFeatureMap.get(mtrArchives.get(0).getDeviceClassId());
                            if(mtrFeatures != null && mtrFeatures.size() > 0 && split.length == mtrFeatures.size()){
                                //List<String> valueList = new ArrayList<>();
                                for (int i = 0; i < mtrFeatures.size(); i++) {
                                    List<DeviceFeatureValue> mtrFeatureValues = finalDeviceFeatureValueMap.get(mtrFeatures.get(i).getId());
                                    List<String> valueList = mtrFeatureValues.stream().map(DeviceFeatureValue::getFeatureValueName).collect(Collectors.toList());
                                    if(!valueList.contains(split[i])){
                                        p.setSpecification(null);
                                    }
                                }
                            }else{
                                p.setSpecification(null);
                            }
                        }else{
                            p.setSpecification(null);
                        }

                    }
                }else if("3".equals(type)){
                    LaborServicesArchives mtr = new LaborServicesArchives();
                    mtr.setValid(0L);
                    mtr.setLaborServicesName(p.getMaterialsNameImport());
                    List<LaborServicesArchives> mtrArchives = laborServicesArchivesService.selectLaborServicesArchivesList(mtr);
                    if(mtrArchives !=null && mtrArchives.size() == 1){
                        p.setMaterialsId(mtrArchives.get(0).getId());
                        p.setMaterialsCode(mtrArchives.get(0).getLaborServicesCode());
                        p.setMaterialsName(mtrArchives.get(0).getLaborServicesName());
                        p.setUnitMeasurement(mtrArchives.get(0).getMeasureUnit());
                        p.setSpecification(mtrArchives.get(0).getFeature());
                        p.setMeasurementRules(mtrArchives.get(0).getMetrologicalRules());
                        p.setWorkContent(mtrArchives.get(0).getBasicJob());
                    }
                }else if("4".equals(type)){
                    MajorSubcontractingArchives mtr = new MajorSubcontractingArchives();
                    mtr.setValid(0L);
                    mtr.setMajorSubcontractingName(p.getMaterialsNameImport());
                    List<MajorSubcontractingArchives> mtrArchives = majorSubcontractingArchivesService.selectMajorSubcontractingArchivesList(mtr);
                    if(mtrArchives !=null && mtrArchives.size() == 1){
                        p.setMaterialsId(mtrArchives.get(0).getId());
                        p.setMaterialsName(mtrArchives.get(0).getMajorSubcontractingName());
                        p.setMaterialsCode(mtrArchives.get(0).getMajorSubcontractingCode());
                        p.setUnitMeasurement(mtrArchives.get(0).getMeasureUnit());
                        p.setSpecification(mtrArchives.get(0).getFeature());
                        p.setMeasurementRules(mtrArchives.get(0).getMetrologicalRules());
                        p.setWorkContent(mtrArchives.get(0).getBasicJob());
                    }
                }
            });
        }


    }
}
