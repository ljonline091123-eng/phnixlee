package com.zhaocai.archives.dossier.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhaocai.archives.common.exception.BusinessException;
import com.zhaocai.archives.dossier.domain.MaterialDetails;
import com.zhaocai.archives.dossier.domain.MaterialEigenvalue;
import com.zhaocai.archives.dossier.domain.MaterialItem;
import com.zhaocai.archives.dossier.domain.MaterialType;
import com.zhaocai.archives.dossier.mapper.MaterialEigenvalueMapper;
import com.zhaocai.archives.dossier.service.IMaterialEigenvalueService;
import com.zhaocai.archives.dossier.service.IMaterialItemService;
import com.zhaocai.archives.dossier.service.IMaterialTypeService;
import com.zhaocai.archives.main.domain.MtrFeature;
import com.zhaocai.archives.main.domain.MtrFeatureValue;
import com.zhaocai.archives.main.service.IMtrFeatureService;
import com.zhaocai.archives.main.service.IMtrFeatureValueService;
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
 * 材料特征值Service业务层处理
 *
 * @author lzq
 * @date 2025-01-06
 */
@Service
public class MaterialEigenvalueServiceImpl extends ServiceImpl<MaterialEigenvalueMapper, MaterialEigenvalue> implements IMaterialEigenvalueService {
    @Autowired
    private MaterialEigenvalueMapper materialEigenvalueMapper;

    @Resource
    private IMaterialItemService iMaterialItemService;

    @Resource
    private IMaterialTypeService materialTypeService;

    @Resource
    private IMtrFeatureValueService iMtrFeatureValueService;

    @Resource
    private IMtrFeatureService iMtrFeatureService;

    /**
     * 查询材料特征值
     *
     * @param id 材料特征值主键
     * @return 材料特征值
     */
    @Override
    public MaterialEigenvalue selectMaterialEigenvalueById(Long id) {
        return materialEigenvalueMapper.selectMaterialEigenvalueById(id);
    }

    /**
     * 查询材料特征值列表
     *
     * @param materialEigenvalue 材料特征值
     * @return 材料特征值
     */
    @Override
    public List<MaterialEigenvalue> selectMaterialEigenvalueListNoChange(MaterialEigenvalue materialEigenvalue) {
        return materialEigenvalueMapper.selectMaterialEigenvalueList(materialEigenvalue);
    }

    /**
     * 查询材料特征值列表
     *
     * @param materialEigenvalue 材料特征值
     * @return 材料特征值
     */
    @Override
    public List<MaterialEigenvalue> selectMaterialEigenvalueList(MaterialEigenvalue materialEigenvalue) {
        if (materialEigenvalue == null) {
            materialEigenvalue = new MaterialEigenvalue();
        }
        materialEigenvalue.setDelFlag("0");
        List<MaterialEigenvalue> eigenvalues = materialEigenvalueMapper.selectMaterialEigenvalueList(materialEigenvalue);
        if (eigenvalues != null && !eigenvalues.isEmpty()) {
            String thStr = eigenvalues.get(0).getOrganCode().substring(0, 4);
            MaterialItem item = iMaterialItemService.selectMaterialItemById(eigenvalues.get(0).getItemId());
            String xStr = item.getItemCode().replace("-" + thStr, "");
            String typeStr = materialTypeService.selectMaterialTypeById(item.getTypeId()).getMaterialCode().replace("-" + thStr, "");
            for (MaterialEigenvalue eigenvalue : eigenvalues) {
                if ("N".equals(eigenvalue.getIsMain())) {
                    eigenvalue.setEigenvalueCode(typeStr + xStr + eigenvalue.getEigenvalueCode() + "-" + thStr);
                }
            }
        }
        Map<Long, Long> idsMap = new HashMap<>();
        if (MaterialType.ALL.equals(materialEigenvalue.getQueryType()) || MaterialType.UNTREATED.equals(materialEigenvalue.getQueryType())) {
            MaterialEigenvalue eigenvalue = new MaterialEigenvalue();
            eigenvalue.setState(3L);
            eigenvalue.setDelFlag("0");
            eigenvalue.setIsMain("N");
            eigenvalue.setMainId("0");
            eigenvalue.setTypeId(materialEigenvalue.getTypeId());
            eigenvalue.setOrganCode(materialEigenvalue.getOrganCode());
            List<MaterialEigenvalue> list = baseMapper.selectMaterialEigenvalueList(eigenvalue);
            list.forEach(item -> {
                idsMap.put(item.getId(), item.getId());
            });
        }
        if (MaterialType.ALL.equals(materialEigenvalue.getQueryType()) || MaterialType.PROCESSED.equals(materialEigenvalue.getQueryType())) {
            List<MaterialEigenvalue> eigenvalues1 = baseMapper.getProcessed(materialEigenvalue.getOrganCode());
            eigenvalues1.forEach(item -> {
                idsMap.put(item.getId(), item.getId());
            });
        }
        if (StringUtils.isEmpty(materialEigenvalue.getQueryType())) {
            return eigenvalues;
        } else if (eigenvalues != null) {
            eigenvalues.removeIf(item -> !idsMap.containsKey(item.getId()));
        }
        return eigenvalues;
    }

    /**
     * 新增材料特征值
     *
     * @param materialEigenvalue 材料特征值
     * @return 结果
     */
    @Override
    public synchronized int insertMaterialEigenvalue(MaterialEigenvalue materialEigenvalue) {
        if (materialEigenvalue != null) {
            if (materialEigenvalue.getItemId() == null) {
                throw new RuntimeException("未获取到特征项id");
            }
            if (StringUtils.isEmpty(materialEigenvalue.getOrganCode())) {
                throw new RuntimeException("未获取到机构代码");
            }
            MaterialEigenvalue materialItem1 = new MaterialEigenvalue();
            materialItem1.setItemId(materialEigenvalue.getItemId());
            materialItem1.setDelFlag("0");
            materialItem1.setOrganCode(materialEigenvalue.getOrganCode());
            materialItem1.setEigenvalueCode(materialEigenvalue.getEigenvalueCode());
            materialItem1.setIsMain("N");
            List<MaterialEigenvalue> materialItems = baseMapper.selectMaterialEigenvalueList(materialItem1);
            if (materialItems != null && !materialItems.isEmpty()) {
                throw new RuntimeException("编码已存在");
            }
            if (materialEigenvalue.getId() == null) {
                materialEigenvalue.setId(KeyUtils.generateId());
                materialEigenvalue.setState(0L);
                materialEigenvalue.setCreateId(SecurityUtils.getUserId());
                materialEigenvalue.setCreateBy(SecurityUtils.getUsername());
                materialEigenvalue.setIsMain("N");
                materialEigenvalue.setCreateTime(DateUtils.getNowDate());
            }
        } else {
            throw new RuntimeException("未获取到特征值信息");
        }
        return materialEigenvalueMapper.insertMaterialEigenvalue(materialEigenvalue);
    }

    /**
     * 修改材料特征值
     *
     * @param materialEigenvalue 材料特征值
     * @return 结果
     */
    @Override
    public int updateMaterialEigenvalue(MaterialEigenvalue materialEigenvalue) {
        if (materialEigenvalue == null) {
            throw new RuntimeException("未获取到特征值信息");
        }
        if (materialEigenvalue.getItemId() == null) {
            throw new RuntimeException("未获取到特征项id");
        }
        if (StringUtils.isEmpty(materialEigenvalue.getOrganCode())) {
            throw new RuntimeException("未获取到机构代码");
        }
//        String qc = "-"+materialEigenvalue.getOrganCode().substring(0, 4);
//        materialEigenvalue.setEigenvalueCode(materialEigenvalue.getEigenvalueCode().replace(qc, ""));
        MaterialEigenvalue materialItem1 = new MaterialEigenvalue();
        materialItem1.setTypeId(materialEigenvalue.getTypeId());
        materialItem1.setDelFlag("0");
        materialItem1.setOrganCode(materialEigenvalue.getOrganCode());
        materialItem1.setEigenvalueCode(materialEigenvalue.getEigenvalueCode());
        materialItem1.setIsMain("N");
        List<MaterialEigenvalue> materialItems = baseMapper.selectMaterialEigenvalueList(materialItem1);
        if (materialItems != null && !materialItems.isEmpty()) {
            for (MaterialEigenvalue type : materialItems) {
                if (!type.getId().equals(materialEigenvalue.getId())) {
                    throw new RuntimeException("编码已存在,请刷新后重试");
                }
            }
        }
        materialEigenvalue.setUpdateId(SecurityUtils.getUserId());
        materialEigenvalue.setUpdateBy(SecurityUtils.getUsername());
        materialEigenvalue.setUpdateTime(DateUtils.getNowDate());
        return materialEigenvalueMapper.updateMaterialEigenvalue(materialEigenvalue);
    }

    /**
     * 批量删除材料特征值
     *
     * @param ids 需要删除的材料特征值主键
     * @return 结果
     */
    @Override
    public Boolean deleteMaterialEigenvalueByIds(Long[] ids) {
        if (ids == null) {
            throw new RuntimeException("参数为空");
        }
        List<MaterialEigenvalue> eigenvalues = materialEigenvalueMapper.selectBatchIds(Arrays.asList(ids));
        for (MaterialEigenvalue eigenvalue : eigenvalues) {
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
     * 删除材料特征值信息
     *
     * @param id 材料特征值主键
     * @return 结果
     */
    @Override
    public int deleteMaterialEigenvalueById(Long id) {
        return materialEigenvalueMapper.deleteMaterialEigenvalueById(id);
    }

    @Override
    public List<MaterialEigenvalue> initData(MaterialEigenvalue materialEigenvalue) {
        List<MaterialEigenvalue> eigenvalues = materialEigenvalueMapper.selectMaterialEigenvalueList(materialEigenvalue);
        List<MtrFeatureValue> mtrFeatureValues = iMtrFeatureValueService.selectMtrFeatureValueList(null);
        MaterialItem item1 = new MaterialItem();
        item1.setOrganCode(materialEigenvalue.getOrganCode());
        item1.setIsMain("Y");
        List<MaterialItem> materialItems = iMaterialItemService.selectMaterialItemList(item1);
        if (materialItems == null || materialItems.isEmpty()) {
            throw new RuntimeException("请先配置特征项");
        }
        Map<String, MaterialItem> params = new HashMap<>();
        materialItems.forEach(item -> {
            if ("Y".equals(item.getIsMain())) {
                params.put(item.getHostId(), item);
            }
        });
        Map<String, MtrFeatureValue> mtrMap = new HashMap<>();
        if (mtrFeatureValues != null && !mtrFeatureValues.isEmpty()) {
            mtrFeatureValues.forEach(mtrFeatureValue -> {
                mtrMap.put(mtrFeatureValue.getId(), mtrFeatureValue);
            });
        }
        if (eigenvalues.isEmpty()) {
            //初始化-最开始无数据情况
            if (mtrFeatureValues != null && !mtrFeatureValues.isEmpty()) {
                mtrFeatureValues.forEach(mtrFeatureValue -> {
                    MaterialEigenvalue bean = new MaterialEigenvalue();
                    bean.setId(KeyUtils.generateId());
                    bean.setTypeId(params.get(mtrFeatureValue.getMtrFeatureId()) == null ? -1L : params.get(mtrFeatureValue.getMtrFeatureId()).getTypeId());
                    bean.setItemId(params.get(mtrFeatureValue.getMtrFeatureId()) == null ? -1L : params.get(mtrFeatureValue.getMtrFeatureId()).getId());
                    bean.setEigenvalueName(mtrFeatureValue.getFeatureValueName());
                    bean.setEigenvalueCode(mtrFeatureValue.getFeatureValueCode());
                    bean.setCreateId(SecurityUtils.getUserId());
                    bean.setIsMain("Y");
                    bean.setState(3L);
                    bean.setOrganCode(materialEigenvalue.getOrganCode());
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
            List<MaterialEigenvalue> addList = new ArrayList<>();
            for (String key : mtrMap.keySet()) {
                if (!map.containsKey(key)) {
                    MtrFeatureValue mtrFeatureValue = mtrMap.get(key);
                    MaterialEigenvalue bean = new MaterialEigenvalue();
                    bean.setId(KeyUtils.generateId());
                    bean.setTypeId(params.get(mtrFeatureValue.getMtrFeatureId()) == null ? -1L : params.get(mtrFeatureValue.getMtrFeatureId()).getTypeId());
                    bean.setItemId(params.get(mtrFeatureValue.getMtrFeatureId()) == null ? -1L : params.get(mtrFeatureValue.getMtrFeatureId()).getId());
                    bean.setEigenvalueName(mtrFeatureValue.getFeatureValueName());
                    bean.setEigenvalueCode(mtrFeatureValue.getFeatureValueCode());
                    bean.setCreateId(SecurityUtils.getUserId());
                    bean.setIsMain("Y");
                    bean.setState(3L);
                    bean.setOrganCode(materialEigenvalue.getOrganCode());
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
    public void updateByHostId(MtrFeatureValue mtrFeatureValue) {
        MaterialEigenvalue eigenvalue = new MaterialEigenvalue();
        eigenvalue.setHostId(mtrFeatureValue.getId());
        eigenvalue.setDelFlag("0");
        List<MaterialEigenvalue> eigenvalues = baseMapper.selectMaterialEigenvalueList(eigenvalue);
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
            QueryWrapper<MaterialEigenvalue> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("del_flag", "0");
            queryWrapper.in("host_id", Arrays.asList(histIds));
            List<MaterialEigenvalue> eigenvalues = this.list(queryWrapper);
            //查询新增至主库时不同级或者编码重复的数据
            QueryWrapper<MaterialEigenvalue> qw = new QueryWrapper<>();
            qw.eq("del_flag", "0");
            qw.in("main_id", Arrays.asList(histIds));
            qw.eq("is_main","Y");
            eigenvalues.addAll(this.list(qw));
            List<MaterialEigenvalue> newDeviceDetails = new ArrayList<>();
            List<MaterialEigenvalue> upDeviceDetails = new ArrayList<>();
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
            List<Long> collect = newDeviceDetails.stream().map(MaterialEigenvalue::getId).collect(Collectors.toList());
            this.removeBatchByIds(collect);
        }
    }


    @Override
    public void addByMain(MtrFeatureValue mtrFeatureValue) {
        MaterialItem materialItem = new MaterialItem();
        materialItem.setHostId(mtrFeatureValue.getMtrFeatureId());
        List<MaterialItem> materialItems = iMaterialItemService.selectMaterialItemList(materialItem);
        //已存在数据排除
        MaterialEigenvalue type1 = new MaterialEigenvalue();
        type1.setHostId(mtrFeatureValue.getId());
        type1.setDelFlag("0");
        List<MaterialEigenvalue> materialTypes1 = baseMapper.selectMaterialEigenvalueList(type1);
        Map<String, Long> map = new HashMap<>();
        if (materialTypes1 != null && !materialTypes1.isEmpty()) {
            materialTypes1.forEach(materialType1 -> {
                map.put(materialType1.getOrganCode(), materialType1.getItemId());
            });
        }
        List<MaterialEigenvalue> eigenvalues = new ArrayList<>();
        if (materialItems != null && !materialItems.isEmpty()) {
            for (MaterialItem item : materialItems) {
                if (map.containsKey(item.getOrganCode()) && (item.getId() + "").equals(map.get(item.getOrganCode()) + "")) {
                    continue;
                }
                MaterialEigenvalue bean = new MaterialEigenvalue();
                bean.setId(KeyUtils.generateId());
                bean.setTypeId(item.getTypeId());
                bean.setItemId(item.getId());
                bean.setEigenvalueName(mtrFeatureValue.getFeatureValueName());
                bean.setEigenvalueCode(mtrFeatureValue.getFeatureValueCode());
                bean.setCreateId(SecurityUtils.getUserId());
                bean.setIsMain("Y");
                bean.setState(3L);
                bean.setOrganCode(item.getOrganCode());
                bean.setCreateBy(SecurityUtils.getUsername());
                bean.setCreateTime(DateUtils.getNowDate());
                bean.setHostId(mtrFeatureValue.getId());
                eigenvalues.add(bean);
            }
        }
        if (!eigenvalues.isEmpty()) {
            this.saveBatch(eigenvalues);
        }
    }


    /**
     * 新增至主库
     *
     * @param materialEigenvalue
     * @return
     */
    @Override
    public synchronized int addToMain(MaterialEigenvalue materialEigenvalue) {
        if (materialEigenvalue.getId() == null) {
            throw new BusinessException("请选择要新增至主库的数据");
        }
        if (materialEigenvalue.getHostId() == null) {
            throw new BusinessException("请选择主库数据");
        }
        MaterialEigenvalue materialType1 = this.selectMaterialEigenvalueById(materialEigenvalue.getId());
        if ("Y".equals(materialType1.getIsMain())) {
            throw new BusinessException("当前数据已新增至主库，无法再次新增");
        }
        MtrFeature mtrClass = iMtrFeatureService.selectMtrFeatureById(materialEigenvalue.getHostId());
        //查询编码是否已在主库存在
        MtrFeatureValue mtrClass1 = new MtrFeatureValue();
        mtrClass1.setMtrFeatureId(materialEigenvalue.getHostId());
        mtrClass1.setValid(0L);
        mtrClass1.setFeatureValueCode(materialType1.getEigenvalueCode());
        if (!StringUtils.isEmpty(materialEigenvalue.getEigenvalueCode())) {
            mtrClass1.setFeatureValueCode(materialEigenvalue.getEigenvalueCode());
        }
        List<MtrFeatureValue> mtrClasses = iMtrFeatureValueService.selectMtrFeatureValueList(mtrClass1);
        String key = KeyUtils.generateId() + "";
        if (mtrClasses != null && !mtrClasses.isEmpty()) {
            return -1;
        } else {
            if (!StringUtils.isEmpty(materialEigenvalue.getEigenvalueCode())) {
                MtrFeatureValue aClass = new MtrFeatureValue();
                aClass.setId(key);
                aClass.setMtrFeatureId(mtrClass.getId());
                aClass.setFeatureValueCode(materialEigenvalue.getEigenvalueCode());
                aClass.setFeatureValueName(materialType1.getEigenvalueName());
                aClass.setSonId(materialType1.getId());
                aClass.setCreateBy(SecurityUtils.getUsername());
                aClass.setCreateTime(DateUtils.getNowDate());
                aClass.setCreateId(SecurityUtils.getUserId() + "");
                iMtrFeatureValueService.insertMtrFeatureValue(aClass);
                materialType1.setMainId(aClass.getId());
                materialType1.setIsMain("Y");
                return baseMapper.updateMaterialEigenvalue(materialType1);
            } else {
                //不存在时 新增至主库
                materialType1.setIsMain("Y");
                materialType1.setHostId(key);
                int i = baseMapper.updateMaterialEigenvalue(materialType1);
                if (i > 0) {
                    MtrFeatureValue aClass = new MtrFeatureValue();
                    aClass.setId(key);
                    aClass.setMtrFeatureId(mtrClass.getId());
                    aClass.setFeatureValueCode(materialType1.getEigenvalueCode());
                    aClass.setFeatureValueName(materialType1.getEigenvalueName());
                    aClass.setSonId(materialType1.getId());
                    aClass.setCreateBy(SecurityUtils.getUsername());
                    aClass.setCreateTime(DateUtils.getNowDate());
                    aClass.setCreateId(SecurityUtils.getUserId() + "");
                    iMtrFeatureValueService.insertMtrFeatureValue(aClass);
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
    public synchronized int associationToMain(MaterialEigenvalue materialEigenvalue) {
        if (materialEigenvalue.getId() == null) {
            throw new BusinessException("请选择要关联至主库的数据");
        }
        if (materialEigenvalue.getHostId() == null) {
            throw new BusinessException("请选择主库数据");
        }
        MaterialEigenvalue materialType1 = this.selectMaterialEigenvalueById(materialEigenvalue.getId());
        if ("Y".equals(materialType1.getIsMain())) {
            throw new BusinessException("当前数据已新增至主库，无法进行关联");
        }
        if (!"0".equals(materialType1.getMainId())) {
            throw new BusinessException("当前数据已关联至主库，无法再次关联");
        }
        materialType1.setMainId(materialEigenvalue.getHostId());
        return baseMapper.updateMaterialEigenvalue(materialType1);
    }


    /**
     * 取消关联至主库
     *
     * @param materialEigenvalue
     * @return
     */
    @Override
    public synchronized int unAssociationToMain(MaterialEigenvalue materialEigenvalue) {
        if (materialEigenvalue.getId() == null) {
            throw new BusinessException("请选择要取消关联至主库的数据");
        }
        MaterialEigenvalue materialType1 = this.selectMaterialEigenvalueById(materialEigenvalue.getId());
        if ("Y".equals(materialType1.getIsMain())) {
            throw new BusinessException("当前数据为主库同步数据，无法进行操作");
        }
        if ("0".equals(materialType1.getMainId())) {
            throw new BusinessException("当前数据尚未关联至主库，无需取消关联");
        }
        materialType1.setMainId("0");
        return baseMapper.updateMaterialEigenvalue(materialType1);
    }

    @Override
    public long selectMaterialEigenvalueListCount(MaterialEigenvalue materialEigenvalue) {
        return baseMapper.selectMaterialEigenvalueListCount(materialEigenvalue);
    }

    @Override
    public List<MaterialEigenvalue> getProcessed(String organCode) {
        return baseMapper.getProcessed(organCode);
    }


}
