package com.zhaocai.business.filez.service.impl;

import com.zhaocai.business.filez.dto.UpdateBookmarkRefRequestDTO;
import com.zhaocai.business.filez.service.AbstractDocumentUpdateService;
import com.zhaocai.business.filez.service.dto.FileZRequestBaseOps;
import com.zhaocai.business.filez.service.dto.FileZRequestContext;
import com.zhaocai.business.filez.service.dto.UpdateBookmarkRefRequestOps;
import com.zhaocai.common.core.utils.bean.BeanCopierUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 书签内容替换
 *
 * @author chenming
 * @date 2024-07-26
 */
@Service
@Slf4j
public class UpdateBookmarkRefService extends AbstractDocumentUpdateService {

    @Override
    protected List<FileZRequestBaseOps> buildOps(FileZRequestContext requestContext) {
        UpdateBookmarkRefRequestDTO refRequestDTO = (UpdateBookmarkRefRequestDTO) requestContext.getFileZRequest();

        List<UpdateBookmarkRefRequestOps.UpdateBookmarkRefArgs> refArgsList = BeanCopierUtil.copyList(refRequestDTO.getBookmarkRefList(),UpdateBookmarkRefRequestOps.UpdateBookmarkRefArgs.class);
        UpdateBookmarkRefRequestOps.UpdateBookmarkRefOps refOps = new UpdateBookmarkRefRequestOps.UpdateBookmarkRefOps(refArgsList);

        UpdateBookmarkRefRequestOps updateBookmarkRefRequestOps = new UpdateBookmarkRefRequestOps();
        updateBookmarkRefRequestOps.setOptions(refOps);

        List<FileZRequestBaseOps> opsList = new ArrayList<>();
        opsList.add(updateBookmarkRefRequestOps);

        return opsList;
    }
}
