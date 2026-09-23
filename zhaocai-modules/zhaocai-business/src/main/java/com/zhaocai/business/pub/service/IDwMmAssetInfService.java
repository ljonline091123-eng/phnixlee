package com.zhaocai.business.pub.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhaocai.business.pub.domain.DwMmAssetInf;

/**
 * 资产 dm0731/材料分类Service接口
 *
 * @author chenming
 * @date 2024-08-28
 */
public interface IDwMmAssetInfService  extends IService<DwMmAssetInf> {

    /**
     * 同步数据
     */
    void synchronizeData();
}
