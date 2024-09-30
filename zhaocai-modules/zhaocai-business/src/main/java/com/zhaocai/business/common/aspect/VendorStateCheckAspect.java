package com.zhaocai.business.common.aspect;

import com.zhaocai.business.common.annotations.VendorStateCheck;
import com.zhaocai.business.common.enums.VendorContactStateEnum;
import com.zhaocai.business.common.exception.ParamValidateException;
import com.zhaocai.business.common.utils.ValidateUtils;
import com.zhaocai.business.vendor.domain.VendorContact;
import com.zhaocai.business.vendor.service.IVendorContactService;
import com.zhaocai.common.core.utils.NumberUtil;
import com.zhaocai.common.security.utils.SecurityUtils;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.stereotype.Component;

/**
 * 供应商状态校验 AOP
 *
 * @author chenming
 * @date 2024-07-03
 */
@Aspect
@Component
@EnableAspectJAutoProxy(proxyTargetClass = true)
public class VendorStateCheckAspect {

    @Autowired
    private IVendorContactService vendorContactService;

    @Before("@annotation(vendorStateCheck)")
    public void checkVendorState(JoinPoint joinPoint, VendorStateCheck vendorStateCheck) {
        Long loginUserId = SecurityUtils.getUserId();
        if (NumberUtil.isNullOrZero(loginUserId)) {
            throw new ParamValidateException("用户未登录，请先登录后再操作");
        }

        VendorContact vendorContact = vendorContactService.getVendorContactByLoginUser(loginUserId);
        ValidateUtils.isNullException(vendorContact,"登录用户对应的联系人不存在，请确认后操作");

        if (VendorContactStateEnum.INVALID.equalsState(vendorContact.getState())) {
            throw new ParamValidateException("您账户当前状态不可用，请先启用账户后再操作");
        }

        if (vendorStateCheck.checkManager() && vendorContact.getIsManager() != 1) {
            throw new ParamValidateException("您不是管理员，无权进行该功能的操作");
        }
    }
}
