package com.zhaocai.business.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Objects;

/**
 * @author ssy
 * @date 2024/7/24 11:01
 */
@Getter
@AllArgsConstructor
public enum ProcurementPlanTypeEnum {

    /**
     * 采购计划类型
     */

    PURCHASE_MATERIALS(1, "购买材料"),
    LEASED_MATERIAL(2, "租赁材料"),
    RENTAL_MACHINERY(3, "租赁机械（设备）"),
    SPECIALTY_SUBCONTRACT(4, "专业分包"),
    SERVICE_SUBCONTRACT(5, "劳务分包"),
    OTHER_TYPE(6, "其他"),

    ;

    private final Integer type;

    private final String desc;

    /**
     * 是否为劳务分包或专业分包
     * @return
     */
    public static boolean isSubService(Integer type) {
        return SPECIALTY_SUBCONTRACT.equalsType(type) || SERVICE_SUBCONTRACT.equalsType(type);
    }

    public Boolean equalsType(Integer type) {
        return this.getType().equals(type);
    }

    /**
     * 根据编码查询枚举
     *
     * @param type
     * @return
     */
    public static String getValueByCode(Integer type) {
        if (Objects.isNull(type)) {
            return null;
        }
        for (ProcurementPlanTypeEnum typeEnum : values()) {
            if (typeEnum.type.equals(type)) {
                return typeEnum.getDesc();
            }
        }
        return null;
    }


    public String getProcessType() {
        switch (this.type){
            case 1:return "C";
            case 2:return "D";
            case 3:return "G";
            case 4:return "B";
            case 5:return "A";
            case 6:return "Z";
        }
        return null;
    }
    public static String getProcessType(Integer type) {
        switch (type){
            case 1:return "C";
            case 2:return "D";
            case 3:return "G";
            case 4:return "B";
            case 5:return "A";
            case 6:return "Z";
        }
        return null;
    }

    /**
     * 是否可拆分 <br/>
     * 购买材料、租赁材料、租赁机械（设备）、其他 四类不可拆分
     * @param type
     * @return
     */
    public static Boolean isSplit(Integer type) {
        return PURCHASE_MATERIALS.equalsType(type) ||
                LEASED_MATERIAL.equalsType(type) ||
                RENTAL_MACHINERY.equalsType(type);

    }

    /**
     * 是否为租赁
     * @param type
     * @return
     */
    public static Boolean isRent(Integer type) {
        return LEASED_MATERIAL.equalsType(type) || RENTAL_MACHINERY.equalsType(type);
    }

    /**
     * 是否为材料
     * @param type
     * @return
     */
    public static Boolean isMaterials(Integer type) {
        return PURCHASE_MATERIALS.equalsType(type) ||
                LEASED_MATERIAL.equalsType(type);
    }

    /**
     * 是否为服务
     * @param type
     * @return
     */
    public static Boolean isService(Integer type) {
        return SPECIALTY_SUBCONTRACT.equalsType(type) ||
                SERVICE_SUBCONTRACT.equalsType(type);
    }
}
