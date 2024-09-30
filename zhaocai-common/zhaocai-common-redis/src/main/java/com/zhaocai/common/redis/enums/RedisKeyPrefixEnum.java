package com.zhaocai.common.redis.enums;

/**
 * Redis key 前缀
 *
 * @author chenming
 * @date 2024-09-05
 */
public enum RedisKeyPrefixEnum {

    LOCK_FLAG_DM071("zhaocai:lockFlag:dm071","DM071 资产数据同步标识锁"),
    LOCK_FLAG_DM073("zhaocai:lockFlag:dm073","DM073 服务数据同步标识锁"),
    SUBMIT_REPEAT("zhaocai:submit-repeat:","重复提交标识"),
    AGREEMENT_MATERIALS_SIGN_DATA("zhaocai:agreement:materials-sign-data:","合同签订数据"),
    DICT_DATA("zhaocai:dict-data:","字典数据"),
    ;



    private String keyPrefix;

    private String desc;

    RedisKeyPrefixEnum(String keyPrefix,String desc) {
        this.keyPrefix = keyPrefix;
        this.desc = desc;
    }

    public String getKeyPrefix() {
        return keyPrefix;
    }

    public String getDesc() {
        return desc;
    }
}
