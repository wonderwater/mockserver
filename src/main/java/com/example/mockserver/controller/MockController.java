package com.example.mockserver.controller;

import com.example.mockserver.config.MockDB;
import com.example.mockserver.entity.Overview;
import com.example.mockserver.entity.Resp;
import com.example.mockserver.service.MockService;
import com.jayway.jsonpath.JsonPath;
import lombok.extern.slf4j.Slf4j;
import org.mockserver.matchers.MatchType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/mock")
public class MockController {

    @Autowired
    private MockService mockService;

    @PostMapping("/create")
    public Resp createPort(@RequestParam("port") int port) {
        try {
            boolean b = mockService.create(port);
            return Resp.newResp(b);
        } catch (Exception e) {
            log.error("", e);
            return Resp.fail(e.getMessage());
        }
    }

    @PostMapping("/stop")
    public Resp stop(@RequestParam("port") Integer port) {
        return Resp.newResp(mockService.stop(port));
    }

    @GetMapping("/list")
    public Resp<List<Overview>> list() {
        List<Overview> list = new ArrayList<>();
        for (Map.Entry<Integer, File> entry : mockService.list().entrySet()) {
            try {
                log.info("read {}", entry.getValue());
                List<String> paths = JsonPath.parse(entry.getValue()).read("$..path");
                Overview overview = new Overview(MockDB.mocks.containsKey(entry.getKey()), entry.getKey(), paths);
                list.add(overview);
            } catch (IOException e) {
                log.info("error", e);
            }
        }
        return Resp.succBody(list);
    }



}
