package com.zhaocai.business.pub.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import com.zhaocai.business.common.exception.BusinessException;
import com.zhaocai.business.pub.service.ISysDictDataService;
import com.zhaocai.business.pub.vo.res.DictListVO;
import com.zhaocai.common.core.constant.SecurityConstants;
import com.zhaocai.common.core.utils.StringUtils;
import com.zhaocai.common.redis.enums.RedisKeyPrefixEnum;
import com.zhaocai.common.redis.service.RedisService;
import com.zhaocai.system.api.domain.SysDictData;
import com.zhaocai.system.api.system.RemoteSystemDictDataService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 字典数据Service业务层处理
 *
 * @author WH
 * @date 2024-06-03
 */
@Slf4j
@Service
public class SysDictDataServiceImpl implements ISysDictDataService {

    @Autowired
    private RemoteSystemDictDataService remoteSystemDictDataService;

    @Autowired
    private RedisService redisService;

    @Override
    public String getLabel(String type, String value) {
        if (StringUtils.isBlank(type) || StringUtils.isBlank(value)) {
            return null;
        }

        String cacheKey = RedisKeyPrefixEnum.DICT_DATA.getKeyPrefix() + type;
        String label = redisService.getCacheMapValue(cacheKey,value);
        if (StringUtils.isBlank(label)) {
            // 为空，则从数据库中获取，然后加入缓存中
            List<SysDictData> result = remoteSystemDictDataService.listDictDataLabel(type,value, SecurityConstants.INNER);
            if (CollectionUtil.isNotEmpty(result) && result.size() > 1)  {
                throw new BusinessException(type + "-" + value + "，的字典记录有多条");
            }

            label = CollectionUtil.isNotEmpty(result) ? result.get(0).getDictLabel() : null;
            // 加入缓存
            redisService.setCacheMapValue(cacheKey,value,label);
        }
        return label;
    }

    @Override
    public List<DictListVO> listDictByType(String type) {
        if (StringUtils.isBlank(type)) {
            return new ArrayList<>();
        }

        List<SysDictData> result = remoteSystemDictDataService.listDictByType(type, SecurityConstants.INNER);

        if (CollectionUtil.isNotEmpty(result)) {
            return result.stream()
                    .map(x -> new DictListVO(x.getDictLabel(),x.getDictValue(),0, x.getRemark())).collect(Collectors.toList());
        }
        return new ArrayList<>();
    }

    @Override
    public String getRemark(String type, String value ,String classVaule) {
        if (StringUtils.isBlank(type) || StringUtils.isBlank(value)) {
            return null;
        }
        List<SysDictData> result = remoteSystemDictDataService.listDictDataLabel(type,value, SecurityConstants.INNER);
        if (CollectionUtil.isNotEmpty(result))  {
            if("label".equals(classVaule)){
                String label = CollectionUtil.isNotEmpty(result) ? result.get(0).getDictLabel() : null;
                return label;
            }
            String remark = CollectionUtil.isNotEmpty(result) ? result.get(0).getRemark() : null;
            return remark;
        }
        return null;
    }
}
