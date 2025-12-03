package com.zhaocai.business.poi.service;



import javax.servlet.http.HttpServletResponse;

public interface WordService {
    void generateWord(Long id, String type, HttpServletResponse response) throws Exception;

    void generateWordPlan(Long id, String type, HttpServletResponse response) throws Exception;
}

