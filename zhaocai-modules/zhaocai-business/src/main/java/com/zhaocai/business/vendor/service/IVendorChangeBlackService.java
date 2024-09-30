package com.zhaocai.business.vendor.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhaocai.business.process.service.IProcessBusinessBaseService;
import com.zhaocai.business.vendor.domain.VendorChange;
import com.zhaocai.business.vendor.vo.req.VendorBlackRequestVO;

/**
 * 供应商黑名单变更Service接口
 *
 * @author lsn
 * @date 2024-08-08
 */
public interface IVendorChangeBlackService extends IService<VendorChange> , IProcessBusinessBaseService {

    /**
     * 保存供应商黑名单变更信息
     * @param requestVO
     */
    void saveVendorBlack(VendorBlackRequestVO requestVO);

}
