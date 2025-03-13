package com.zhaocai.archives.pub;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 物料库分类枚举类
 *
 * @author cff
 */
@AllArgsConstructor
@Getter
public enum ArchivesTypeEnum {

//    { categoryName: '材料类', categoryCode: '1' },
//    { categoryName: '设备类', categoryCode: '2' },
//    { categoryName: '劳务类', categoryCode: '3' },
//    { categoryName: '专业分包类', categoryCode: '4' },

    /** 账户枚举类 */
    Mtr_Class("1","材料类"),

    Device_Feature("2","设备类"),

    Labor_Services("3","劳务类"),

    Major_Subcontracting("4","专业分包类"),
;
    private final String type;

    private final String desc;
}
