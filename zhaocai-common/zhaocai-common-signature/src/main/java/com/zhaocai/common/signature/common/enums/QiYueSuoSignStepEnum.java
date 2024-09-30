package com.zhaocai.common.signature.common.enums;


import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 契约锁签署步骤
 *
 * @author chenming
 * @date 2024-09-25
 */
@AllArgsConstructor
@Getter
public enum QiYueSuoSignStepEnum {
    CREATE_BY_FILE("create_by_file", "创建合同文件"),
    CREATE_BY_CATEGORY("create_by_category", "创建合同文件"),
    TO_SIGN("to_sign","待签署"),
    SIGN_FINISHED("sign_finished","签署完成"),
    ;

    private final String step;

    private final String stepName;

    public Boolean equalsStep(String step) {
        return this.getStep().equals(step);
    }
}
