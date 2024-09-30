package com.zhaocai.business.common.conver;

import com.zhaocai.business.common.exception.ParamValidateException;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 采购方式类型转换器
 *
 * @author chenming
 * @date 2024-07-09
 */
public class ProcurementPlanTypeConver {

    private static final Map<String,Integer> TO_PROCUREMENT_PLAN_TYPE= new HashMap<>();

    static {
        // 劳务分包
        TO_PROCUREMENT_PLAN_TYPE.put("A",5);

        // 专业分包
        TO_PROCUREMENT_PLAN_TYPE.put("B",4);

        // 物资采购
        TO_PROCUREMENT_PLAN_TYPE.put("C",1);

        // 物资租赁
        TO_PROCUREMENT_PLAN_TYPE.put("D",2);

        // 机械租赁
        TO_PROCUREMENT_PLAN_TYPE.put("G",3);

        // 其他
        TO_PROCUREMENT_PLAN_TYPE.put("Z",6);
    }

    private static final Map<Integer,String> FROM_PROCUREMENT_PLAN_TYPE= new HashMap<>();

    static {
        // 劳务分包
        FROM_PROCUREMENT_PLAN_TYPE.put(5,"A");

        // 专业分包
        FROM_PROCUREMENT_PLAN_TYPE.put(4,"B");

        // 物资采购
        FROM_PROCUREMENT_PLAN_TYPE.put(1,"C");

        // 物资租赁
        FROM_PROCUREMENT_PLAN_TYPE.put(2,"D");

        // 机械租赁
        FROM_PROCUREMENT_PLAN_TYPE.put(3,"G");

        // 其他
        FROM_PROCUREMENT_PLAN_TYPE.put(6,"Z");
    }

    public static Integer converToProcurementPlanType(String type) {
        Integer procurementType = TO_PROCUREMENT_PLAN_TYPE.get(type);
        if (procurementType == null) {
            throw new ParamValidateException("采购类型参数有误，类型为[" + type + "]的类型在系统中未定义");
        }
        return procurementType;
    }

    public static String converFromProcurementPlanType(Integer procurementType) {
        if (procurementType != null) {
            return Optional.ofNullable(FROM_PROCUREMENT_PLAN_TYPE.get(procurementType))
                    .orElseThrow(() -> new ParamValidateException("采购类型参数有误，类型为[" + procurementType + "]的类型在系统中未定义"));
        }
        return null;
    }
}
