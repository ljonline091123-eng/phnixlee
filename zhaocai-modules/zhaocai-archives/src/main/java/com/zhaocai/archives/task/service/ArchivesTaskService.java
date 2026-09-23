package com.zhaocai.archives.task.service;

/**
 * @author liaozhiqin
 * @date 2025/1/17
 */
public interface ArchivesTaskService {
    boolean synchronizeMasterData();

    boolean pushMiddlePlatform();
}
