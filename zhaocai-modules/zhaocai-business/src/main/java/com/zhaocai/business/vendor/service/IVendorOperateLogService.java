package com.zhaocai.business.vendor.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhaocai.business.common.enums.VendorOperateLogCodeEnum;
import com.zhaocai.business.vendor.domain.VendorOperateLog;

/**
 * 供应商操作日志Service接口
 *
 * @author WH
 * @date 2024-06-25
 */
public interface IVendorOperateLogService  extends IService<VendorOperateLog> {

    /**
     * 添加供应商操作日志
     *
     * @param vendorId
     * @param businessCode
     */
    Long addVendorOperateLog(Long vendorId, VendorOperateLogCodeEnum businessCode);
}
