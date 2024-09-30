package com.zhaocai.business.filez.vo.res;

import java.io.Serializable;

/**
 * @className: 批注内容类
 * @description: 批注内容中的相关信息
 */
public class Content implements Serializable {
    String Uid;
    String neid;
    String commType;
    String words;
    String type;
    String commIcon;

    public String getUid() {
        return Uid;
    }

    public void setUid(String uid) {
        Uid = uid;
    }

    public String getNeid() {
        return neid;
    }

    public void setNeid(String neid) {
        this.neid = neid;
    }

    public String getCommType() {
        return commType;
    }

    public void setCommType(String commType) {
        this.commType = commType;
    }

    public String getWords() {
        return words;
    }

    public void setWords(String words) {
        this.words = words;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getCommIcon() {
        return commIcon;
    }

    public void setCommIcon(String commIcon) {
        this.commIcon = commIcon;
    }
}
