package com.example.mockserver.service;

import com.example.mockserver.config.MockDB;
import com.example.mockserver.entity.Resp;
import com.netflix.appinfo.ApplicationInfoManager;
import com.netflix.appinfo.EurekaInstanceConfig;
import com.netflix.appinfo.InstanceInfo;
import com.netflix.appinfo.MyDataCenterInstanceConfig;
import com.netflix.discovery.DefaultEurekaClientConfig;
import com.netflix.discovery.DiscoveryClient;
import com.netflix.discovery.EurekaClient;
import com.netflix.discovery.EurekaClientConfig;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.mockserver.configuration.Configuration;
import org.mockserver.integration.ClientAndServer;
import org.springframework.cloud.netflix.eureka.EurekaDiscoveryClient;
import org.springframework.stereotype.Service;

import javax.imageio.spi.ServiceRegistry;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.mockserver.integration.ClientAndServer.startClientAndServer;

@Slf4j
@Service
public class MockService {

    public boolean create(Integer port) {

        if (MockDB.mocks.containsKey(port)) {
            return false;
        }
        String jsonPath = portFile(port);
        Configuration c = Configuration.configuration().initializationJsonPath(jsonPath).persistedExpectationsPath(jsonPath);
        try {
            ClientAndServer mockServer = startClientAndServer(c, port);
            return MockDB.mocks.putIfAbsent(port, mockServer) == null;
        } catch (Exception e) {
            log.error("", e);
            return false;
        }
    }

    public boolean stop(Integer port) {
        Optional.ofNullable(MockDB.mocks.remove(port)).map(x -> x.stop(true));
        return true;
    }

    public boolean change(Integer source, Integer target) throws IOException {
        if (new File(portFile(target)).exists() || !new File(portFile(source)).exists()) {
            return false;
        }
        stop(source);
        FileUtils.moveFile(new File(portFile(source)), new File(portFile(target)));
        boolean b = false;
        try {
            b = create(target);
        } catch (Exception e) {
            log.info("change", e);
        }
        if (!b) {
            FileUtils.moveFile(new File(portFile(target)), new File(portFile(source)));
            create(source);
        }
        return true;
    }

    private String portFile(Integer port) {
        return port + ".json";
    }


    private static final Pattern p = Pattern.compile("^(\\d+).json$");

    public Map<Integer, File> list() {

        Map<Integer, File> result = new HashMap<>();
        List<File> files = Arrays.stream(new File(".").listFiles()).filter(x -> x.getName().matches("^\\d+.json$")).toList();
        for (File file : files) {
            Matcher matcher = p.matcher(file.getName());
            if (matcher.find()) {
                result.put(Integer.parseInt(matcher.group(1)), file);
            }
        }
        return result;
    }

}
