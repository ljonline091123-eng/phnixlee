package com.zhaocai.business.procurement.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhaocai.business.procurement.domain.MinProjectDictProjectType;
import com.zhaocai.business.procurement.vo.res.MinProjectDictProjectTypeVO;
import com.zhaocai.business.vendor.domain.VendorClassify;

import java.util.List;

public interface IMinProjectDictProjectTypeService extends IService<MinProjectDictProjectType> {
    List<MinProjectDictProjectTypeVO> getMinProjectDictProjectTypeTree();

    /*
    * 查询工程类型对应的工程类别名
    * */
    String getDictProjectTypeName(String codes);
}
