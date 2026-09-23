package com.zhaocai.business.filez.vo.res;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * @className: 文档基本数据类
 * @description: 文档基本的相关信息
 */
@Data
@NoArgsConstructor
public class FileData implements Serializable {

    private String type;
    private String author;
    private String owner;
    private String commentsid;
    private String link;
    private String content;
    private String fileid;
    private String filename;
    private String[] mentionList;

    @Builder
    public FileData(String type, String author, String owner, String commentsid, String link, String content, String fileid, String filename, String[] mentionList) {
        this.type = type;
        this.author = author;
        this.owner = owner;
        this.commentsid = commentsid;
        this.link = link;
        this.content = content;
        this.fileid = fileid;
        this.filename = filename;
        this.mentionList = mentionList;
    }
}
