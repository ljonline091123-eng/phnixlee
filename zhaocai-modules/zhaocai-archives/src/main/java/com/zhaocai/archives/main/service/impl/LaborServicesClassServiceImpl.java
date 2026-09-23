package com.zhaocai.archives.main.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhaocai.archives.dossier.service.ILabourTypeService;
import com.zhaocai.archives.dossier.tree.DeviceTypeTree;
import com.zhaocai.archives.dossier.tree.LabourTypeTree;
import com.zhaocai.archives.main.domain.*;
import com.zhaocai.archives.main.mapper.LaborServicesClassMapper;
import com.zhaocai.archives.main.service.ILaborServicesArchivesService;
import com.zhaocai.archives.main.service.ILaborServicesClassService;
import com.zhaocai.archives.main.service.ILaborServicesFeatureService;
import com.zhaocai.archives.main.service.ILaborServicesFeatureValueService;
import com.zhaocai.archives.utils.KeyUtils;
import com.zhaocai.common.core.exception.ServiceException;
import com.zhaocai.common.core.utils.DateUtils;
import com.zhaocai.common.core.utils.PageUtils;
import com.zhaocai.common.core.utils.StringUtils;
import com.zhaocai.common.security.utils.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;

/**
 * 劳务分类主Service业务层处理
 *
 * @author lzq
 * @date 2025-01-06
 */
@Service
public class LaborServicesClassServiceImpl extends ServiceImpl<LaborServicesClassMapper, LaborServicesClass> implements ILaborServicesClassService {
    @Autowired
    private LaborServicesClassMapper laborServicesClassMapper;

    @Resource
    private ILabourTypeService iLabourTypeService;
    @Autowired
    private ILaborServicesFeatureService iLaborServicesFeatureService;
    @Autowired
    private ILaborServicesFeatureValueService iLaborServicesFeatureValueService;
    @Autowired
    private ILaborServicesArchivesService iLaborServicesArchivesService;

    /**
     * 查询劳务分类主
     *
     * @param id 劳务分类主主键
     * @return 劳务分类主
     */
    @Override
    public LaborServicesClass selectLaborServicesClassById(String id) {
        LaborServicesClass aClass = laborServicesClassMapper.selectLaborServicesClassById(id);
        if (aClass.getParentId() != null && !"0".equals(aClass.getParentId())) {
            LaborServicesClass aClass1 = laborServicesClassMapper.selectLaborServicesClassById(aClass.getParentId());
            aClass.setBelongingLevel(aClass1.getLaborServicesClassName());
        } else {
            aClass.setBelongingLevel("顶级");
        }
        return aClass;
    }

    /**
     * 查询劳务分类主列表
     *
     * @param laborServicesClass 劳务分类主
     * @return 劳务分类主
     */
    @Override
    public List<LaborServicesClass> selectLaborServicesClassList(LaborServicesClass laborServicesClass) {
        if (laborServicesClass == null) {
            laborServicesClass = new LaborServicesClass();
        }
        laborServicesClass.setValid(0L);
        return laborServicesClassMapper.selectLaborServicesClassList(laborServicesClass);
    }

    /**
     * 新增劳务分类主
     *
     * @param laborServicesClass 劳务分类主
     * @return 结果
     */
    @Override
    public synchronized int insertLaborServicesClass(LaborServicesClass laborServicesClass) {
        if (laborServicesClass.getId() == null) {
            throw new RuntimeException("请先初始化");
        }
        if (laborServicesClass.getParentId() == null) {
            throw new RuntimeException("父级不能为空");
        }
        //判断code是否已存在
        LaborServicesClass mtrClass1 = new LaborServicesClass();
        mtrClass1.setValid(0L);
        mtrClass1.setLaborServicesClassCode(laborServicesClass.getLaborServicesClassCode());
        List<LaborServicesClass> mtrClasses = this.selectLaborServicesClassList(mtrClass1);
        if (mtrClasses != null && !mtrClasses.isEmpty()) {
            throw new RuntimeException("编码已存在");
        }
        laborServicesClass.setCreateId(SecurityUtils.getUserId() + "");
        laborServicesClass.setCreateBy(SecurityUtils.getUsername());
        laborServicesClass.setCreateTime(DateUtils.getNowDate());
        laborServicesClass.setValid(0L);
        int i = laborServicesClassMapper.insertLaborServicesClass(laborServicesClass);
        if (i > 0) {
            iLabourTypeService.addTypeByMain(laborServicesClass);
        }
        return i;
    }

    /**
     * 修改劳务分类主
     *
     * @param laborServicesClass 劳务分类主
     * @return 结果
     */
    @Override
    public int updateLaborServicesClass(LaborServicesClass laborServicesClass) {
        //判断code是否已存在
        LaborServicesClass mtrClass1 = new LaborServicesClass();
        mtrClass1.setValid(0L);
        mtrClass1.setLaborServicesClassCode(laborServicesClass.getLaborServicesClassCode());
        List<LaborServicesClass> mtrClasses = this.selectLaborServicesClassList(mtrClass1);
        if (mtrClasses != null && !mtrClasses.isEmpty()) {
            for (LaborServicesClass mtrClass2 : mtrClasses) {
                if (!laborServicesClass.getId().equals(mtrClass2.getId())) {
                    throw new RuntimeException("编码已存在");
                }
            }
        }
        laborServicesClass.setUpdateTime(DateUtils.getNowDate());
        int i = laborServicesClassMapper.updateLaborServicesClass(laborServicesClass);
        if (i > 0) {
            iLabourTypeService.updateByHostId(laborServicesClass);
        }
        return i;
    }

    /**
     * 批量删除劳务分类主
     *
     * @param ids 需要删除的劳务分类主主键
     * @return 结果
     */
    @Override
    public boolean deleteLaborServicesClassByIds(String[] ids) {
        if (ids == null) {
            throw new RuntimeException("id不能为空");
        }
        List<LaborServicesClass> mtrFeatures = this.listByIds(Arrays.asList(ids));
        Map<Long, Long> map = new HashMap<>();
        if (mtrFeatures != null && !mtrFeatures.isEmpty()) {
            mtrFeatures.forEach(item -> {
                int i = baseMapper.getMaterialJoinNoMy(item.getId());
                if (i > 0) {
                    throw new RuntimeException("当前分类下存在数据，无法进行删除");
                }
                item.setValid(System.currentTimeMillis() / 1000L);
                if (item.getSonId() != null) {
                    map.put(item.getSonId(), item.getSonId());
                }
            });
        }
        boolean b = this.updateBatchById(mtrFeatures);
        if (b) {
            iLabourTypeService.deleteByHostId(ids, map);
        }
        return b;
    }

    /**
     * 删除劳务分类主信息
     *
     * @param id 劳务分类主主键
     * @return 结果
     */
    @Override
    public int deleteLaborServicesClassById(String id) {
        return laborServicesClassMapper.deleteLaborServicesClassById(id);
    }


    public List<LabourTypeTree> getLaborServicesClassTree() {
        LaborServicesClass mtrClass = new LaborServicesClass();
        mtrClass.setValid(0L);
        PageUtils.clearPage();
        List<LaborServicesClass> select = baseMapper.selectLaborServicesClassList(mtrClass);
        Map<String, LabourTypeTree> map = new HashMap<>();
        select.forEach(item -> {
            LabourTypeTree materialTypeTree = new LabourTypeTree();
            materialTypeTree.setId(item.getId() + "");
            materialTypeTree.setLabel(item.getLaborServicesClassName());
            materialTypeTree.setType(item.getLaborServicesClassType());
            materialTypeTree.setCode(item.getLaborServicesClassCode());
            materialTypeTree.setChildren(new ArrayList<>());
            map.put(item.getId(), materialTypeTree);
        });

        // 构建树形结构
        List<LabourTypeTree> list = new ArrayList<>();
        for (LaborServicesClass type : select) {
            LabourTypeTree treeVo = map.get(type.getId());
            if (StringUtils.isEmpty(type.getParentId()) || "0".equals(type.getParentId())) {
                // 根节点，直接添加
                list.add(treeVo);
            } else {
                // 非根节点，找到父节点并添加到其子节点列表中
                LabourTypeTree materialTypeTree = map.get(type.getParentId());
                if (materialTypeTree != null) {
                    materialTypeTree.getChildren().add(treeVo);
                }
            }
        }
        return list;
    }

    public List<LabourTypeTree> getLaborServicesClassTreeTwo() {
        LaborServicesClass mtrClass = new LaborServicesClass();
        mtrClass.setValid(0L);
        PageUtils.clearPage();
        List<LaborServicesClass> select = baseMapper.selectLaborServicesClassList(mtrClass);
        Map<String, LabourTypeTree> map = new HashMap<>();
        select.forEach(item -> {
            LabourTypeTree materialTypeTree = new LabourTypeTree();
            materialTypeTree.setId(item.getId() + "");
            materialTypeTree.setLabel(item.getLaborServicesClassName());
            materialTypeTree.setType(item.getLaborServicesClassType());
            materialTypeTree.setCode(item.getLaborServicesClassCode());
            materialTypeTree.setChildren(new ArrayList<>());
            map.put(item.getId(), materialTypeTree);
        });

        // 构建树形结构
        List<LabourTypeTree> list = new ArrayList<>();
        for (LaborServicesClass type : select) {
            LabourTypeTree treeVo = map.get(type.getId());
            if (StringUtils.isEmpty(type.getParentId()) || "0".equals(type.getParentId())) {
                // 根节点，直接添加
                list.add(treeVo);
            } else {
                // 非根节点，找到父节点并添加到其子节点列表中
                LabourTypeTree materialTypeTree = map.get(type.getParentId());
                if (materialTypeTree != null) {
                    materialTypeTree.getChildren().add(treeVo);
                }
            }
        }
        //具体档案加入树
        LaborServicesArchives detail = new LaborServicesArchives();
        detail.setValid(0L);
        List<LaborServicesArchives> detailList = iLaborServicesArchivesService.selectLaborServicesArchivesList(detail);
        if(CollectionUtil.isNotEmpty(detailList)){
            detailList.stream().forEach(item->{
                LabourTypeTree materialTypeTree = map.get(item.getLaborServicesClassId());
                if (materialTypeTree != null) {
                    LabourTypeTree tree = new LabourTypeTree();
                    tree.setId(item.getId() + "");
                    tree.setLabel(item.getLaborServicesName());
                    tree.setType(item.getLaborServicesClassName());
                    tree.setCode(item.getLaborServicesCode());
                    tree.setChildren(new ArrayList<>());
                    tree.setState(item.getValid());
                    materialTypeTree.getChildren().add(tree);
                }
            });
        }
        return list;
    }

    @Override
    public LaborServicesClass initCode(LaborServicesClass laborServicesClass) {
        if (laborServicesClass.getId() == null) {
            throw new RuntimeException("id不能为空");
        }
        String materialCode = "";
        LaborServicesClass type = new LaborServicesClass();
        if ("0".equals(laborServicesClass.getId())) {
            String[] strs = {"A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z"};
            LaborServicesClass aClassx = new LaborServicesClass();
            aClassx.setParentId("0");
            List<LaborServicesClass> mtrClasses = baseMapper.selectLaborServicesClassList(aClassx);
            Map<String, String> map = new HashMap<>();
            for (LaborServicesClass mtrClass1 : mtrClasses) {
                String s = retainEnglishLetters(mtrClass1.getLaborServicesClassCode());
                map.put(s, s);
            }
            for (String s : strs) {
                if (StringUtils.isEmpty(map.get(s))) {
                    materialCode = s + "1";
                    break;
                }
            }
        } else {
            type = baseMapper.selectLaborServicesClassById(laborServicesClass.getId());
            materialCode = type.getLaborServicesClassCode();
            Integer maxCode = baseMapper.getMaxCode(materialCode, type.getId());
            if (maxCode != null) {
                maxCode += 1;
                //根据规则，长度大于8的流水号有3位
                if (materialCode.length() >= 8) {
                    if (maxCode < 100 && maxCode >= 10) {
                        materialCode = materialCode + "0" + maxCode;
                    } else if (maxCode < 10) {
                        materialCode = materialCode + "00" + maxCode;
                    } else {
                        materialCode = materialCode + maxCode;
                    }
                } else {
                    if (maxCode < 10) {
                        materialCode = materialCode + "0" + maxCode;
                    } else {
                        materialCode = materialCode + maxCode;
                    }
                }
            } else {
                if (materialCode.length() >= 8) {
                    materialCode = materialCode + "001";
                } else {
                    materialCode = materialCode + "01";
                }
            }
        }
        LaborServicesClass aClass = new LaborServicesClass();
        aClass.setId(KeyUtils.generateId() + "");
        aClass.setParentId(laborServicesClass.getId());
        aClass.setLaborServicesClassCode(materialCode);
        aClass.setCreateId(SecurityUtils.getUserId() + "");
        aClass.setCreateBy(SecurityUtils.getUsername());
        aClass.setCreateTime(DateUtils.getNowDate());
        return aClass;
    }

    @Override
    public long selectLaborServicesClassListCount(LaborServicesClass laborServicesClass) {
        return baseMapper.selectLaborServicesClassListCount(laborServicesClass);
    }


    /**
     * 保留字符串中的英文字符
     *
     * @param input 输入字符串
     * @return 只包含英文字符的字符串
     */
    public static String retainEnglishLetters(String input) {
        if (input == null || input.isEmpty()) {
            return input; // 处理空字符串或 null 的情况
        }
        return input.replaceAll("[^a-zA-Z]", ""); // 保留 a-z 和 A-Z 范围内的字符
    }


    @Override
    public String importData(List<LaborServicesClassExcelData> userList, boolean updateSupport, String operName) {
        if (StringUtils.isNull(userList) || userList.size() == 0) {
            throw new ServiceException("导入数据不能为空！");
        }
        LaborServicesClass mtrClass = new LaborServicesClass();
        mtrClass.setValid(0L);
        List<LaborServicesClass> mtrClasses = laborServicesClassMapper.selectLaborServicesClassList(mtrClass);
        Map<String, LaborServicesClass> map = new HashMap<>();
        mtrClasses.forEach(item -> {
            map.put(item.getLaborServicesClassCode(), item);
        });
        LaborServicesFeature mtrFeature = new LaborServicesFeature();
        mtrFeature.setValid(0L);
        Map<String, LaborServicesFeature> featureMap = new HashMap<>();
        List<LaborServicesFeature> mtrFeatures = iLaborServicesFeatureService.selectLaborServicesFeatureList(mtrFeature);
        mtrFeatures.forEach(item -> {
            featureMap.put(item.getLaborServicesClassId() + "|" + item.getFeatureCode(), item);
        });
        LaborServicesFeatureValue mtrFeatureValue = new LaborServicesFeatureValue();
        mtrFeatureValue.setValid(0L);
        Map<String, LaborServicesFeatureValue> valMap = new HashMap<>();
        List<LaborServicesFeatureValue> mtrFeatureValues = iLaborServicesFeatureValueService.selectLaborServicesFeatureValueList(mtrFeatureValue);
        mtrFeatureValues.forEach(item -> {
            valMap.put(item.getLaborServicesFeatureId() + "|" + item.getFeatureValueCode(), item);
        });
        LaborServicesArchives mtrArchives1 = new LaborServicesArchives();
        mtrArchives1.setValid(0L);
        Map<String, LaborServicesArchives> mtrArchivesMap = new HashMap<>();
        List<LaborServicesArchives> mtrArchives = iLaborServicesArchivesService.selectLaborServicesArchivesList(mtrArchives1);
        mtrArchives.forEach(item -> {
            mtrArchivesMap.put(item.getLaborServicesClassId() + "|" + item.getLaborServicesCode(), item);
        });
        List<LaborServicesClass> addMtrClasses = new ArrayList<>();
        List<LaborServicesFeature> mtrFeatureList = new ArrayList<>();
        List<LaborServicesFeatureValue> mtrFeatureValueList = new ArrayList<>();
        List<LaborServicesArchives> mtrArchivesList = new ArrayList<>();
        int errIndex = 1;
        StringBuilder successMsg = new StringBuilder();
        StringBuilder failureMsg = new StringBuilder();
        for (LaborServicesClassExcelData data : userList) {
            errIndex++;
            //判空
            if (StringUtils.isEmpty(data.getParentCode())) {
                failureMsg.append("<br/>第" + errIndex + "行，父级劳务分类编码为空");
            }
            if (StringUtils.isEmpty(data.getLaborServicesClassCode())) {
                failureMsg.append("<br/>第" + errIndex + "行，劳务分类编码为空");
            }
            if (StringUtils.isEmpty(data.getLaborServicesClassName())) {
                failureMsg.append("<br/>第" + errIndex + "行，劳务分类名称为空");
            }
            if (!StringUtils.isEmpty(failureMsg.toString())) {
                continue;
            }
            if ("0".equals(data.getParentCode())) {
                if (map.containsKey(data.getLaborServicesClassCode())) {
                    String id = map.get(data.getLaborServicesClassCode()).getId();
                    String name = map.get(data.getLaborServicesClassCode()).getLaborServicesClassName();
                    //判断是否有特征值、特征项、详细档案
                    if (!StringUtils.isEmpty(data.getFeatureCode()) || !StringUtils.isEmpty(data.getFeatureValueCode()) || !StringUtils.isEmpty(data.getLaborServicesCode())) {
                        if (!StringUtils.isEmpty(data.getFeatureCode())) {
                            if (featureMap.containsKey(id + "|" + data.getFeatureCode())) {
                                if (!StringUtils.isEmpty(data.getFeatureValueCode())) {
                                    String id1 = featureMap.get(id + "|" + data.getFeatureCode()).getId();
                                    if (valMap.containsKey(id1 + "|" + data.getFeatureValueCode())) {
                                        failureMsg.append("<br/>第" + errIndex + "行，特征值已存在");
                                    } else {
                                        LaborServicesFeatureValue mtrFeatureValue1 = new LaborServicesFeatureValue();
                                        mtrFeatureValue1.setId(KeyUtils.generateId() + "");
                                        mtrFeatureValue1.setLaborServicesFeatureId(id1);
                                        mtrFeatureValue1.setFeatureValueName(data.getFeatureValueCode());
                                        mtrFeatureValue1.setFeatureValueCode(data.getFeatureValueCode());
                                        mtrFeatureValue1.setCreateId(SecurityUtils.getUserId() + "");
                                        mtrFeatureValue1.setCreateBy(SecurityUtils.getUsername());
                                        mtrFeatureValue1.setCreateTime(DateUtils.getNowDate());
                                        mtrFeatureValueList.add(mtrFeatureValue1);
                                        valMap.put(id1 + "|" + data.getFeatureValueCode(), mtrFeatureValue1);
                                    }
                                } else {
                                    failureMsg.append("<br/>第" + errIndex + "行，特征项已存在");
                                }
                            } else {
                                LaborServicesFeature mtrFeature1 = new LaborServicesFeature();
                                mtrFeature1.setId(KeyUtils.generateId() + "");
                                mtrFeature1.setLaborServicesClassId(id);
                                mtrFeature1.setFeatureCode(data.getFeatureCode());
                                mtrFeature1.setFeatureName(data.getFeatureName());
                                mtrFeature1.setCreateId(SecurityUtils.getUserId() + "");
                                mtrFeature1.setCreateBy(SecurityUtils.getUsername());
                                mtrFeature1.setCreateTime(DateUtils.getNowDate());
                                mtrFeatureList.add(mtrFeature1);
                                featureMap.put(id + "|" + data.getFeatureCode(), mtrFeature1);
                                if (!StringUtils.isEmpty(data.getFeatureValueCode())) {
                                    LaborServicesFeatureValue mtrFeatureValue1 = new LaborServicesFeatureValue();
                                    mtrFeatureValue1.setId(KeyUtils.generateId() + "");
                                    mtrFeatureValue1.setLaborServicesFeatureId(mtrFeature1.getId());
                                    mtrFeatureValue1.setFeatureValueName(data.getFeatureValueCode());
                                    mtrFeatureValue1.setFeatureValueCode(data.getFeatureValueCode());
                                    mtrFeatureValue1.setCreateId(SecurityUtils.getUserId() + "");
                                    mtrFeatureValue1.setCreateBy(SecurityUtils.getUsername());
                                    mtrFeatureValue1.setCreateTime(DateUtils.getNowDate());
                                    mtrFeatureValueList.add(mtrFeatureValue1);
                                    valMap.put(mtrFeature1.getId() + "|" + data.getFeatureValueCode(), mtrFeatureValue1);
                                }
                            }
                        }
                        if (!StringUtils.isEmpty(data.getLaborServicesCode())) {
                            if (mtrArchivesMap.containsKey(id + "|" + data.getLaborServicesCode())) {
                                failureMsg.append("<br/>第" + errIndex + "行，具体档案已存在");
                            } else {
                                LaborServicesArchives mtrArchives2 = new LaborServicesArchives();
                                mtrArchives2.setId(KeyUtils.generateId() + "");
                                mtrArchives2.setLaborServicesClassId(id);
                                mtrArchives2.setLaborServicesCode(data.getLaborServicesCode());
                                mtrArchives2.setLaborServicesName(data.getLaborServicesName());
                                mtrArchives2.setLaborServicesClassName(name);
                                mtrArchives2.setMeasureUnit(data.getUnit());
                                mtrArchives2.setSpecs(data.getSpecs());
                                mtrArchives2.setFeature(data.getFeature());
                                mtrArchives2.setMetrologicalRules(data.getMetrologicalRules());
                                mtrArchives2.setBasicJob(data.getBasicJob());
                                mtrArchives2.setCreateId(SecurityUtils.getUserId() + "");
                                mtrArchives2.setCreateBy(SecurityUtils.getUsername());
                                mtrArchives2.setCreateTime(DateUtils.getNowDate());
                                mtrArchivesList.add(mtrArchives2);
                                mtrArchivesMap.put(id + "|" + data.getLaborServicesCode(), mtrArchives2);
                            }
                        }
                    } else {
                        failureMsg.append("<br/>第" + errIndex + "行，材料分类已存在");
                    }
                } else {
                    LaborServicesClass mtrClass1 = new LaborServicesClass();
                    mtrClass1.setValid(0L);
                    mtrClass1.setId(KeyUtils.generateId() + "");
                    mtrClass1.setParentId(data.getParentCode());
                    mtrClass1.setLaborServicesClassCode(data.getLaborServicesClassCode());
                    mtrClass1.setLaborServicesClassName(data.getLaborServicesClassName());
                    mtrClass1.setMeasureUnit(data.getMeasureUnit());
                    mtrClass1.setCreateId(SecurityUtils.getUserId() + "");
                    mtrClass1.setCreateBy(SecurityUtils.getUsername());
                    mtrClass1.setCreateTime(DateUtils.getNowDate());
                    addMtrClasses.add(mtrClass1);
                    map.put(mtrClass1.getLaborServicesClassCode(), mtrClass1);
                }
            } else {
                if (map.containsKey(data.getLaborServicesClassCode())) {
                    String id = map.get(data.getLaborServicesClassCode()).getId();
                    String name = map.get(data.getLaborServicesClassCode()).getLaborServicesClassName();
                    //判断是否有特征值、特征项、详细档案
                    if (!StringUtils.isEmpty(data.getFeatureCode()) || !StringUtils.isEmpty(data.getFeatureValueCode()) || !StringUtils.isEmpty(data.getLaborServicesCode())) {
                        if (!StringUtils.isEmpty(data.getFeatureCode())) {
                            if (featureMap.containsKey(id + "|" + data.getFeatureCode())) {
                                if (!StringUtils.isEmpty(data.getFeatureValueCode())) {
                                    String id1 = featureMap.get(id + "|" + data.getFeatureCode()).getId();
                                    if (valMap.containsKey(id1 + "|" + data.getFeatureValueCode())) {
                                        failureMsg.append("<br/>第" + errIndex + "行，特征值已存在");
                                    } else {
                                        LaborServicesFeatureValue mtrFeatureValue1 = new LaborServicesFeatureValue();
                                        mtrFeatureValue1.setId(KeyUtils.generateId() + "");
                                        mtrFeatureValue1.setLaborServicesFeatureId(id1);
                                        mtrFeatureValue1.setFeatureValueName(data.getFeatureValueCode());
                                        mtrFeatureValue1.setFeatureValueCode(data.getFeatureValueCode());
                                        mtrFeatureValue1.setCreateId(SecurityUtils.getUserId() + "");
                                        mtrFeatureValue1.setCreateBy(SecurityUtils.getUsername());
                                        mtrFeatureValue1.setCreateTime(DateUtils.getNowDate());
                                        mtrFeatureValueList.add(mtrFeatureValue1);
                                        valMap.put(id1 + "|" + data.getFeatureValueCode(), mtrFeatureValue1);
                                    }
                                } else {
                                    failureMsg.append("<br/>第" + errIndex + "行，特征项已存在");
                                }
                            } else {
                                LaborServicesFeature mtrFeature1 = new LaborServicesFeature();
                                mtrFeature1.setId(KeyUtils.generateId() + "");
                                mtrFeature1.setLaborServicesClassId(id);
                                mtrFeature1.setFeatureCode(data.getFeatureCode());
                                mtrFeature1.setFeatureName(data.getFeatureName());
                                mtrFeature1.setCreateId(SecurityUtils.getUserId() + "");
                                mtrFeature1.setCreateBy(SecurityUtils.getUsername());
                                mtrFeature1.setCreateTime(DateUtils.getNowDate());
                                mtrFeatureList.add(mtrFeature1);
                                featureMap.put(id + "|" + data.getFeatureCode(), mtrFeature1);

                                if (!StringUtils.isEmpty(data.getFeatureValueCode())) {
                                    LaborServicesFeatureValue mtrFeatureValue1 = new LaborServicesFeatureValue();
                                    mtrFeatureValue1.setId(KeyUtils.generateId() + "");
                                    mtrFeatureValue1.setLaborServicesFeatureId(mtrFeature1.getId());
                                    mtrFeatureValue1.setFeatureValueName(data.getFeatureValueCode());
                                    mtrFeatureValue1.setFeatureValueCode(data.getFeatureValueCode());
                                    mtrFeatureValue1.setCreateId(SecurityUtils.getUserId() + "");
                                    mtrFeatureValue1.setCreateBy(SecurityUtils.getUsername());
                                    mtrFeatureValue1.setCreateTime(DateUtils.getNowDate());
                                    mtrFeatureValueList.add(mtrFeatureValue1);
                                    valMap.put(mtrFeature1.getId() + "|" + data.getFeatureValueCode(), mtrFeatureValue1);
                                }
                            }
                        }
                        if (!StringUtils.isEmpty(data.getLaborServicesCode())) {
                            if (mtrArchivesMap.containsKey(id + "|" + data.getLaborServicesCode())) {
                                failureMsg.append("<br/>第" + errIndex + "行，具体档案已存在");
                            } else {
                                LaborServicesArchives mtrArchives2 = new LaborServicesArchives();
                                mtrArchives2.setId(KeyUtils.generateId() + "");
                                mtrArchives2.setLaborServicesClassId(id);
                                mtrArchives2.setLaborServicesCode(data.getLaborServicesCode());
                                mtrArchives2.setLaborServicesName(data.getLaborServicesName());
                                mtrArchives2.setLaborServicesClassName(name);
                                mtrArchives2.setMeasureUnit(data.getUnit());
                                mtrArchives2.setSpecs(data.getSpecs());
                                mtrArchives2.setFeature(data.getFeature());
                                mtrArchives2.setMetrologicalRules(data.getMetrologicalRules());
                                mtrArchives2.setBasicJob(data.getBasicJob());
                                mtrArchives2.setCreateId(SecurityUtils.getUserId() + "");
                                mtrArchives2.setCreateBy(SecurityUtils.getUsername());
                                mtrArchives2.setCreateTime(DateUtils.getNowDate());
                                mtrArchivesList.add(mtrArchives2);
                                mtrArchivesMap.put(id + "|" + data.getLaborServicesCode(), mtrArchives2);
                            }
                        }
                    } else {
                        failureMsg.append("<br/>第" + errIndex + "行，材料分类已存在");
                    }
                } else {
                    LaborServicesClass mtrClass1 = new LaborServicesClass();
                    mtrClass1.setValid(0L);
                    mtrClass1.setId(KeyUtils.generateId() + "");
                    mtrClass1.setParentId(map.get(data.getParentCode()).getId());
                    mtrClass1.setLaborServicesClassCode(data.getLaborServicesClassCode());
                    mtrClass1.setLaborServicesClassName(data.getLaborServicesClassName());
                    mtrClass1.setMeasureUnit(data.getMeasureUnit());
                    mtrClass1.setCreateId(SecurityUtils.getUserId() + "");
                    mtrClass1.setCreateBy(SecurityUtils.getUsername());
                    mtrClass1.setCreateTime(DateUtils.getNowDate());
                    addMtrClasses.add(mtrClass1);
                    map.put(mtrClass1.getLaborServicesClassCode(), mtrClass1);
                    if (!StringUtils.isEmpty(data.getFeatureCode())) {
                        LaborServicesFeature mtrFeature1 = new LaborServicesFeature();
                        mtrFeature1.setId(KeyUtils.generateId() + "");
                        mtrFeature1.setLaborServicesClassId(mtrClass1.getId());
                        mtrFeature1.setFeatureCode(data.getFeatureCode());
                        mtrFeature1.setFeatureName(data.getFeatureName());
                        mtrFeature1.setCreateId(SecurityUtils.getUserId() + "");
                        mtrFeature1.setCreateBy(SecurityUtils.getUsername());
                        mtrFeature1.setCreateTime(DateUtils.getNowDate());
                        mtrFeatureList.add(mtrFeature1);
                        featureMap.put(mtrClass1.getId() + "|" + data.getFeatureCode(), mtrFeature1);
                        if (!StringUtils.isEmpty(data.getFeatureValueCode())) {
                            LaborServicesFeatureValue mtrFeatureValue1 = new LaborServicesFeatureValue();
                            mtrFeatureValue1.setId(KeyUtils.generateId() + "");
                            mtrFeatureValue1.setLaborServicesFeatureId(mtrFeature1.getId());
                            mtrFeatureValue1.setFeatureValueName(data.getFeatureValueCode());
                            mtrFeatureValue1.setFeatureValueCode(data.getFeatureValueCode());
                            mtrFeatureValue1.setCreateId(SecurityUtils.getUserId() + "");
                            mtrFeatureValue1.setCreateBy(SecurityUtils.getUsername());
                            mtrFeatureValue1.setCreateTime(DateUtils.getNowDate());
                            mtrFeatureValueList.add(mtrFeatureValue1);
                            valMap.put(mtrFeature1.getId() + "|" + data.getFeatureValueCode(), mtrFeatureValue1);
                        }
                    }
                    if (!StringUtils.isEmpty(data.getLaborServicesCode())) {
                        LaborServicesArchives mtrArchives2 = new LaborServicesArchives();
                        mtrArchives2.setId(KeyUtils.generateId() + "");
                        mtrArchives2.setLaborServicesClassId(mtrClass1.getId());
                        mtrArchives2.setLaborServicesCode(data.getLaborServicesCode());
                        mtrArchives2.setLaborServicesName(data.getLaborServicesName());
                        mtrArchives2.setLaborServicesClassName(data.getLaborServicesClassName());
                        mtrArchives2.setMeasureUnit(data.getUnit());
                        mtrArchives2.setSpecs(data.getSpecs());
                        mtrArchives2.setFeature(data.getFeature());
                        mtrArchives2.setMetrologicalRules(data.getMetrologicalRules());
                        mtrArchives2.setBasicJob(data.getBasicJob());
                        mtrArchives2.setCreateId(SecurityUtils.getUserId() + "");
                        mtrArchives2.setCreateBy(SecurityUtils.getUsername());
                        mtrArchives2.setCreateTime(DateUtils.getNowDate());
                        mtrArchivesList.add(mtrArchives2);
                        mtrArchivesMap.put(mtrClass1.getId() + "|" + data.getLaborServicesCode(), mtrArchives2);
                    }
                }
            }
        }
        if (StringUtils.isEmpty(failureMsg.toString())) {
            if (!addMtrClasses.isEmpty()) {
                for (LaborServicesClass mtrClass1 : addMtrClasses) {
                    laborServicesClassMapper.insertLaborServicesClass(mtrClass1);
                }
            }
            if (!mtrFeatureList.isEmpty()) {
                for (LaborServicesFeature mtrClass1 : mtrFeatureList) {
                    iLaborServicesFeatureService.insertLaborServicesFeature(mtrClass1);
                }
            }
            if (!mtrFeatureValueList.isEmpty()) {
                for (LaborServicesFeatureValue mtrClass1 : mtrFeatureValueList) {
                    iLaborServicesFeatureValueService.insertLaborServicesFeatureValue(mtrClass1);
                }
            }
            if (!mtrArchivesList.isEmpty()) {
                for (LaborServicesArchives mtrClass1 : mtrArchivesList) {
                    iLaborServicesArchivesService.insertLaborServicesArchives(mtrClass1);
                }
            }
        } else {
            failureMsg.insert(0, "很抱歉，导入失败！错误如下：");
            throw new ServiceException(failureMsg.toString());
        }
        successMsg.insert(0, "恭喜您，数据已全部导入成功！");
        return successMsg.toString();
    }


}
