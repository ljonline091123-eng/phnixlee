package com.zhaocai.archives.dossier.tree;

import java.io.Serializable;
import java.util.List;

/**
 * 材料类型树
 *
 * @author lzq
 * @date 2025-01-06
 */
public class SubcontractingTypeTree implements Serializable {

    private static final long serialVersionUID = 1L;

    private String id;

    private String label;

    private String type;

    private String code;

    private Long state;

    private String mainId;

    private String isMain;

    private String businessId;

    private String processId;

    private List<SubcontractingTypeTree> children;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public List<SubcontractingTypeTree> getChildren() {
        return children;
    }

    public void setChildren(List<SubcontractingTypeTree> children) {
        this.children = children;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public Long getState() {
        return state;
    }

    public void setState(Long state) {
        this.state = state;
    }

    public String getMainId() {
        return mainId;
    }

    public void setMainId(String mainId) {
        this.mainId = mainId;
    }

    public String getIsMain() {
        return isMain;
    }

    public void setIsMain(String isMain) {
        this.isMain = isMain;
    }

    public String getBusinessId() {
        return businessId;
    }

    public void setBusinessId(String businessId) {
        this.businessId = businessId;
    }

    public String getProcessId() {
        return processId;
    }

    public void setProcessId(String processId) {
        this.processId = processId;
    }
}
