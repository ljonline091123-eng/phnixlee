package com.zhaocai.business.pub.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhaocai.business.manager.http.dto.res.DwMmAssetInfResponseDTO;
import com.zhaocai.business.manager.http.service.UnderlingSystemService;
import com.zhaocai.business.pub.domain.DwMmAssetInf;
import com.zhaocai.business.pub.mapper.DwMmAssetInfMapper;
import com.zhaocai.business.pub.service.IDwMmAssetInfService;
import com.zhaocai.common.core.utils.bean.BeanCopierUtil;
import com.zhaocai.common.redis.enums.RedisKeyPrefixEnum;
import com.zhaocai.common.redis.service.RedisService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 资产 dm0731/材料分类Service业务层处理
 *
 * @author WH
 * @date 2024-08-28
 */
@Slf4j
@Service
public class DwMmAssetInfServiceImpl extends ServiceImpl<DwMmAssetInfMapper, DwMmAssetInf> implements IDwMmAssetInfService {

    @Autowired
    private RedisService redisService;

    @Autowired
    private UnderlingSystemService underlingSystemService;

    @Override
    public void synchronizeData() {
        long beginTime = System.currentTimeMillis();

        redisService.setCacheObject(RedisKeyPrefixEnum.LOCK_FLAG_DM071.getKeyPrefix(),"dm071",1000L, TimeUnit.SECONDS);
        log.info("[dm071 数据同步] - 获取锁标识成功，开始执行数据同步操作...");
        try{
            List<DwMmAssetInfResponseDTO> dwMmAssetInfResponseList = underlingSystemService.listDwMmAssetInf();


            log.info("[dm071 数据同步] - 从底层逻辑获取数据成功，总数量为:{}",dwMmAssetInfResponseList.size());
            int deleteCount = baseMapper.deleteAll();
            log.info("[dm071 数据同步] - 删除本地数据成功，删除数据量为:{}",deleteCount);

            // 批量保存
            List<DwMmAssetInf> dwMmAssetInfList = BeanCopierUtil.copyList(dwMmAssetInfResponseList,DwMmAssetInf.class);
            dwMmAssetInfList.forEach(x -> x.setCreateTime(new Date()));
            this.saveBatch(dwMmAssetInfList,200);

             int allCount = baseMapper.countAllData();

            log.info("[dm071 数据同步] - 插入本地数据库成功，共插入数据量:{}",allCount);
        } catch (Exception ex) {
            log.error("[dm071 数据同步] - 同步数据失败，case by :{}",ex.getMessage(),ex);
        } finally {
            // 删除锁标识
            redisService.deleteObject(RedisKeyPrefixEnum.LOCK_FLAG_DM071.getKeyPrefix());
        }

        log.info("[dm071 数据同步] - 同步数据完成，共耗时:{}",(System.currentTimeMillis() - beginTime));
    }
}
