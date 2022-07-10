package com.example.mockserver.entity;

import java.util.List;

public record Overview(boolean started, Integer port, List<String> paths) {
}
