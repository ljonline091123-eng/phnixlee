package com.zhaocai.business.pub.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhaocai.business.pub.domain.DwMmServiceInf;
import com.zhaocai.business.pub.vo.res.DwMmServiceInfoVO;

import java.util.List;

/**
 * 服务 dm073Service接口
 *
 * @author WH
 * @date 2024-08-29
 */
public interface IDwMmServiceInfService  extends IService<DwMmServiceInf> {
    /**
     * 同步数据
     */
    void synchronizeData();

    /**
     * 根据资产分类编码获取
     * @param serviceClassCode
     * @return
     */
    DwMmServiceInf getByClassCode(String serviceClassCode);

    /**
     * 获取服务类为交易标的物的资产
     * @return
     */
    List<DwMmServiceInfoVO> listDwMmServiceSubjectMatter();
}
