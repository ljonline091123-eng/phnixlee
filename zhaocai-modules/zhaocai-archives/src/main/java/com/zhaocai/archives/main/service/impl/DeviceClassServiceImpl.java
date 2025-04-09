package com.zhaocai.archives.main.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhaocai.archives.dossier.service.IDeviceTypeService;
import com.zhaocai.archives.dossier.tree.DeviceTypeTree;
import com.zhaocai.archives.main.domain.DeviceClass;
import com.zhaocai.archives.main.domain.MtrClass;
import com.zhaocai.archives.main.mapper.DeviceClassMapper;
import com.zhaocai.archives.main.service.IDeviceClassService;
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
 * 设备分类主Service业务层处理
 *
 * @author lzq
 * @date 2025-01-06
 */
@Service
public class DeviceClassServiceImpl extends ServiceImpl<DeviceClassMapper, DeviceClass> implements IDeviceClassService {
    @Autowired
    private DeviceClassMapper deviceClassMapper;

    @Resource
    private IDeviceTypeService iDeviceTypeService;

    /**
     * 查询设备分类主
     *
     * @param id 设备分类主主键
     * @return 设备分类主
     */
    @Override
    public DeviceClass selectDeviceClassById(String id) {
        DeviceClass aClass = deviceClassMapper.selectDeviceClassById(id);
        if (aClass != null) {
            if (aClass.getParentId() != null && !"0".equals(aClass.getParentId())) {
                DeviceClass aClass1 = deviceClassMapper.selectDeviceClassById(aClass.getParentId());
                aClass.setBelongingLevel(aClass1.getDeviceClassName());
            } else {
                aClass.setBelongingLevel("顶级");
            }
        }
        return aClass;
    }

    /**
     * 查询设备分类主列表
     *
     * @param deviceClass 设备分类主
     * @return 设备分类主
     */
    @Override
    public List<DeviceClass> selectDeviceClassList(DeviceClass deviceClass) {
        return deviceClassMapper.selectDeviceClassList(deviceClass);
    }

    /**
     * 新增设备分类主
     *
     * @param deviceClass 设备分类主
     * @return 结果
     */
    @Override
    public synchronized int insertDeviceClass(DeviceClass deviceClass) {
        if (deviceClass.getId() == null) {
            throw new RuntimeException("请先初始化");
        }
        if (deviceClass.getParentId() == null) {
            throw new RuntimeException("父级不能为空");
        }
        //判断code是否已存在
        DeviceClass mtrClass1 = new DeviceClass();
        mtrClass1.setValid(0L);
        mtrClass1.setDeviceClassCode(deviceClass.getDeviceClassCode());
        List<DeviceClass> mtrClasses = this.selectDeviceClassList(mtrClass1);
        if (mtrClasses != null && !mtrClasses.isEmpty()) {
            throw new RuntimeException("编码已存在");
        }
        deviceClass.setCreateId(SecurityUtils.getUserId() + "");
        deviceClass.setCreateBy(SecurityUtils.getUsername());
        deviceClass.setCreateTime(DateUtils.getNowDate());
        deviceClass.setValid(0L);
        int i = deviceClassMapper.insertDeviceClass(deviceClass);
        if (i > 0) {
            iDeviceTypeService.addTypeByMain(deviceClass);
        }
        return i;
    }

    /**
     * 修改设备分类主
     *
     * @param deviceClass 设备分类主
     * @return 结果
     */
    @Override
    public synchronized int updateDeviceClass(DeviceClass deviceClass) {
        //判断code是否已存在
        DeviceClass mtrClass1 = new DeviceClass();
        mtrClass1.setValid(0L);
        mtrClass1.setDeviceClassCode(deviceClass.getDeviceClassCode());
        List<DeviceClass> mtrClasses = this.selectDeviceClassList(mtrClass1);
        if (mtrClasses != null && !mtrClasses.isEmpty()) {
            for (DeviceClass mtrClass2 : mtrClasses) {
                if (!deviceClass.getId().equals(mtrClass2.getId())) {
                    throw new RuntimeException("编码已存在");
                }
            }
        }
        deviceClass.setUpdateTime(DateUtils.getNowDate());
        deviceClass.setIsTb("2");
        int i = deviceClassMapper.updateDeviceClass(deviceClass);
        if (i > 0) {
            iDeviceTypeService.updateByHostId(deviceClass);
        }
        return i;
    }

    /**
     * 批量删除设备分类主
     *
     * @param ids 需要删除的设备分类主主键
     * @return 结果
     */
    @Override
    public boolean deleteDeviceClassByIds(String[] ids) {
        if (ids == null) {
            throw new RuntimeException("id不能为空");
        }
        List<DeviceClass> mtrFeatures = this.listByIds(Arrays.asList(ids));
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
            iDeviceTypeService.deleteByHostId(ids, map);
        }
        return b;
    }

    /**
     * 删除设备分类主信息
     *
     * @param id 设备分类主主键
     * @return 结果
     */
    @Override
    public int deleteDeviceClassById(String id) {
        return deviceClassMapper.deleteDeviceClassById(id);
    }

    @Override
    public List<DeviceTypeTree> getDeviceClassTree() {
        DeviceClass mtrClass = new DeviceClass();
        mtrClass.setValid(0L);
        PageUtils.clearPage();
        List<DeviceClass> select = baseMapper.selectDeviceClassList(mtrClass);
        Map<String, DeviceTypeTree> map = new HashMap<>();
        select.forEach(item -> {
            DeviceTypeTree materialTypeTree = new DeviceTypeTree();
            materialTypeTree.setId(item.getId() + "");
            materialTypeTree.setLabel(item.getDeviceClassName());
            materialTypeTree.setType(item.getDeviceClassType());
            materialTypeTree.setCode(item.getDeviceClassCode());
            materialTypeTree.setChildren(new ArrayList<>());
            map.put(item.getId(), materialTypeTree);
        });

        // 构建树形结构
        List<DeviceTypeTree> list = new ArrayList<>();
        for (DeviceClass type : select) {
            DeviceTypeTree treeVo = map.get(type.getId());
            if (StringUtils.isEmpty(type.getParentId()) || "0".equals(type.getParentId())) {
                // 根节点，直接添加
                list.add(treeVo);
            } else {
                // 非根节点，找到父节点并添加到其子节点列表中
                DeviceTypeTree materialTypeTree = map.get(type.getParentId());
                if (materialTypeTree != null) {
                    materialTypeTree.getChildren().add(treeVo);
                }
            }
        }
        return list;
    }

    @Override
    public DeviceClass initCode(DeviceClass deviceClass) {
        if (deviceClass.getId() == null) {
            throw new RuntimeException("id不能为空");
        }
        String materialCode = "";
        DeviceClass type = new DeviceClass();
        if ("0".equals(deviceClass.getId())) {
            String[] strs = {"A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z"};
            DeviceClass aClassx = new DeviceClass();
            aClassx.setParentId("0");
            List<DeviceClass> mtrClasses = baseMapper.selectDeviceClassList(aClassx);
            Map<String, String> map = new HashMap<>();
            for (DeviceClass mtrClass1 : mtrClasses) {
                String s = retainEnglishLetters(mtrClass1.getDeviceClassCode());
                map.put(s, s);
            }
            for (String s : strs) {
                if (StringUtils.isEmpty(map.get(s))) {
                    materialCode = s + "1";
                    break;
                }
            }
        } else {
             type = deviceClassMapper.selectDeviceClassById(deviceClass.getId());
             materialCode = type.getDeviceClassCode();
            Integer maxCode = deviceClassMapper.getMaxCode(materialCode, type.getId());
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
        DeviceClass aClass = new DeviceClass();
        aClass.setId(KeyUtils.generateId() + "");
        aClass.setParentId(deviceClass.getId());
        aClass.setDeviceClassCode(materialCode);
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
    public long selectDeviceClassListCount(DeviceClass deviceClass) {
        return deviceClassMapper.selectDeviceClassListCount(deviceClass);
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
