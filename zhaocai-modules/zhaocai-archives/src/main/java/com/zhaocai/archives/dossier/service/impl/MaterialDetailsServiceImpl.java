package com.zhaocai.archives.dossier.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhaocai.archives.common.exception.BusinessException;
import com.zhaocai.archives.dossier.domain.LabourType;
import com.zhaocai.archives.dossier.domain.MaterialDetails;
import com.zhaocai.archives.dossier.domain.MaterialType;
import com.zhaocai.archives.dossier.mapper.MaterialDetailsMapper;
import com.zhaocai.archives.dossier.service.IMaterialDetailsService;
import com.zhaocai.archives.dossier.service.IMaterialTypeService;
import com.zhaocai.archives.main.domain.MtrArchives;
import com.zhaocai.archives.main.domain.MtrClass;
import com.zhaocai.archives.main.service.IMtrArchivesService;
import com.zhaocai.archives.main.service.IMtrClassService;
import com.zhaocai.archives.utils.KeyUtils;
import com.zhaocai.common.core.utils.DateUtils;
import com.zhaocai.common.core.utils.StringUtils;
import com.zhaocai.common.security.utils.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 材料详情Service业务层处理
 *
 * @author lzq
 * @date 2025-01-06
 */
@Service
public class MaterialDetailsServiceImpl extends ServiceImpl<MaterialDetailsMapper, MaterialDetails> implements IMaterialDetailsService {
    @Autowired
    private MaterialDetailsMapper materialDetailsMapper;

    @Resource
    private IMtrArchivesService iMtrArchivesService;

    @Resource
    private IMaterialTypeService materialTypeService;

    @Resource
    private IMtrClassService mtrClassService;


    /**
     * 查询材料详情
     *
     * @param id 材料详情主键
     * @return 材料详情
     */
    @Override
    public MaterialDetails selectMaterialDetailsById(Long id) {
        MaterialDetails materialDetails = materialDetailsMapper.selectMaterialDetailsById(id);
//        if ("N".equals(materialDetails.getIsMain())) {
//            materialDetails.setMaterialCode(materialDetails.getMaterialCode() + "-" + materialDetails.getOrganCode().substring(0, 4));
//        }
        return materialDetails;
    }


    /**
     * 查询材料详情列表
     *
     * @param materialDetails 材料详情
     * @return 材料详情
     */
    @Override
    public List<MaterialDetails> selectMaterialDetailsListNoChange(MaterialDetails materialDetails) {
        return materialDetailsMapper.selectMaterialDetailsList(materialDetails);
    }


    /**
     * 查询材料详情列表
     *
     * @param materialDetails 材料详情
     * @return 材料详情
     */
    @Override
    public List<MaterialDetails> selectMaterialDetailsList(MaterialDetails materialDetails) {
        if (materialDetails == null) {
            materialDetails = new MaterialDetails();
        }
        materialDetails.setDelFlag("0");
        List<MaterialDetails> materialDetails1 = materialDetailsMapper.selectMaterialDetailsList(materialDetails);
        materialDetails1.forEach(item -> {
            if ("N".equals(item.getIsMain())) {
                item.setMaterialCode(item.getMaterialCode() + "-" + item.getOrganCode().substring(0, 4));
            }
        });
        Map<Long, Long> idsMap = new HashMap<>();
        if (MaterialType.ALL.equals(materialDetails.getQueryType()) || MaterialType.UNTREATED.equals(materialDetails.getQueryType())) {
            //查询未处理的项数据
            MaterialDetails details = new MaterialDetails();
            details.setState(3L);
            details.setDelFlag("0");
            details.setIsMain("N");
            details.setMainId("0");
            details.setOrganCode(materialDetails.getOrganCode());
            List<MaterialDetails> list = baseMapper.selectMaterialDetailsList(details);
            list.forEach(item -> {
                idsMap.put(item.getId(), item.getId());
            });
        }
        if (MaterialType.ALL.equals(materialDetails.getQueryType()) || MaterialType.PROCESSED.equals(materialDetails.getQueryType())) {
            List<MaterialDetails> details = baseMapper.getProcessed(materialDetails.getOrganCode());
            details.forEach(item -> {
                idsMap.put(item.getId(), item.getId());
            });
        }
        if (StringUtils.isEmpty(materialDetails.getQueryType())) {
            return materialDetails1;
        } else {
            materialDetails1.removeIf(item -> !idsMap.containsKey(item.getId()));
        }
        return materialDetails1;
    }

    /**
     * 新增材料详情
     *
     * @param materialDetails 材料详情
     * @return 结果
     */
    @Override
    public synchronized int insertMaterialDetails(MaterialDetails materialDetails) {
        if (materialDetails == null) {
            throw new RuntimeException("参数为空");
        }
        if (StringUtils.isEmpty(materialDetails.getOrganCode())) {
            throw new RuntimeException("机构编码不能为空");
        }
        if (materialDetails.getTypeId() == null) {
            throw new RuntimeException("材料类型不能为空");
        }
        MaterialDetails materialItem1 = new MaterialDetails();
        materialItem1.setTypeId(materialDetails.getTypeId());
        materialItem1.setDelFlag("0");
        materialItem1.setOrganCode(materialDetails.getOrganCode());
        materialItem1.setMaterialCode(materialDetails.getMaterialCode());
        materialItem1.setIsMain("N");
        List<MaterialDetails> materialItems = baseMapper.selectMaterialDetailsList(materialItem1);
        if (materialItems != null && !materialItems.isEmpty()) {
            throw new RuntimeException("特征编码已存在");
        }
        if (materialDetails.getId() == null) {
            materialDetails.setId(KeyUtils.generateId());
            materialDetails.setCreateId(SecurityUtils.getUserId());
            materialDetails.setIsMain("N");
            materialDetails.setState(0L);
            materialDetails.setCreateBy(SecurityUtils.getUsername());
            materialDetails.setCreateTime(DateUtils.getNowDate());
        }
        return materialDetailsMapper.insertMaterialDetails(materialDetails);
    }

    /**
     * 修改材料详情
     *
     * @param materialDetails 材料详情
     * @return 结果
     */
    @Override
    public int updateMaterialDetails(MaterialDetails materialDetails) {
        if (materialDetails == null) {
            throw new RuntimeException("参数为空");
        }
        if (StringUtils.isEmpty(materialDetails.getOrganCode())) {
            throw new RuntimeException("机构编码不能为空");
        }
        if (materialDetails.getTypeId() == null) {
            throw new RuntimeException("材料类型不能为空");
        }
//        String qc = "-"+materialDetails.getOrganCode().substring(0, 4);
//        materialDetails.setMaterialCode(materialDetails.getMaterialCode().replace(qc, ""));
        MaterialDetails materialItem1 = new MaterialDetails();
        materialItem1.setTypeId(materialDetails.getTypeId());
        materialItem1.setDelFlag("0");
        materialItem1.setOrganCode(materialDetails.getOrganCode());
        materialItem1.setMaterialCode(materialDetails.getMaterialCode());
        materialItem1.setIsMain("N");
        List<MaterialDetails> materialItems = baseMapper.selectMaterialDetailsList(materialItem1);
        if (materialItems != null && !materialItems.isEmpty()) {
            for (MaterialDetails type : materialItems) {
                if (!type.getId().equals(materialDetails.getId())) {
                    throw new RuntimeException("编码已存在,请刷新后重试");
                }
            }
        }
        materialDetails.setUpdateId(SecurityUtils.getUserId());
        materialDetails.setUpdateBy(SecurityUtils.getUsername());
        materialDetails.setUpdateTime(DateUtils.getNowDate());
        return materialDetailsMapper.updateMaterialDetails(materialDetails);
    }

    /**
     * 批量删除材料详情
     *
     * @param ids 需要删除的材料详情主键
     * @return 结果
     */
    @Override
    public Boolean deleteMaterialDetailsByIds(Long[] ids) {
        if (ids == null) {
            throw new RuntimeException("参数为空");
        }
        List<MaterialDetails> materialDetails = materialDetailsMapper.selectBatchIds(Arrays.asList(ids));
        if (materialDetails != null && !materialDetails.isEmpty()) {
            for (MaterialDetails materialDetail : materialDetails) {
                if (materialDetail.getIsMain().equals("Y")) {
                    throw new RuntimeException("主库数据不能删除");
                }
                if (materialDetail.getState() != 0L) {
                    throw new RuntimeException("已提交数据不能删除");
                }
            }
        }
        return this.removeBatchByIds(Arrays.asList(ids));
    }

    /**
     * 删除材料详情信息
     *
     * @param id 材料详情主键
     * @return 结果
     */
    @Override
    public int deleteMaterialDetailsById(Long id) {
        return materialDetailsMapper.deleteMaterialDetailsById(id);
    }


    @Override
    public List<MaterialDetails> initData(MaterialDetails materialDetails) {
        List<MaterialDetails> materialDetailsList = materialDetailsMapper.selectMaterialDetailsList(materialDetails);
        List<MtrArchives> mtrArchivesList = iMtrArchivesService.selectMtrArchivesList(null);
        MaterialType type1 = new MaterialType();
        type1.setOrganCode(materialDetails.getOrganCode());
        type1.setIsMain("Y");
        List<MaterialType> materialTypes = materialTypeService.selectMaterialTypeList(type1);
        if (materialTypes == null || materialTypes.isEmpty()) {
            throw new RuntimeException("请先配置材料类型");
        }
        Map<String, MaterialType> params = new HashMap<>();
        materialTypes.forEach(item -> {
            if ("Y".equals(item.getIsMain())) {
                params.put(item.getHostId(), item);
            }
        });
        Map<String, MtrArchives> mtrMap = new HashMap<>();
        if (mtrArchivesList != null && !mtrArchivesList.isEmpty()) {
            mtrArchivesList.forEach(mtrFeature -> {
                mtrMap.put(mtrFeature.getId(), mtrFeature);
            });
        }
        if (materialDetailsList == null || materialDetailsList.isEmpty()) {
            //初始化-最开始无数据情况
            if (mtrArchivesList != null && !mtrArchivesList.isEmpty()) {
                mtrArchivesList.forEach(mtrFeature -> {
                    MaterialDetails bean = new MaterialDetails();
                    bean.setId(KeyUtils.generateId());
                    bean.setTypeId(params.get(mtrFeature.getMtrClassId()) == null ? -1L : params.get(mtrFeature.getMtrClassId()).getId());
                    bean.setTypeName(params.get(mtrFeature.getMtrClassId()) == null ? "" : params.get(mtrFeature.getMtrClassId()).getMaterialName());
                    bean.setMaterialSpecifications(mtrFeature.getSpecs());
                    bean.setUnit(mtrFeature.getMeasureUnit());
                    bean.setMaterialCode(mtrFeature.getMtrCode());
                    bean.setMaterialName(mtrFeature.getMtrName());
                    bean.setCreateId(SecurityUtils.getUserId());
                    bean.setIsMain("Y");
                    bean.setState(3L);
                    bean.setOrganCode(materialDetails.getOrganCode());
                    bean.setHostId(mtrFeature.getId());
                    bean.setCreateBy(SecurityUtils.getUsername());
                    bean.setCreateTime(DateUtils.getNowDate());
                    materialDetailsList.add(bean);
                });
                this.saveBatch(materialDetailsList);
            }
        } else {
            //有数据的情况，对比主库数据，新增主库存在副库不存在的数据，且非本副库新增至主库数据。（关联数据是否还需要将主库数据同步到副库）
            Map<String, Long> items = new HashMap<>();
            materialDetailsList.forEach(item -> {
                if ("Y".equals(item.getIsMain())) {
                    items.put(item.getHostId(), item.getId());
                }
            });
            List<MaterialDetails> addList = new ArrayList<>();
            for (String key : mtrMap.keySet()) {
                if (!items.containsKey(key)) {
                    MtrArchives mtrArchives = mtrMap.get(key);
                    MaterialDetails bean = new MaterialDetails();
                    bean.setId(KeyUtils.generateId());
                    bean.setTypeId(params.get(mtrArchives.getMtrClassId()) == null ? -1L : params.get(mtrArchives.getMtrClassId()).getId());
                    bean.setTypeName(params.get(mtrArchives.getMtrClassId()) == null ? "" : params.get(mtrArchives.getMtrClassId()).getMaterialName());
                    bean.setMaterialSpecifications(mtrArchives.getSpecs());
                    bean.setUnit(mtrArchives.getMeasureUnit());
                    bean.setMaterialCode(mtrArchives.getMtrCode());
                    bean.setMaterialName(mtrArchives.getMtrName());
                    bean.setCreateId(SecurityUtils.getUserId());
                    bean.setIsMain("Y");
                    bean.setState(3L);
                    bean.setOrganCode(materialDetails.getOrganCode());
                    bean.setHostId(mtrArchives.getId());
                    bean.setCreateBy(SecurityUtils.getUsername());
                    bean.setCreateTime(DateUtils.getNowDate());
                    materialDetailsList.add(bean);
                    addList.add(bean);
                }
            }
            if (!addList.isEmpty()) {
                this.saveBatch(addList);
            }
        }
        return materialDetailsList;
    }

    @Override
    public void updateByHostId(MtrArchives mtrArchives) {
        MaterialDetails materialDetails = new MaterialDetails();
        materialDetails.setHostId(mtrArchives.getId());
        materialDetails.setDelFlag("0");
        List<MaterialDetails> materialDetails1 = baseMapper.selectMaterialDetailsList(materialDetails);
        if (materialDetails1 != null && !materialDetails1.isEmpty()) {
            materialDetails1.forEach(bean -> {
                bean.setMaterialSpecifications(mtrArchives.getSpecs());
                bean.setUnit(mtrArchives.getMeasureUnit());
                bean.setMaterialCode(mtrArchives.getMtrCode());
                bean.setMaterialName(mtrArchives.getMtrName());
            });
        }
        this.updateBatchById(materialDetails1);
    }


    @Override
    public void deleteByHostId(String[] histIds, Map<Long, Long> idsMap) {
        if (histIds != null) {
            QueryWrapper<MaterialDetails> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("del_flag", "0");
            queryWrapper.in("host_id", Arrays.asList(histIds));
            List<MaterialDetails> eigenvalues = this.list(queryWrapper);
            //查询新增至主库时不同级或者编码重复的数据
            QueryWrapper<MaterialDetails> qw = new QueryWrapper<>();
            qw.eq("del_flag", "0");
            qw.in("main_id", Arrays.asList(histIds));
            qw.eq("is_main","Y");
            eigenvalues.addAll(this.list(qw));
            List<MaterialDetails> newDeviceDetails = new ArrayList<>();
            List<MaterialDetails> upDeviceDetails = new ArrayList<>();
            if (idsMap != null && !idsMap.isEmpty()) {
                eigenvalues.forEach(item -> {
                    if (idsMap.containsKey(item.getId())) {
                        item.setHostId("0");
                        item.setIsMain("N");
                        item.setMainId("0");
                        upDeviceDetails.add(item);
                    } else {
                        newDeviceDetails.add(item);
                    }
                });
            }
            this.updateBatchById(upDeviceDetails);
            List<Long> collect = newDeviceDetails.stream().map(MaterialDetails::getId).collect(Collectors.toList());
            this.removeBatchByIds(collect);
        }
    }

    @Override
    public void addByMain(MtrArchives mtrArchives) {
        MaterialType materialType = new MaterialType();
        materialType.setHostId(mtrArchives.getMtrClassId());
        List<MaterialType> materialTypes = materialTypeService.selectMaterialTypeList(materialType);
        //已存在数据排除
        MaterialDetails type1 = new MaterialDetails();
        type1.setHostId(mtrArchives.getId());
        type1.setDelFlag("0");
        List<MaterialDetails> materialTypes1 = baseMapper.selectMaterialDetailsList(type1);
        Map<String, Long> map = new HashMap<>();
        if (materialTypes1 != null && !materialTypes1.isEmpty()) {
            materialTypes1.forEach(materialType1 -> {
                map.put(materialType1.getOrganCode(), materialType1.getTypeId());
            });
        }
        List<MaterialDetails> addList = new ArrayList<>();
        if (materialTypes != null && !materialTypes.isEmpty()) {
            for (MaterialType type : materialTypes) {
                if (map.containsKey(type.getOrganCode()) && (type.getId() + "").equals(map.get(type.getOrganCode()) + "")) {
                    continue;
                }
                MaterialDetails bean = new MaterialDetails();
                bean.setId(KeyUtils.generateId());
                bean.setTypeId(type.getId());
                bean.setTypeName(type.getMaterialName());
                bean.setMaterialSpecifications(mtrArchives.getSpecs());
                bean.setUnit(mtrArchives.getMeasureUnit());
                bean.setMaterialCode(mtrArchives.getMtrCode());
                bean.setMaterialName(mtrArchives.getMtrName());
                bean.setCreateId(SecurityUtils.getUserId());
                bean.setIsMain("Y");
                bean.setState(3L);
                bean.setOrganCode(type.getOrganCode());
                bean.setHostId(mtrArchives.getId());
                bean.setCreateBy(SecurityUtils.getUsername());
                bean.setCreateTime(DateUtils.getNowDate());
                addList.add(bean);
            }
        }
        if (!addList.isEmpty()) {
            this.saveBatch(addList);
        }
    }


    /**
     * 新增至主库
     *
     * @param materialDetails
     * @return
     */
    @Override
    public synchronized int addToMain(MaterialDetails materialDetails) {
        if (materialDetails.getId() == null) {
            throw new BusinessException("请选择要新增至主库的数据");
        }
        if (materialDetails.getHostId() == null) {
            throw new BusinessException("请选择主库数据");
        }
        MaterialDetails materialType1 = this.selectMaterialDetailsById(materialDetails.getId());
        if ("Y".equals(materialType1.getIsMain())) {
            throw new BusinessException("当前数据已新增至主库，无法再次新增");
        }
        MtrClass mtrClass = mtrClassService.selectMtrClassById(materialDetails.getHostId());
        //查询编码是否已在主库存在
        MtrArchives mtrClass1 = new MtrArchives();
        mtrClass1.setMtrClassId(materialDetails.getHostId());
        mtrClass1.setValid(0L);
        mtrClass1.setMtrCode(materialType1.getMaterialCode());
        if (!StringUtils.isEmpty(materialDetails.getMaterialCode())) {
            mtrClass1.setMtrCode(materialDetails.getMaterialCode());
        }
        List<MtrArchives> mtrClasses = iMtrArchivesService.selectMtrArchivesList(mtrClass1);
        String key = KeyUtils.generateId() + "";
        if (mtrClasses != null && !mtrClasses.isEmpty()) {
            return -1;
        } else {
            if (!StringUtils.isEmpty(materialDetails.getMaterialCode())) {
                MtrArchives aClass = new MtrArchives();
                aClass.setId(key);
                aClass.setMtrClassId(mtrClass.getId());
                aClass.setMtrCode(materialDetails.getMaterialCode());
                aClass.setMtrName(materialType1.getMaterialName());
                aClass.setSpecs(materialType1.getMaterialSpecifications());
                aClass.setMeasureUnit(materialType1.getUnit());
                aClass.setSonId(materialType1.getId());
                aClass.setCreateBy(SecurityUtils.getUsername());
                aClass.setCreateId(SecurityUtils.getUserId() + "");
                aClass.setCreateTime(DateUtils.getNowDate());
                iMtrArchivesService.insertMtrArchives(aClass);
                materialType1.setMainId(aClass.getId());
                materialType1.setIsMain("Y");
                return baseMapper.updateMaterialDetails(materialType1);
            } else {
                //不存在时 新增至主库
                materialType1.setIsMain("Y");
                materialType1.setHostId(key);
                int i = baseMapper.updateMaterialDetails(materialType1);
                if (i > 0) {
                    MtrArchives aClass = new MtrArchives();
                    aClass.setId(key);
                    aClass.setMtrClassId(mtrClass.getId());
                    aClass.setMtrCode(materialType1.getMaterialCode());
                    aClass.setMtrName(materialType1.getMaterialName());
                    aClass.setSpecs(materialType1.getMaterialSpecifications());
                    aClass.setMeasureUnit(materialType1.getUnit());
                    aClass.setSonId(materialType1.getId());
                    aClass.setCreateBy(SecurityUtils.getUsername());
                    aClass.setCreateId(SecurityUtils.getUserId() + "");
                    aClass.setCreateTime(DateUtils.getNowDate());
                    iMtrArchivesService.insertMtrArchives(aClass);
                }
                return i;
            }
        }
    }


    /**
     * 关联至主库
     *
     * @param materialDetails
     * @return
     */
    @Override
    public synchronized int associationToMain(MaterialDetails materialDetails) {
        if (materialDetails.getId() == null) {
            throw new BusinessException("请选择要关联至主库的数据");
        }
        if (materialDetails.getHostId() == null) {
            throw new BusinessException("请选择主库数据");
        }
        MaterialDetails materialType1 = this.selectMaterialDetailsById(materialDetails.getId());
        if ("Y".equals(materialType1.getIsMain())) {
            throw new BusinessException("当前数据已新增至主库，无法进行关联");
        }
        if (!"0".equals(materialType1.getMainId())) {
            throw new BusinessException("当前数据已关联至主库，无法再次关联");
        }
        materialType1.setMainId(materialDetails.getHostId());
        return baseMapper.updateMaterialDetails(materialType1);
    }


    /**
     * 取消关联至主库
     *
     * @param materialDetails
     * @return
     */
    @Override
    public synchronized int unAssociationToMain(MaterialDetails materialDetails) {
        if (materialDetails.getId() == null) {
            throw new BusinessException("请选择要取消关联至主库的数据");
        }
        MaterialDetails materialType1 = this.selectMaterialDetailsById(materialDetails.getId());
        if ("Y".equals(materialType1.getIsMain())) {
            throw new BusinessException("当前数据为主库同步数据，无法进行操作");
        }
        if ("0".equals(materialType1.getMainId())) {
            throw new BusinessException("当前数据尚未关联至主库，无需取消关联");
        }
        materialType1.setMainId("0");
        return baseMapper.updateMaterialDetails(materialType1);
    }

    @Override
    public long selectMaterialDetailsListCount(MaterialDetails materialDetails) {
        return baseMapper.selectMaterialDetailsListCount(materialDetails);
    }

    @Override
    public List<MaterialDetails> getProcessed(String organCode) {
        return baseMapper.getProcessed(organCode);
    }


}
