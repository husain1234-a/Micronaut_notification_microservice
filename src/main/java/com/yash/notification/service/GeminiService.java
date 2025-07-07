package com.yash.notification.service;

import reactor.core.publisher.Mono;

public interface GeminiService {
    Mono<String> generateMessage(String prompt);
}