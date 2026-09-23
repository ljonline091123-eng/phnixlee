package com.zhaocai.business.filez.vo.res;

import java.io.Serializable;

/**
 * @className: 批注数据类
 * @description: 文档批注的相关信息
 */
public class ResultData implements Serializable {

    String neid;
    String nsid;
    String type;
    String createId;
    long ctime;
    String docsId;
    int id;
    int state;
    Content content;

    public String getNeid() {
        return neid;
    }

    public void setNeid(String neid) {
        this.neid = neid;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getNsid() {
        return nsid;
    }

    public void setNsid(String nsid) {
        this.nsid = nsid;
    }

    public String getCreateId() {
        return createId;
    }

    public void setCreateId(String createId) {
        this.createId = createId;
    }

    public long getCtime() {
        return ctime;
    }

    public void setCtime(long ctime) {
        this.ctime = ctime;
    }

    public String getDocsId() {
        return docsId;
    }

    public void setDocsId(String docsId) {
        this.docsId = docsId;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getState() {
        return state;
    }

    public void setState(int state) {
        this.state = state;
    }

    public Content getContent() {
        return content;
    }

    public void setContent(Content content) {
        this.content = content;
    }
}
