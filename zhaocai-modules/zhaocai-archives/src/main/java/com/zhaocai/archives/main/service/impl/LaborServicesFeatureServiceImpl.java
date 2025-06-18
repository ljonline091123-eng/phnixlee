package com.zhaocai.archives.main.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhaocai.archives.dossier.service.ILabourItemService;
import com.zhaocai.archives.main.domain.LaborServicesFeature;
import com.zhaocai.archives.main.mapper.LaborServicesFeatureMapper;
import com.zhaocai.archives.main.service.ILaborServicesFeatureService;
import com.zhaocai.archives.utils.KeyUtils;
import com.zhaocai.common.core.utils.DateUtils;
import com.zhaocai.common.core.utils.StringUtils;
import com.zhaocai.common.security.utils.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 劳务特征项主Service业务层处理
 *
 * @author lzq
 * @date 2025-01-06
 */
@Service
public class LaborServicesFeatureServiceImpl extends ServiceImpl<LaborServicesFeatureMapper, LaborServicesFeature> implements ILaborServicesFeatureService {
    @Autowired
    private LaborServicesFeatureMapper laborServicesFeatureMapper;

    @Resource
    private ILabourItemService iLabourItemService;

    /**
     * 查询劳务特征项主
     *
     * @param id 劳务特征项主主键
     * @return 劳务特征项主
     */
    @Override
    public LaborServicesFeature selectLaborServicesFeatureById(String id) {
        return laborServicesFeatureMapper.selectLaborServicesFeatureById(id);
    }

    /**
     * 查询劳务特征项主列表
     *
     * @param laborServicesFeature 劳务特征项主
     * @return 劳务特征项主
     */
    @Override
    public List<LaborServicesFeature> selectLaborServicesFeatureList(LaborServicesFeature laborServicesFeature) {
        if (laborServicesFeature == null) {
            laborServicesFeature = new LaborServicesFeature();
        }
        laborServicesFeature.setValid(0L);
        return laborServicesFeatureMapper.selectLaborServicesFeatureList(laborServicesFeature);
    }

    /**
     * 新增劳务特征项主
     *
     * @param laborServicesFeature 劳务特征项主
     * @return 结果
     */
    @Override
    public synchronized int insertLaborServicesFeature(LaborServicesFeature laborServicesFeature) {
        if (laborServicesFeature == null) {
            throw new RuntimeException("参数为空");
        }
        if (StringUtils.isEmpty(laborServicesFeature.getLaborServicesClassId())) {
            throw new RuntimeException("类型id不能为空");
        }
        LaborServicesFeature mtrFeature1 = new LaborServicesFeature();
        mtrFeature1.setFeatureCode(laborServicesFeature.getFeatureCode());
        mtrFeature1.setValid(0L);
        mtrFeature1.setLaborServicesClassId(laborServicesFeature.getLaborServicesClassId());
        List<LaborServicesFeature> mtrFeatures = baseMapper.selectLaborServicesFeatureList(mtrFeature1);
        if (mtrFeatures != null && !mtrFeatures.isEmpty()) {
            throw new RuntimeException("当前特征项编号已存在");
        }
        if (laborServicesFeature.getId() == null) {
            laborServicesFeature.setId(KeyUtils.generateId() + "");
            laborServicesFeature.setCreateTime(DateUtils.getNowDate());
            laborServicesFeature.setCreateId(SecurityUtils.getUserId() + "");
            laborServicesFeature.setCreateBy(SecurityUtils.getUsername());
            laborServicesFeature.setValid(0L);
        }
        int i = laborServicesFeatureMapper.insertLaborServicesFeature(laborServicesFeature);
//        if (i > 0) {
//            iLabourItemService.addTypeByMain(laborServicesFeature);
//        }
        return i;
    }

    /**
     * 修改劳务特征项主
     *
     * @param laborServicesFeature 劳务特征项主
     * @return 结果
     */
    @Override
    public int updateLaborServicesFeature(LaborServicesFeature laborServicesFeature) {
        if (laborServicesFeature == null) {
            throw new RuntimeException("参数为空");
        }
        if (StringUtils.isEmpty(laborServicesFeature.getLaborServicesClassId())) {
            throw new RuntimeException("类型id不能为空");
        }
        LaborServicesFeature mtrFeature1 = new LaborServicesFeature();
        mtrFeature1.setFeatureCode(laborServicesFeature.getFeatureCode());
        mtrFeature1.setValid(0L);
        mtrFeature1.setLaborServicesClassId(laborServicesFeature.getLaborServicesClassId());
        List<LaborServicesFeature> mtrFeatures = baseMapper.selectLaborServicesFeatureList(mtrFeature1);
        if (mtrFeatures != null && !mtrFeatures.isEmpty()) {
            for (LaborServicesFeature mtrFeature2 : mtrFeatures) {
                if (!laborServicesFeature.getId().equals(mtrFeature2.getId())) {
                    throw new RuntimeException("当前特征项编号已存在");
                }
            }
        }
        laborServicesFeature.setUpdateTime(DateUtils.getNowDate());
        laborServicesFeature.setUpdateBy(SecurityUtils.getUsername());
        int i = laborServicesFeatureMapper.updateLaborServicesFeature(laborServicesFeature);
        if (i > 0) {
            iLabourItemService.updateByHostId(laborServicesFeature);
        }
        return i;
    }

    /**
     * 批量删除劳务特征项主
     *
     * @param ids 需要删除的劳务特征项主主键
     * @return 结果
     */
    @Override
    public boolean deleteLaborServicesFeatureByIds(String[] ids) {
        if (ids == null) {
            throw new RuntimeException("id不能为空");
        }
        List<LaborServicesFeature> mtrFeatures = this.listByIds(Arrays.asList(ids));
        Map<Long, Long> map = new HashMap<>();
        if (mtrFeatures != null && !mtrFeatures.isEmpty()) {
            mtrFeatures.forEach(item -> {
                item.setValid(System.currentTimeMillis() / 1000L);
                if (item.getSonId() != null) {
                    map.put(item.getSonId(), item.getSonId());
                }
            });
        }
        boolean b = this.updateBatchById(mtrFeatures);
        if (b) {
            iLabourItemService.deleteByHostId(ids,map);
        }
        return b;
    }

    /**
     * 删除劳务特征项主信息
     *
     * @param id 劳务特征项主主键
     * @return 结果
     */
    @Override
    public int deleteLaborServicesFeatureById(String id) {
        return laborServicesFeatureMapper.deleteLaborServicesFeatureById(id);
    }

    @Override
    public long selectLaborServicesFeatureListCount(LaborServicesFeature laborServicesFeature) {
        return baseMapper.selectLaborServicesFeatureListCount(laborServicesFeature);
    }
}
