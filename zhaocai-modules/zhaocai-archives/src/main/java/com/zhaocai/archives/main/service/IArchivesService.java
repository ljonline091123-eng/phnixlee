package com.zhaocai.archives.main.service;

import com.github.pagehelper.PageInfo;
import com.zhaocai.archives.main.domain.MaterialsVO;
import com.zhaocai.archives.main.vo.req.ArchivesDetailQueryVO;
import com.zhaocai.archives.main.vo.res.ArchivesDetail;

import java.util.List;

public interface IArchivesService {
    List<ArchivesDetail> getArchivesDetailList(ArchivesDetailQueryVO queryVO);

    void matchList(List<MaterialsVO> list);

    List<ArchivesDetail> getArchivesDetailListTwo(ArchivesDetailQueryVO queryVO);
}
