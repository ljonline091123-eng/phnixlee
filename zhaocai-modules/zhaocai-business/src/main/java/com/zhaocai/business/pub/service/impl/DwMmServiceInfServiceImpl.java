package com.zhaocai.business.pub.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhaocai.business.common.exception.ParamValidateException;
import com.zhaocai.business.manager.http.dto.res.DwMmServiceInfResponseDTO;
import com.zhaocai.business.manager.http.service.UnderlingSystemService;
import com.zhaocai.business.pub.domain.DwMmServiceInf;
import com.zhaocai.business.pub.mapper.DwMmServiceInfMapper;
import com.zhaocai.business.pub.service.IDwMmServiceInfService;
import com.zhaocai.business.pub.vo.res.DwMmServiceInfoVO;
import com.zhaocai.common.core.utils.StringUtils;
import com.zhaocai.common.core.utils.bean.BeanCopierUtil;
import com.zhaocai.common.redis.enums.RedisKeyPrefixEnum;
import com.zhaocai.common.redis.service.RedisService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 服务 dm073Service业务层处理
 *
 * @author WH
 * @date 2024-08-29
 */
@Slf4j
@Service
public class DwMmServiceInfServiceImpl extends ServiceImpl<DwMmServiceInfMapper, DwMmServiceInf> implements IDwMmServiceInfService {

    @Autowired
    private RedisService redisService;

    @Autowired
    private UnderlingSystemService underlingSystemService;

    @Override
    public void synchronizeData() {
        long beginTime = System.currentTimeMillis();

        redisService.setCacheObject(RedisKeyPrefixEnum.LOCK_FLAG_DM073.getKeyPrefix(), "dm073",1000L, TimeUnit.SECONDS);
        log.info("[dm073 数据同步] - 获取锁标识成功，开始执行数据同步操作...");
        try{
            List<DwMmServiceInfResponseDTO> dwMmServiceInf = underlingSystemService.listDwMmServiceInf();


            log.info("[dm073 数据同步] - 从底层逻辑获取数据成功，总数量为:{}",dwMmServiceInf.size());
            int deleteCount = baseMapper.deleteAll();
            log.info("[dm073 数据同步] - 删除本地数据成功，删除数据量为:{}",deleteCount);

            List<DwMmServiceInf> dwMmAssetInfList = BeanCopierUtil.copyList(dwMmServiceInf,DwMmServiceInf.class);
            dwMmAssetInfList.forEach(x -> x.setCreateTime(new Date()));
            this.saveBatch(dwMmAssetInfList,200);

            int allCount = baseMapper.countAllData();

            log.info("[dm073 数据同步] - 插入本地数据库成功，共插入数据量:{}",allCount);
        } catch (Exception ex) {
            log.error("[dm073 数据同步] - 同步数据失败，case by :{}",ex.getMessage(),ex);
        } finally {
            // 删除锁标识
            redisService.deleteObject(RedisKeyPrefixEnum.LOCK_FLAG_DM073.getKeyPrefix());
        }

        log.info("[dm073 数据同步] - 同步数据完成，共耗时:{}",(System.currentTimeMillis() - beginTime));
    }

    @Override
    public DwMmServiceInf getByClassCode(String serviceClassCode) {
        String flag = redisService.getCacheObject(RedisKeyPrefixEnum.LOCK_FLAG_DM073.getKeyPrefix());
        if (StringUtils.isNotBlank(flag)) {
            throw new ParamValidateException("DM073 服务类资产数据正在同步，请稍后重试");
        }

        return this.getOne(new LambdaQueryWrapper<DwMmServiceInf>()
                .eq(DwMmServiceInf::getServiceClassCode,serviceClassCode)
                .eq(DwMmServiceInf::getIsSubjectMatterCd,'Y'));
    }

    @Override
    public List<DwMmServiceInfoVO> listDwMmServiceSubjectMatter() {
        String flag = redisService.getCacheObject(RedisKeyPrefixEnum.LOCK_FLAG_DM073.getKeyPrefix());
        if (StringUtils.isNotBlank(flag)) {
            throw new ParamValidateException("DM073 服务类资产数据正在同步，请稍后重试");
        }

        List<DwMmServiceInf> serviceInfs = this.list(new LambdaQueryWrapper<DwMmServiceInf>()
                .eq(DwMmServiceInf::getIsSubjectMatterCd,"Y"));

        Map<String,DwMmServiceInfoVO> dwMmServiceInfoMap = new HashMap<>();
        DwMmServiceInfoVO serviceInfoVO;
        for (DwMmServiceInf serviceInf : serviceInfs) {
            serviceInfoVO = new DwMmServiceInfoVO(serviceInf.getServiceClassCode(),serviceInf.getServiceClassName(),new ArrayList<>());
            dwMmServiceInfoMap.put(serviceInf.getServiceClassCode(),serviceInfoVO);
        }

        List<DwMmServiceInfoVO> rootList = new ArrayList<>();
        DwMmServiceInfoVO serviceInfoParentVO;
        for (DwMmServiceInf serviceInf : serviceInfs) {
            serviceInfoVO = dwMmServiceInfoMap.get(serviceInf.getServiceClassCode());
            if (StringUtils.isBlank(serviceInf.getBelgPreServiceClassCode())) {
                rootList.add(serviceInfoVO);
            } else {
                serviceInfoParentVO = dwMmServiceInfoMap.get(serviceInf.getBelgPreServiceClassCode());
                if (serviceInfoParentVO != null) {
                    serviceInfoParentVO.getChildren().add(serviceInfoVO);
                }
            }
        }
        return rootList;
    }
}
