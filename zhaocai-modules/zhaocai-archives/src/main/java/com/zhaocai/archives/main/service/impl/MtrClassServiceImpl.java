package com.zhaocai.archives.main.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhaocai.archives.dossier.service.IMaterialTypeService;
import com.zhaocai.archives.dossier.tree.MaterialTypeTree;
import com.zhaocai.archives.main.domain.MtrClass;
import com.zhaocai.archives.main.mapper.MtrClassMapper;
import com.zhaocai.archives.main.service.IMtrClassService;
import com.zhaocai.archives.utils.KeyUtils;
import com.zhaocai.common.core.utils.DateUtils;
import com.zhaocai.common.core.utils.PageUtils;
import com.zhaocai.common.core.utils.StringUtils;
import com.zhaocai.common.security.utils.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;

/**
 * 材料分类主Service业务层处理
 *
 * @author lzq
 * @date 2025-01-06
 */
@Service
public class MtrClassServiceImpl extends ServiceImpl<MtrClassMapper, MtrClass> implements IMtrClassService {
    @Autowired
    private MtrClassMapper mtrClassMapper;

    @Resource
    private IMaterialTypeService materialTypeService;


    /**
     * 查询材料分类主
     *
     * @param id 材料分类主主键
     * @return 材料分类主
     */
    @Override
    public MtrClass selectMtrClassById(String id) {
        MtrClass aClass = mtrClassMapper.selectMtrClassById(id);
        if (aClass.getParentId() != null && !"0".equals(aClass.getParentId())) {
            MtrClass aClass1 = mtrClassMapper.selectMtrClassById(aClass.getParentId());
            aClass.setBelongingLevel(aClass1.getMtrClassName());
        } else {
            aClass.setBelongingLevel("顶级");
        }
        return aClass;
    }

    /**
     * 查询材料分类主列表
     *
     * @param mtrClass 材料分类主
     * @return 材料分类主
     */
    @Override
    public List<MtrClass> selectMtrClassList(MtrClass mtrClass) {
        if (mtrClass == null) {
            mtrClass = new MtrClass();
        }
        mtrClass.setValid(0L);
        return mtrClassMapper.selectMtrClassList(mtrClass);
    }

    /**
     * 查询材料分类主列表
     *
     * @param mtrClass 材料分类主
     * @return 材料分类主
     */
    @Override
    public List<MtrClass> selectMtrClassListNoChange(MtrClass mtrClass) {
        return mtrClassMapper.selectMtrClassList(mtrClass);
    }

    /**
     * 新增材料分类主
     *
     * @param mtrClass 材料分类主
     * @return 结果
     */
    @Override
    public synchronized int insertMtrClass(MtrClass mtrClass) {
        if (mtrClass.getId() == null) {
            throw new RuntimeException("请先初始化");
        }
        if (mtrClass.getParentId() == null) {
            throw new RuntimeException("父级不能为空");
        }
        //判断code是否已存在
        MtrClass mtrClass1 = new MtrClass();
        mtrClass1.setValid(0L);
        mtrClass1.setMtrClassCode(mtrClass.getMtrClassCode());
        List<MtrClass> mtrClasses = this.selectMtrClassList(mtrClass1);
        if (mtrClasses != null && !mtrClasses.isEmpty()) {
            throw new RuntimeException("编码已存在");
        }
        mtrClass.setCreateId(SecurityUtils.getUserId() + "");
        mtrClass.setCreateBy(SecurityUtils.getUsername());
        mtrClass.setCreateTime(DateUtils.getNowDate());
        mtrClass.setValid(0L);
        int i = mtrClassMapper.insertMtrClass(mtrClass);
        if (i > 0) {
            materialTypeService.addMaterialTypeByMain(mtrClass);
        }
        return i;
    }

    /**
     * 修改材料分类主
     *
     * @param mtrClass 材料分类主
     * @return 结果
     */
    @Override
    public synchronized int updateMtrClass(MtrClass mtrClass) {
        //判断code是否已存在
        MtrClass mtrClass1 = new MtrClass();
        mtrClass1.setValid(0L);
        mtrClass1.setMtrClassCode(mtrClass.getMtrClassCode());
        List<MtrClass> mtrClasses = this.selectMtrClassList(mtrClass1);
        if (mtrClasses != null && !mtrClasses.isEmpty()) {
            for (MtrClass mtrClass2 : mtrClasses) {
                if (!mtrClass.getId().equals(mtrClass2.getId())) {
                    throw new RuntimeException("编码已存在");
                }
            }
        }
        mtrClass.setUpdateTime(DateUtils.getNowDate());
        mtrClass.setIsTb("2");
        int i = mtrClassMapper.updateMtrClass(mtrClass);
        if (i > 0) {
            materialTypeService.updateByHostId(mtrClass);
        }
        return i;
    }

    /**
     * 批量删除材料分类主
     *
     * @param ids 需要删除的材料分类主主键
     * @return 结果
     */
    @Override
    public Boolean deleteMtrClassByIds(String[] ids) {
        if (ids == null) {
            throw new RuntimeException("id不能为空");
        }
        List<MtrClass> mtrFeatures = this.listByIds(Arrays.asList(ids));
        Map<Long, Long> map = new HashMap<>();
        if (mtrFeatures != null && !mtrFeatures.isEmpty()) {
            mtrFeatures.forEach(item -> {
                int i = baseMapper.getMaterialJoinNoMy(item.getId());
                if (i > 0) {
                    throw new RuntimeException("当前分类下存在数据，无法进行删除");
                }
                item.setValid(System.currentTimeMillis() / 1000L);
                item.setIsTb("3");
                if (item.getSonId() != null) {
                    map.put(item.getSonId(), item.getSonId());
                }
            });
        }
        boolean b = this.updateBatchById(mtrFeatures);
        if (b) {
            materialTypeService.deleteByHostId(ids, map);
        }
        return b;
    }

    /**
     * 删除材料分类主信息
     *
     * @param id 材料分类主主键
     * @return 结果
     */
    @Override
    public int deleteMtrClassById(String id) {
        return mtrClassMapper.deleteMtrClassById(id);
    }

    @Override
    public MtrClass initCode(MtrClass mtrClass) {
        if (mtrClass.getId() == null) {
            throw new RuntimeException("id不能为空");
        }
        String materialCode = "";
        MtrClass type = new MtrClass();
        if ("0".equals(mtrClass.getId())) {
            String[] strs = {"A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z"};
            MtrClass aClassx = new MtrClass();
            aClassx.setParentId("0");
            List<MtrClass> mtrClasses = baseMapper.selectMtrClassList(aClassx);
            Map<String, String> map = new HashMap<>();
            for (MtrClass mtrClass1 : mtrClasses) {
                String s = retainEnglishLetters(mtrClass1.getMtrClassCode());
                map.put(s, s);
            }
            for (String s : strs) {
                if (StringUtils.isEmpty(map.get(s))) {
                    materialCode = s + "1";
                    break;
                }
            }
        } else {
            type = baseMapper.selectMtrClassById(mtrClass.getId());
            materialCode = type.getMtrClassCode();
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
        MtrClass aClass = new MtrClass();
        aClass.setId(KeyUtils.generateId() + "");
        aClass.setParentId(mtrClass.getId());
        aClass.setMtrClassCode(materialCode);
        try {
            if (!StringUtils.isEmpty(type.getClassLevelCd())) {
                int cj = Integer.parseInt(type.getClassLevelCd()) + 1;
                aClass.setClassLevelCd(cj + "");
                aClass.setClassLevel(this.getLevel(cj + ""));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        aClass.setCreateId(SecurityUtils.getUserId() + "");
        aClass.setCreateBy(SecurityUtils.getUsername());
        aClass.setCreateTime(DateUtils.getNowDate());
        return aClass;
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
    public List<MaterialTypeTree> getMtrClassTree(MtrClass materialType) {
        MtrClass mtrClass = new MtrClass();
        mtrClass.setValid(0L);
        PageUtils.clearPage();
        List<MtrClass> select = baseMapper.selectMtrClassList(mtrClass);
        Map<String, MaterialTypeTree> map = new HashMap<>();
        select.forEach(item -> {
            MaterialTypeTree materialTypeTree = new MaterialTypeTree();
            materialTypeTree.setId(item.getId() + "");
            materialTypeTree.setLabel(item.getMtrClassName());
            materialTypeTree.setType(item.getMtrClassType());
            materialTypeTree.setCode(item.getMtrClassCode());
            materialTypeTree.setChildren(new ArrayList<>());
            map.put(item.getId(), materialTypeTree);
        });

        // 构建树形结构
        List<MaterialTypeTree> list = new ArrayList<>();
        for (MtrClass type : select) {
            MaterialTypeTree treeVo = map.get(type.getId());
            if (type.getParentId() == null || type.getParentId().equals("0")) {
                // 根节点，直接添加
                list.add(treeVo);
            } else {
                // 非根节点，找到父节点并添加到其子节点列表中
                MaterialTypeTree materialTypeTree = map.get(type.getParentId());
                if (materialTypeTree != null) {
                    materialTypeTree.getChildren().add(treeVo);
                }
            }
        }
        return list;
    }

    @Override
    public long selectMtrClassListCount(MtrClass mtrClass) {
        return baseMapper.selectMtrClassListCount(mtrClass);
    }


    private String getLevel(String level) {
        level = level.replace("1", "一")
                .replace("2", "二")
                .replace("3", "三")
                .replace("4", "四")
                .replace("5", "五")
                .replace("6", "六")
                .replace("7", "七")
                .replace("8", "八")
                .replace("9", "九");
        level = level + "级";
        return level;
    }

}
