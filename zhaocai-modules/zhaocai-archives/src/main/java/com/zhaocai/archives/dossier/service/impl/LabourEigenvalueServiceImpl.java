package com.zhaocai.archives.dossier.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhaocai.archives.common.exception.BusinessException;
import com.zhaocai.archives.dossier.domain.LabourDetails;
import com.zhaocai.archives.dossier.domain.LabourEigenvalue;
import com.zhaocai.archives.dossier.domain.LabourItem;
import com.zhaocai.archives.dossier.domain.MaterialType;
import com.zhaocai.archives.dossier.mapper.LabourEigenvalueMapper;
import com.zhaocai.archives.dossier.service.ILabourEigenvalueService;
import com.zhaocai.archives.dossier.service.ILabourItemService;
import com.zhaocai.archives.dossier.service.ILabourTypeService;
import com.zhaocai.archives.main.domain.LaborServicesFeature;
import com.zhaocai.archives.main.domain.LaborServicesFeatureValue;
import com.zhaocai.archives.main.service.ILaborServicesFeatureService;
import com.zhaocai.archives.main.service.ILaborServicesFeatureValueService;
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
 * 劳务特征值Service业务层处理
 *
 * @author lzq
 * @date 2025-01-06
 */
@Service
public class LabourEigenvalueServiceImpl extends ServiceImpl<LabourEigenvalueMapper, LabourEigenvalue> implements ILabourEigenvalueService {
    @Autowired
    private LabourEigenvalueMapper labourEigenvalueMapper;

    @Resource
    private ILaborServicesFeatureValueService iLaborServicesFeatureValueService;

    @Resource
    private ILabourItemService iLabourItemService;

    @Resource
    private ILabourTypeService labourTypeService;


    @Resource
    private ILaborServicesFeatureService iLaborServicesFeatureService;

    /**
     * 查询劳务特征值
     *
     * @param id 劳务特征值主键
     * @return 劳务特征值
     */
    @Override
    public LabourEigenvalue selectLabourEigenvalueById(Long id) {
        LabourEigenvalue labourEigenvalue = labourEigenvalueMapper.selectLabourEigenvalueById(id);
//        if ("N".equals(labourEigenvalue.getIsMain())) {
//            String thStr = labourEigenvalue.getOrganCode().substring(0, 4);
//            String xStr = iLabourItemService.selectLabourItemById(labourEigenvalue.getItemId()).getItemCode().replace(thStr, "");
//            String typeStr = labourTypeService.selectLabourTypeById(labourEigenvalue.getTypeId()).getLabourCode().replace(thStr, "");
//            labourEigenvalue.setEigenvalueCode(typeStr + xStr + labourEigenvalue.getEigenvalueCode() + "-" + thStr);
//        }
        return labourEigenvalue;
    }

    /**
     * 查询劳务特征值列表
     *
     * @param labourEigenvalue 劳务特征值
     * @return 劳务特征值
     */
    @Override
    public List<LabourEigenvalue> selectLabourEigenvalueList(LabourEigenvalue labourEigenvalue) {
        if (labourEigenvalue == null) {
            labourEigenvalue = new LabourEigenvalue();
        }
        labourEigenvalue.setDelFlag("0");
        List<LabourEigenvalue> labourEigenvalues = labourEigenvalueMapper.selectLabourEigenvalueList(labourEigenvalue);
        if (labourEigenvalues != null && !labourEigenvalues.isEmpty()) {
            String thStr = labourEigenvalues.get(0).getOrganCode().substring(0, 4);
            LabourItem item = iLabourItemService.selectLabourItemById(labourEigenvalues.get(0).getItemId());
            String xStr = item.getItemCode().replace("-" + thStr, "");
            String typeStr = labourTypeService.selectLabourTypeById(item.getTypeId()).getLabourCode().replace("-" + thStr, "");
            for (LabourEigenvalue eigenvalue : labourEigenvalues) {
                if ("N".equals(eigenvalue.getIsMain())) {
                    eigenvalue.setEigenvalueCode(typeStr + xStr + eigenvalue.getEigenvalueCode() + "-" + thStr);
                }
            }
        }
        Map<Long, Long> idsMap = new HashMap<>();
        if (MaterialType.ALL.equals(labourEigenvalue.getQueryType()) || MaterialType.UNTREATED.equals(labourEigenvalue.getQueryType())) {
            LabourEigenvalue eigenvalue = new LabourEigenvalue();
            eigenvalue.setState(3L);
            eigenvalue.setDelFlag("0");
            eigenvalue.setIsMain("N");
            eigenvalue.setMainId("0");
            eigenvalue.setTypeId(labourEigenvalue.getTypeId());
            eigenvalue.setOrganCode(labourEigenvalue.getOrganCode());
            List<LabourEigenvalue> list = baseMapper.selectLabourEigenvalueList(eigenvalue);
            list.forEach(item -> {
                idsMap.put(item.getId(), item.getId());
            });
        }
        if (MaterialType.ALL.equals(labourEigenvalue.getQueryType()) || MaterialType.PROCESSED.equals(labourEigenvalue.getQueryType())) {
            List<LabourEigenvalue> eigenvalues1 = baseMapper.getProcessed(labourEigenvalue.getOrganCode());
            eigenvalues1.forEach(item -> {
                idsMap.put(item.getId(), item.getId());
            });
        }
        if (StringUtils.isEmpty(labourEigenvalue.getQueryType())) {
            return labourEigenvalues;
        } else if (labourEigenvalues != null) {
            labourEigenvalues.removeIf(item -> !idsMap.containsKey(item.getId()));
        }
        return labourEigenvalues;
    }


    /**
     * 查询劳务特征值列表
     *
     * @param labourEigenvalue 劳务特征值
     * @return 劳务特征值
     */
    @Override
    public List<LabourEigenvalue> selectLabourEigenvalueListNoChange(LabourEigenvalue labourEigenvalue) {
        return labourEigenvalueMapper.selectLabourEigenvalueList(labourEigenvalue);
    }

    /**
     * 新增劳务特征值
     *
     * @param labourEigenvalue 劳务特征值
     * @return 结果
     */
    @Override
    public synchronized int insertLabourEigenvalue(LabourEigenvalue labourEigenvalue) {
        if (labourEigenvalue != null) {
            if (labourEigenvalue.getItemId() == null) {
                throw new RuntimeException("未获取到特征项id");
            }
            if (StringUtils.isEmpty(labourEigenvalue.getOrganCode())) {
                throw new RuntimeException("未获取到机构代码");
            }
            LabourEigenvalue materialItem1 = new LabourEigenvalue();
            materialItem1.setItemId(labourEigenvalue.getItemId());
            materialItem1.setDelFlag("0");
            materialItem1.setOrganCode(labourEigenvalue.getOrganCode());
            materialItem1.setEigenvalueCode(labourEigenvalue.getEigenvalueCode());
            materialItem1.setIsMain("N");
            List<LabourEigenvalue> materialItems = baseMapper.selectLabourEigenvalueList(materialItem1);
            if (materialItems != null && !materialItems.isEmpty()) {
                throw new RuntimeException("编码已存在");
            }
            if (labourEigenvalue.getId() == null) {
                labourEigenvalue.setId(KeyUtils.generateId());
                labourEigenvalue.setState(0L);
                labourEigenvalue.setCreateId(SecurityUtils.getUserId());
                labourEigenvalue.setCreateBy(SecurityUtils.getUsername());
                labourEigenvalue.setIsMain("N");
                labourEigenvalue.setCreateTime(DateUtils.getNowDate());
            }
        } else {
            throw new RuntimeException("未获取到特征值信息");
        }
        labourEigenvalue.setCreateTime(DateUtils.getNowDate());
        return labourEigenvalueMapper.insertLabourEigenvalue(labourEigenvalue);
    }

    /**
     * 修改劳务特征值
     *
     * @param labourEigenvalue 劳务特征值
     * @return 结果
     */
    @Override
    public int updateLabourEigenvalue(LabourEigenvalue labourEigenvalue) {
        if (labourEigenvalue == null) {
            throw new RuntimeException("未获取到特征值信息");
        }
        if (labourEigenvalue.getItemId() == null) {
            throw new RuntimeException("未获取到特征项id");
        }
        if (StringUtils.isEmpty(labourEigenvalue.getOrganCode())) {
            throw new RuntimeException("未获取到机构代码");
        }
//        String qc = "-"+labourEigenvalue.getOrganCode().substring(0, 4);
//        labourEigenvalue.setEigenvalueCode(labourEigenvalue.getEigenvalueCode().replace(qc, ""));
        LabourEigenvalue materialItem1 = new LabourEigenvalue();
        materialItem1.setTypeId(labourEigenvalue.getTypeId());
        materialItem1.setDelFlag("0");
        materialItem1.setOrganCode(labourEigenvalue.getOrganCode());
        materialItem1.setEigenvalueCode(labourEigenvalue.getEigenvalueCode());
        materialItem1.setIsMain("N");
        List<LabourEigenvalue> materialItems = baseMapper.selectLabourEigenvalueList(materialItem1);
        if (materialItems != null && !materialItems.isEmpty()) {
            for (LabourEigenvalue type : materialItems) {
                if (!type.getId().equals(labourEigenvalue.getId())) {
                    throw new RuntimeException("编码已存在,请刷新后重试");
                }
            }
        }
        labourEigenvalue.setUpdateId(SecurityUtils.getUserId());
        labourEigenvalue.setUpdateBy(SecurityUtils.getUsername());
        labourEigenvalue.setUpdateTime(DateUtils.getNowDate());
        return labourEigenvalueMapper.updateLabourEigenvalue(labourEigenvalue);
    }

    /**
     * 批量删除劳务特征值
     *
     * @param ids 需要删除的劳务特征值主键
     * @return 结果
     */
    @Override
    public boolean deleteLabourEigenvalueByIds(Long[] ids) {
        if (ids == null) {
            throw new RuntimeException("参数为空");
        }
        List<LabourEigenvalue> eigenvalues = labourEigenvalueMapper.selectBatchIds(Arrays.asList(ids));
        for (LabourEigenvalue eigenvalue : eigenvalues) {
            if (eigenvalue.getIsMain().equals("Y")) {
                throw new RuntimeException("主库同步数据不允许删除");
            }
            if (eigenvalue.getState() != 0L) {
                throw new RuntimeException("流程中数据不允许删除");
            }
        }
        return this.removeBatchByIds(Arrays.asList(ids));
    }

    /**
     * 删除劳务特征值信息
     *
     * @param id 劳务特征值主键
     * @return 结果
     */
    @Override
    public int deleteLabourEigenvalueById(Long id) {
        return labourEigenvalueMapper.deleteLabourEigenvalueById(id);
    }

    @Override
    public List<LabourEigenvalue> initData(LabourEigenvalue labourEigenvalue) {
        List<LabourEigenvalue> eigenvalues = labourEigenvalueMapper.selectLabourEigenvalueList(labourEigenvalue);
        List<LaborServicesFeatureValue> mtrFeatureValues = iLaborServicesFeatureValueService.selectLaborServicesFeatureValueList(null);
        LabourItem item1 = new LabourItem();
        item1.setOrganCode(labourEigenvalue.getOrganCode());
        item1.setIsMain("Y");
        List<LabourItem> materialItems = iLabourItemService.selectLabourItemList(item1);
        if (materialItems == null || materialItems.isEmpty()) {
            throw new RuntimeException("请先配置特征项");
        }
        Map<String, LabourItem> params = new HashMap<>();
        materialItems.forEach(item -> {
            if ("Y".equals(item.getIsMain())) {
                params.put(item.getHostId(), item);
            }
        });
        Map<String, LaborServicesFeatureValue> mtrMap = new HashMap<>();
        if (mtrFeatureValues != null && !mtrFeatureValues.isEmpty()) {
            mtrFeatureValues.forEach(mtrFeatureValue -> {
                mtrMap.put(mtrFeatureValue.getId(), mtrFeatureValue);
            });
        }
        if (eigenvalues.isEmpty()) {
            //初始化-最开始无数据情况
            if (mtrFeatureValues != null && !mtrFeatureValues.isEmpty()) {
                mtrFeatureValues.forEach(mtrFeatureValue -> {
                    LabourEigenvalue bean = new LabourEigenvalue();
                    bean.setId(KeyUtils.generateId());
                    bean.setTypeId(params.get(mtrFeatureValue.getLaborServicesFeatureId()) == null ? -1L : params.get(mtrFeatureValue.getLaborServicesFeatureId()).getTypeId());
                    bean.setItemId(params.get(mtrFeatureValue.getLaborServicesFeatureId()) == null ? -1L : params.get(mtrFeatureValue.getLaborServicesFeatureId()).getId());
                    bean.setEigenvalueName(mtrFeatureValue.getFeatureValueName());
                    bean.setEigenvalueCode(mtrFeatureValue.getFeatureValueCode());
                    bean.setCreateId(SecurityUtils.getUserId());
                    bean.setIsMain("Y");
                    bean.setState(3L);
                    bean.setOrganCode(labourEigenvalue.getOrganCode());
                    bean.setCreateBy(SecurityUtils.getUsername());
                    bean.setCreateTime(DateUtils.getNowDate());
                    bean.setHostId(mtrFeatureValue.getId());
                    eigenvalues.add(bean);
                });
                this.saveBatch(eigenvalues);
            }
        } else {
            //有数据的情况，对比主库数据，新增主库存在副库不存在的数据，且非本副库新增至主库数据。（关联数据是否还需要将主库数据同步到副库）
            Map<String, Long> map = new HashMap<>();
            eigenvalues.forEach(item -> {
                if ("Y".equals(item.getIsMain())) {
                    map.put(item.getHostId(), item.getId());
                }
            });
            List<LabourEigenvalue> addList = new ArrayList<>();
            for (String key : mtrMap.keySet()) {
                if (!map.containsKey(key)) {
                    LaborServicesFeatureValue mtrFeatureValue = mtrMap.get(key);
                    LabourEigenvalue bean = new LabourEigenvalue();
                    bean.setId(KeyUtils.generateId());
                    bean.setTypeId(params.get(mtrFeatureValue.getLaborServicesFeatureId()) == null ? -1L : params.get(mtrFeatureValue.getLaborServicesFeatureId()).getTypeId());
                    bean.setItemId(params.get(mtrFeatureValue.getLaborServicesFeatureId()) == null ? -1L : params.get(mtrFeatureValue.getLaborServicesFeatureId()).getId());
                    bean.setEigenvalueName(mtrFeatureValue.getFeatureValueName());
                    bean.setEigenvalueCode(mtrFeatureValue.getFeatureValueCode());
                    bean.setCreateId(SecurityUtils.getUserId());
                    bean.setIsMain("Y");
                    bean.setState(3L);
                    bean.setOrganCode(labourEigenvalue.getOrganCode());
                    bean.setHostId(mtrFeatureValue.getId());
                    bean.setCreateBy(SecurityUtils.getUsername());
                    bean.setCreateTime(DateUtils.getNowDate());
                    eigenvalues.add(bean);
                    addList.add(bean);
                }
            }
            if (!addList.isEmpty()) {
                this.saveBatch(addList);
            }
        }
        return eigenvalues;
    }

    @Override
    public void addTypeByMain(LaborServicesFeatureValue mtrFeatureValue) {
        LabourItem materialItem = new LabourItem();
        materialItem.setHostId(mtrFeatureValue.getLaborServicesFeatureId());
        List<LabourItem> materialItems = iLabourItemService.selectLabourItemList(materialItem);
        //已存在数据排除
        LabourEigenvalue type1 = new LabourEigenvalue();
        type1.setHostId(mtrFeatureValue.getId());
        type1.setDelFlag("0");
        List<LabourEigenvalue> materialTypes1 = baseMapper.selectLabourEigenvalueList(type1);
        Map<String, Long> map = new HashMap<>();
        if (materialTypes1 != null && !materialTypes1.isEmpty()) {
            materialTypes1.forEach(materialType1 -> {
                map.put(materialType1.getOrganCode(), materialType1.getItemId());
            });
        }
        List<LabourEigenvalue> eigenvalues = new ArrayList<>();
        if (materialItems != null && !materialItems.isEmpty()) {
            for (LabourItem item : materialItems) {
                if (map.containsKey(item.getOrganCode()) && (item.getId() + "").equals(map.get(item.getOrganCode()) + "")) {
                    continue;
                }
                LabourEigenvalue bean = new LabourEigenvalue();
                bean.setId(KeyUtils.generateId());
                bean.setTypeId(item.getTypeId());
                bean.setItemId(item.getId());
                bean.setEigenvalueName(mtrFeatureValue.getFeatureValueName());
                bean.setEigenvalueCode(mtrFeatureValue.getFeatureValueCode());
                bean.setCreateId(SecurityUtils.getUserId());
                bean.setIsMain("Y");
                bean.setState(3L);
                bean.setOrganCode(item.getOrganCode());
                bean.setHostId(mtrFeatureValue.getId());
                bean.setCreateBy(SecurityUtils.getUsername());
                bean.setCreateTime(DateUtils.getNowDate());
                eigenvalues.add(bean);
            }
        }
        if (!eigenvalues.isEmpty()) {
            this.saveBatch(eigenvalues);
        }
    }


    @Override
    public void updateByHostId(LaborServicesFeatureValue mtrFeatureValue) {
        LabourEigenvalue eigenvalue = new LabourEigenvalue();
        eigenvalue.setHostId(mtrFeatureValue.getId());
        eigenvalue.setDelFlag("0");
        List<LabourEigenvalue> eigenvalues = baseMapper.selectLabourEigenvalueList(eigenvalue);
        if (eigenvalues != null && !eigenvalues.isEmpty()) {
            eigenvalues.forEach(bean -> {
                bean.setEigenvalueName(mtrFeatureValue.getFeatureValueName());
                bean.setEigenvalueCode(mtrFeatureValue.getFeatureValueCode());
            });
        }
        this.updateBatchById(eigenvalues);
    }

    @Override
    public void deleteByHostId(String[] histIds, Map<Long, Long> idsMap) {
        if (histIds != null) {
            QueryWrapper<LabourEigenvalue> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("del_flag", "0");
            queryWrapper.in("host_id", Arrays.asList(histIds));
            List<LabourEigenvalue> eigenvalues = this.list(queryWrapper);
            //查询新增至主库时不同级或者编码重复的数据
            QueryWrapper<LabourEigenvalue> qw = new QueryWrapper<>();
            qw.eq("del_flag", "0");
            qw.in("main_id", Arrays.asList(histIds));
            qw.eq("is_main","Y");
            eigenvalues.addAll(this.list(qw));
            List<LabourEigenvalue> newDeviceDetails = new ArrayList<>();
            List<LabourEigenvalue> upDeviceDetails = new ArrayList<>();
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
            List<Long> collect = newDeviceDetails.stream().map(LabourEigenvalue::getId).collect(Collectors.toList());
            this.removeBatchByIds(collect);
        }
    }

    /**
     * 新增至主库
     *
     * @param materialEigenvalue
     * @return
     */
    @Override
    public synchronized int addToMain(LabourEigenvalue materialEigenvalue) {
        if (materialEigenvalue.getId() == null) {
            throw new BusinessException("请选择要新增至主库的数据");
        }
        if (materialEigenvalue.getHostId() == null) {
            throw new BusinessException("请选择主库数据");
        }
        LabourEigenvalue materialType1 = this.selectLabourEigenvalueById(materialEigenvalue.getId());
        if ("Y".equals(materialType1.getIsMain())) {
            throw new BusinessException("当前数据已新增至主库，无法再次新增");
        }
        LaborServicesFeature mtrClass = iLaborServicesFeatureService.selectLaborServicesFeatureById(materialEigenvalue.getHostId());
        //查询编码是否已在主库存在
        LaborServicesFeatureValue mtrClass1 = new LaborServicesFeatureValue();
        mtrClass1.setLaborServicesFeatureId(materialEigenvalue.getHostId());
        mtrClass1.setValid(0L);
        mtrClass1.setFeatureValueCode(materialType1.getEigenvalueCode());
        if (!StringUtils.isEmpty(materialEigenvalue.getEigenvalueCode())) {
            mtrClass1.setFeatureValueCode(materialEigenvalue.getEigenvalueCode());
        }
        List<LaborServicesFeatureValue> mtrClasses = iLaborServicesFeatureValueService.selectLaborServicesFeatureValueList(mtrClass1);
        String key = KeyUtils.generateId() + "";
        if (mtrClasses != null && !mtrClasses.isEmpty()) {
            return -1;
        } else {
            if (!StringUtils.isEmpty(materialEigenvalue.getEigenvalueCode())) {
                LaborServicesFeatureValue aClass = new LaborServicesFeatureValue();
                aClass.setId(key);
                aClass.setLaborServicesFeatureId(mtrClass.getId());
                aClass.setFeatureValueCode(materialEigenvalue.getEigenvalueCode());
                aClass.setFeatureValueName(materialType1.getEigenvalueName());
                aClass.setSonId(materialType1.getId());
                aClass.setCreateBy(SecurityUtils.getUsername());
                aClass.setCreateTime(DateUtils.getNowDate());
                aClass.setCreateId(SecurityUtils.getUserId() + "");
                iLaborServicesFeatureValueService.insertLaborServicesFeatureValue(aClass);
                materialType1.setMainId(key);
                materialType1.setIsMain("Y");
                return baseMapper.updateLabourEigenvalue(materialType1);
            } else {
                //不存在时 新增至主库
                materialType1.setIsMain("Y");
                materialType1.setHostId(key);
                int i = baseMapper.updateLabourEigenvalue(materialType1);
                if (i > 0) {
                    LaborServicesFeatureValue aClass = new LaborServicesFeatureValue();
                    aClass.setId(key);
                    aClass.setLaborServicesFeatureId(mtrClass.getId());
                    aClass.setFeatureValueCode(materialType1.getEigenvalueCode());
                    aClass.setFeatureValueName(materialType1.getEigenvalueName());
                    aClass.setSonId(materialType1.getId());
                    aClass.setCreateBy(SecurityUtils.getUsername());
                    aClass.setCreateTime(DateUtils.getNowDate());
                    aClass.setCreateId(SecurityUtils.getUserId() + "");
                    iLaborServicesFeatureValueService.insertLaborServicesFeatureValue(aClass);
                }
                return i;
            }
        }
    }


    /**
     * 关联至主库
     *
     * @param materialEigenvalue
     * @return
     */
    @Override
    public synchronized int associationToMain(LabourEigenvalue materialEigenvalue) {
        if (materialEigenvalue.getId() == null) {
            throw new BusinessException("请选择要关联至主库的数据");
        }
        if (materialEigenvalue.getHostId() == null) {
            throw new BusinessException("请选择主库数据");
        }
        LabourEigenvalue materialType1 = this.selectLabourEigenvalueById(materialEigenvalue.getId());
        if ("Y".equals(materialType1.getIsMain())) {
            throw new BusinessException("当前数据已新增至主库，无法进行关联");
        }
        if (!"0".equals(materialType1.getMainId())) {
            throw new BusinessException("当前数据已关联至主库，无法再次关联");
        }
        materialType1.setMainId(materialEigenvalue.getHostId());
        return baseMapper.updateLabourEigenvalue(materialType1);
    }


    /**
     * 取消关联至主库
     *
     * @param materialEigenvalue
     * @return
     */
    @Override
    public synchronized int unAssociationToMain(LabourEigenvalue materialEigenvalue) {
        if (materialEigenvalue.getId() == null) {
            throw new BusinessException("请选择要取消关联至主库的数据");
        }
        LabourEigenvalue materialType1 = this.selectLabourEigenvalueById(materialEigenvalue.getId());
        if ("Y".equals(materialType1.getIsMain())) {
            throw new BusinessException("当前数据为主库同步数据，无法进行操作");
        }
        if ("0".equals(materialType1.getMainId())) {
            throw new BusinessException("当前数据尚未关联至主库，无需取消关联");
        }
        materialType1.setMainId("0");
        return baseMapper.updateLabourEigenvalue(materialType1);
    }

    @Override
    public long selectLabourEigenvalueListCount(LabourEigenvalue labourEigenvalue) {
        return baseMapper.selectLabourEigenvalueListCount(labourEigenvalue);
    }

    @Override
    public List<LabourEigenvalue> getProcessed(String organCode) {
        return baseMapper.getProcessed(organCode);
    }


}
