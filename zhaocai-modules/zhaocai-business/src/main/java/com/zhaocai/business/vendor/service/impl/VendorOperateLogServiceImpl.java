package com.zhaocai.business.vendor.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhaocai.business.common.enums.VendorOperateLogCodeEnum;
import com.zhaocai.business.vendor.domain.VendorOperateLog;
import com.zhaocai.business.vendor.mapper.VendorOperateLogMapper;
import com.zhaocai.business.vendor.service.IVendorOperateLogService;
import org.springframework.stereotype.Service;

/**
 * 供应商操作日志Service业务层处理
 *
 * @author WH
 * @date 2024-06-25
 */
@Service
public class VendorOperateLogServiceImpl extends ServiceImpl<VendorOperateLogMapper, VendorOperateLog> implements IVendorOperateLogService {

    @Override
    public Long addVendorOperateLog(Long vendorId, VendorOperateLogCodeEnum businessCode) {
         VendorOperateLog operateLog = new VendorOperateLog();
         operateLog.setVendorId(vendorId);
         operateLog.setBusinessCode(businessCode.getBusinessCode());
         operateLog.setState(0);

         super.save(operateLog);
         return operateLog.getId();
    }
}
