package com.zhaocai.archives.main.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhaocai.archives.dossier.service.IMaterialItemService;
import com.zhaocai.archives.main.domain.MtrClass;
import com.zhaocai.archives.main.domain.MtrFeature;
import com.zhaocai.archives.main.mapper.MtrFeatureMapper;
import com.zhaocai.archives.main.service.IMtrFeatureService;
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
 * 材料特征项主Service业务层处理
 *
 * @author lzq
 * @date 2025-01-06
 */
@Service
public class MtrFeatureServiceImpl extends ServiceImpl<MtrFeatureMapper, MtrFeature> implements IMtrFeatureService {
    @Autowired
    private MtrFeatureMapper mtrFeatureMapper;

    @Resource
    private IMaterialItemService materialItemService;

    /**
     * 查询材料特征项主
     *
     * @param id 材料特征项主主键
     * @return 材料特征项主
     */
    @Override
    public MtrFeature selectMtrFeatureById(String id) {
        return mtrFeatureMapper.selectMtrFeatureById(id);
    }

    /**
     * 查询材料特征项主列表
     *
     * @param mtrFeature 材料特征项主
     * @return 材料特征项主
     */
    @Override
    public List<MtrFeature> selectMtrFeatureList(MtrFeature mtrFeature) {
        if (mtrFeature == null) {
            mtrFeature = new MtrFeature();
        }
        mtrFeature.setValid(0L);
        return mtrFeatureMapper.selectMtrFeatureList(mtrFeature);
    }

    /**
     * 新增材料特征项主
     *
     * @param mtrFeature 材料特征项主
     * @return 结果
     */
    @Override
    public synchronized int insertMtrFeature(MtrFeature mtrFeature) {
        if (mtrFeature == null) {
            throw new RuntimeException("参数为空");
        }
        if (StringUtils.isEmpty(mtrFeature.getMtrClassId())) {
            throw new RuntimeException("类型id不能为空");
        }
        MtrFeature mtrFeature1 = new MtrFeature();
        mtrFeature1.setFeatureCode(mtrFeature.getFeatureCode());
        mtrFeature1.setValid(0L);
        mtrFeature1.setMtrClassId(mtrFeature.getMtrClassId());
        List<MtrFeature> mtrFeatures = baseMapper.selectMtrFeatureList(mtrFeature1);
        if (mtrFeatures != null && !mtrFeatures.isEmpty()) {
            throw new RuntimeException("当前特征项编号已存在");
        }
        if (mtrFeature.getId() == null) {
            mtrFeature.setId(KeyUtils.generateId() + "");
            mtrFeature.setCreateTime(DateUtils.getNowDate());
            mtrFeature.setCreateId(SecurityUtils.getUserId() + "");
            mtrFeature.setCreateBy(SecurityUtils.getUsername());
            mtrFeature.setValid(0L);
        }
        int i = mtrFeatureMapper.insertMtrFeature(mtrFeature);
//        if (i > 0) {
//            materialItemService.addByMain(mtrFeature);
//        }
        return i;
    }


    /**
     * 修改材料特征项主
     *
     * @param mtrFeature 材料特征项主
     * @return 结果
     */
    @Override
    public int updateMtrFeature(MtrFeature mtrFeature) {
        if (mtrFeature == null) {
            throw new RuntimeException("参数为空");
        }
        if (StringUtils.isEmpty(mtrFeature.getMtrClassId())) {
            throw new RuntimeException("类型id不能为空");
        }
        MtrFeature mtrFeature1 = new MtrFeature();
        mtrFeature1.setFeatureCode(mtrFeature.getFeatureCode());
        mtrFeature1.setValid(0L);
        mtrFeature1.setMtrClassId(mtrFeature.getMtrClassId());
        List<MtrFeature> mtrFeatures = baseMapper.selectMtrFeatureList(mtrFeature1);
        if (mtrFeatures != null && !mtrFeatures.isEmpty()) {
            for (MtrFeature mtrFeature2 : mtrFeatures) {
                if (!mtrFeature.getId().equals(mtrFeature2.getId())) {
                    throw new RuntimeException("当前特征项编号已存在");
                }
            }
        }
        mtrFeature.setUpdateTime(DateUtils.getNowDate());
        mtrFeature.setUpdateBy(SecurityUtils.getUsername());
        mtrFeature.setIsTb("2");
        int i = mtrFeatureMapper.updateMtrFeature(mtrFeature);
        if (i > 0) {
            materialItemService.updateByHostId(mtrFeature);
        }
        return i;
    }

    /**
     * 批量删除材料特征项主
     *
     * @param ids 需要删除的材料特征项主主键
     * @return 结果
     */
    @Override
    public boolean deleteMtrFeatureByIds(String[] ids) {
        if (ids == null) {
            throw new RuntimeException("id不能为空");
        }
        List<MtrFeature> mtrFeatures = this.listByIds(Arrays.asList(ids));
        Map<Long, Long> map = new HashMap<>();
        if (mtrFeatures != null && !mtrFeatures.isEmpty()) {
            mtrFeatures.forEach(item -> {
                item.setValid(System.currentTimeMillis() / 1000L);
                item.setIsTb("3");
                if (item.getSonId() != null) {
                    map.put(item.getSonId(), item.getSonId());
                }
            });
        }
        boolean b = this.updateBatchById(mtrFeatures);
        if (b) {
            materialItemService.deleteByHostId(ids, map);
        }
        return b;
    }

    /**
     * 删除材料特征项主信息
     *
     * @param id 材料特征项主主键
     * @return 结果
     */
    @Override
    public int deleteMtrFeatureById(String id) {
        return mtrFeatureMapper.deleteMtrFeatureById(id);
    }

    @Override
    public MtrFeature initCode(MtrClass mtrClass) {
        String code = mtrFeatureMapper.getMaxCode(mtrClass.getId());
        MtrFeature mtrFeature = new MtrFeature();
        mtrFeature.setFeatureCode(code + 1);
        mtrFeature.setMtrClassId(mtrClass.getId());
        mtrFeature.setId(KeyUtils.generateId() + "");
        mtrFeature.setCreateBy(SecurityUtils.getUsername());
        mtrFeature.setCreateId(SecurityUtils.getUserId() + "");
        mtrFeature.setCreateTime(DateUtils.getNowDate());
        return mtrFeature;
    }

    @Override
    public long selectMtrFeatureListCount(MtrFeature mtrFeature) {
        return baseMapper.selectMtrFeatureListCount(mtrFeature);
    }
}
