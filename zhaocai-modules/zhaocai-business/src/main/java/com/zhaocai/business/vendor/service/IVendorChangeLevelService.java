package com.zhaocai.business.vendor.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhaocai.business.process.service.IPBMOverrideService;
import com.zhaocai.business.process.service.IProcessBusinessBaseService;
import com.zhaocai.business.vendor.domain.VendorChange;
import com.zhaocai.business.vendor.vo.req.VendorLevelRequestVO;

/**
 * 供应商修改等级变更Service接口
 *
 * @author lsn
 * @date 2024-08-15
 */
public interface IVendorChangeLevelService extends IService<VendorChange>, IProcessBusinessBaseService, IPBMOverrideService {

    /**
     * 保存供应商变更等级信息
     *
     * @param requestVO
     */
    void saveVendorLevel(VendorLevelRequestVO requestVO);
}
