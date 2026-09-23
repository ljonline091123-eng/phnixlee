package com.zhaocai.business.pub.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhaocai.business.common.enums.BusinessCodeEnum;
import com.zhaocai.business.pub.domain.BusinessCode;

/**
 * 系统业务编号规则Service接口
 * 
 * @author WH
 * @date 2024-05-27
 */
public interface IBusinessCodeService  extends IService<BusinessCode> {

    /**
     * 获取业务编码
     * @param businessCodeEnum
     * @return
     */
    String getBusinessCode(BusinessCodeEnum businessCodeEnum);
}
