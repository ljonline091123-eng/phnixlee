package com.zhaocai.business.poi.controller;

import com.zhaocai.business.poi.service.WordService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;


@RestController
@RequestMapping("/agreementWord")
public class WordController {

    private final WordService wordService;

    public WordController(WordService wordService) {
        this.wordService = wordService;
    }

    /**
     *  http://127.0.0.1:8052/business/agreementWord/generate?id=1888879000955854849
     *
     * @param id
     * @param response
     * @throws Exception
     */
    @GetMapping("/generate")
    public void generateWord(@RequestParam Long id, @RequestParam(required = false) String type, HttpServletResponse response) throws Exception {
        wordService.generateWord(id,type,response);
    }

    @GetMapping("/generatePlan")
    public void generateWordPlan(@RequestParam Long id, @RequestParam(required = false) String type, HttpServletResponse response) throws Exception {
        wordService.generateWordPlan(id,type,response);
    }
}
