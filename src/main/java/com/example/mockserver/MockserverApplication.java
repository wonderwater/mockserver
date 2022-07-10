package com.example.mockserver;

import com.example.mockserver.service.MockService;
import com.netflix.appinfo.ApplicationInfoManager;
import com.netflix.appinfo.EurekaInstanceConfig;
import com.netflix.appinfo.InstanceInfo;
import com.netflix.appinfo.LeaseInfo;
import com.netflix.discovery.AbstractDiscoveryClientOptionalArgs;
import com.netflix.discovery.DiscoveryClient;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RegExUtils;
import org.mockserver.configuration.ConfigurationProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.commons.util.InetUtils;
import org.springframework.cloud.commons.util.InetUtilsProperties;
import org.springframework.cloud.netflix.eureka.EurekaClientConfigBean;
import org.springframework.cloud.netflix.eureka.EurekaInstanceConfigBean;
import org.springframework.cloud.netflix.eureka.InstanceInfoFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.mockserver.integration.ClientAndServer.startClientAndServer;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.JsonBody.json;
import static org.mockserver.model.NottableString.string;


@Slf4j
@SpringBootApplication
public class MockserverApplication {


    public static void main(String[] args) {
        ConfigurationProperties.enableCORSForAPI(true);
        ConfigurationProperties.enableCORSForAllResponses(true);
        ConfigurationProperties.corsAllowOrigin("*");
        ConfigurationProperties.corsAllowMethods("CONNECT, DELETE, GET, HEAD, OPTIONS, POST, PUT, PATCH, TRACE");
        ConfigurationProperties.corsAllowHeaders("Allow, Content-Encoding, Content-Length, Content-Type, ETag, Expires, Last-Modified, Location, Server, Vary, Authorization");
        ConfigurationProperties.corsMaxAgeInSeconds(300);

        ConfigurationProperties.persistExpectations(true);
        String path = "t.json";
        ConfigurationProperties.persistedExpectationsPath(path);
        ConfigurationProperties.initializationJsonPath(path);

        SpringApplication.run(MockserverApplication.class, args);
    }


    @Autowired
    private MockService mockService;

    /**
     * Eureka application name / instance prefix
     */
    private final static String APPLICATION_NAME = "my-app";

    @Autowired
    private EurekaInstanceConfigBean originalInstanceConfig;

    @Autowired
    private EurekaClientConfigBean originalClientConfig;

    @Autowired
    private ApplicationInfoManager applicationInfoManager;

    @Autowired
    private ApplicationContext context;

    @Autowired
    private AbstractDiscoveryClientOptionalArgs args;

    @Value("${HOSTNAME:localhost}")
    private String hostName;

    @Value("${my.app.numInstances:5}")
    private int numInstances;

    private final Set<DiscoveryClient> discoveryClients = new HashSet<>();

    @PostConstruct
    public void t() {

        for (Integer port : mockService.list().keySet()) {
            log.info("start {}", port);
            mockService.create(port);
            DiscoveryClient discoveryClient =
                    new DiscoveryClient(
                            duplicateAppInfoManager(port.toString(), port),
                            duplicateConfig(),
                            args);
            discoveryClients.add(discoveryClient);
        }
    }

    /**
     * Handles forcing all the extra instances we register to DOWN state when the
     * application is terminated.
     */
    @Bean
    public ApplicationListener discoveryClientShutdownListener()
    {
        return applicationEvent -> {
            if(applicationEvent instanceof ContextClosedEvent)
            {
                for(DiscoveryClient client: discoveryClients)
                {
                    log.info("Shutting down discovery client for " + client.getApplicationInfoManager().getEurekaInstanceConfig().getInstanceId());
                    client.shutdown();
                }
            }
        };
    }

    //
    // These duplicateXXX methods are used to create copies of the
    // various Eureka beans that the Spring Cloud auto config
    // creates for us.
    //

    private ApplicationInfoManager duplicateAppInfoManager(String appName, int port)
    {
        EurekaInstanceConfigBean newInstanceConfig =
                new EurekaInstanceConfigBean(new InetUtils(new InetUtilsProperties()));

//        newInstanceConfig.setEnvironment(context.getEnvironment());
        newInstanceConfig.setAppname(appName);
        newInstanceConfig.setInstanceId(newInstanceConfig.getIpAddress() + ":" + appName + ":" + port);
        newInstanceConfig.setInitialStatus(InstanceInfo.InstanceStatus.UP);
        newInstanceConfig.setNonSecurePortEnabled(originalInstanceConfig.isNonSecurePortEnabled());
        newInstanceConfig.setNonSecurePort(port);
        newInstanceConfig.setSecurePortEnabled(originalInstanceConfig.isSecurePortEnabled());
        newInstanceConfig.setSecurePort(port);
        newInstanceConfig.setDataCenterInfo(originalInstanceConfig.getDataCenterInfo());
        newInstanceConfig.setHealthCheckUrl(RegExUtils.replaceFirst(originalInstanceConfig.getHealthCheckUrl(), ":\\d+", ":" + port));
        newInstanceConfig.setSecureHealthCheckUrl(RegExUtils.replaceFirst(originalInstanceConfig.getSecureHealthCheckUrl(), ":\\d+", ":" + port));
        newInstanceConfig.setHomePageUrl(RegExUtils.replaceFirst(originalInstanceConfig.getHomePageUrl(), ":\\d+", ":" + port));
        newInstanceConfig.setStatusPageUrl(RegExUtils.replaceFirst(originalInstanceConfig.getStatusPageUrl(), ":\\d+", ":" + port));
        newInstanceConfig.setStatusPageUrlPath(originalInstanceConfig.getStatusPageUrlPath());
        newInstanceConfig.setIpAddress(originalInstanceConfig.getIpAddress());
        newInstanceConfig.setPreferIpAddress(originalInstanceConfig.isPreferIpAddress());

        ApplicationInfoManager manager =
                new ApplicationInfoManager(
                        newInstanceConfig,
                        duplicateInstanceInfo(applicationInfoManager.getInfo(), newInstanceConfig, port));

        return manager;
    }

    private InstanceInfo duplicateInstanceInfo(InstanceInfo info, EurekaInstanceConfigBean instanceConfig, int port)
    {
        //
        // Temporarily swap the instance ID; have to restore so that the bean
        // created in the auto configs keeps it original ID.
        //

        InstanceInfo.Builder builder = InstanceInfo.Builder.newBuilder();
        InstanceInfo newInfo = new InstanceInfoFactory().create(instanceConfig);

        newInfo.setStatus(InstanceInfo.InstanceStatus.UP);

        return newInfo;
    }


    private EurekaClientConfigBean duplicateConfig()
    {
        EurekaClientConfigBean newConfig = new EurekaClientConfigBean();
        newConfig.setFetchRegistry(false);
        newConfig.setEurekaServerPort(originalClientConfig.getEurekaServerPort());
        newConfig.setAllowRedirects(originalClientConfig.isAllowRedirects());
        newConfig.setAvailabilityZones(originalClientConfig.getAvailabilityZones());
        newConfig.setBackupRegistryImpl(originalClientConfig.getBackupRegistryImpl());
        newConfig.setServiceUrl(originalClientConfig.getServiceUrl());
        return newConfig;
    }
}
