package com.zhaocai.business.pub.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhaocai.business.common.enums.BusinessCodeEnum;
import com.zhaocai.business.common.exception.NotFoundException;
import com.zhaocai.business.pub.domain.BusinessCode;
import com.zhaocai.business.pub.mapper.BusinessCodeMapper;
import com.zhaocai.business.pub.service.IBusinessCodeService;
import com.zhaocai.common.core.utils.DateUtils;
import com.zhaocai.common.core.utils.StringUtils;
import org.springframework.stereotype.Service;

/**
 * 系统业务编号规则Service业务层处理
 *
 * @author WH
 * @date 2024-05-27
 */
@Service
public class BusinessCodeServiceImpl extends ServiceImpl<BusinessCodeMapper,BusinessCode> implements IBusinessCodeService {

    @Override
    public String getBusinessCode(BusinessCodeEnum businessCodeEnum) {
        //由于要保证每个业务类型它的编码值是递增的，所以这里使用锁

        // Todo:这里需要使用 Redis 分布式锁
        synchronized (businessCodeEnum.getBusinessCode().intern()) {
            // 同一个 businessCode 共用一把锁
            BusinessCode businessCode = baseMapper.selectOne(new LambdaQueryWrapper<BusinessCode>()
                    .eq(BusinessCode::getBusinessCode, businessCodeEnum.getBusinessCode()));
            if (businessCode == null) {
                throw new NotFoundException(businessCodeEnum.getBusinessName() + "对应的编码配置不存在");
            }

            String nowDateType = businessCode.getNowDateType();
            String nowDate = businessCode.getNowDate();

            // 当前日期
            String currentDate = DateUtils.dateTimeNow(nowDateType);
            long number = !currentDate.equals(nowDate) ? 1L : businessCode.getCodeNumber() + 1;

            baseMapper.updateCodeNumberIncr(businessCode.getId(),number,currentDate);
            return StringUtils.leftPad(String.valueOf(number),businessCode.getCodeNumberLength(),"0");
        }
    }
}
