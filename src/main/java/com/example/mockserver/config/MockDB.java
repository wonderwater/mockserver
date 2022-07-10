package com.example.mockserver.config;

import org.mockserver.integration.ClientAndServer;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MockDB {

    public final static Map<Integer, ClientAndServer> mocks = new ConcurrentHashMap<>();

}
