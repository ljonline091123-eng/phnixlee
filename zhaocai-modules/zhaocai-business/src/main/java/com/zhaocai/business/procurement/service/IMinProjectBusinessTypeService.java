package com.zhaocai.business.procurement.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhaocai.business.procurement.domain.MinProjectBusinessType;
import com.zhaocai.business.procurement.domain.MinProjectDictProjectType;
import com.zhaocai.business.procurement.vo.res.MinProjectBusinessTypeVO;

import java.util.List;

public interface IMinProjectBusinessTypeService extends IService<MinProjectBusinessType> {
    List<MinProjectBusinessTypeVO> getVendorClassifyTree();

    /*
    * 查询工程类型对应的工程类别名
    * */
    String getMinProjectBusinessTypeName(String codes);
}
