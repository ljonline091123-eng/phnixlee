package com.zhaocai.system.api.domain;


public class SetConfigValueDTO {

    /**
     * key
     */
    private String configKey;

    /**
     * value
     */
    private String configValue;

    public SetConfigValueDTO(){

    }

    public SetConfigValueDTO(String configKey, String configValue) {
        this.configKey = configKey;
        this.configValue = configValue;
    }

    public String getConfigKey() {
        return configKey;
    }

    public void setConfigKey(String configKey) {
        this.configKey = configKey;
    }

    public String getConfigValue() {
        return configValue;
    }

    public void setConfigValue(String configValue) {
        this.configValue = configValue;
    }
}
