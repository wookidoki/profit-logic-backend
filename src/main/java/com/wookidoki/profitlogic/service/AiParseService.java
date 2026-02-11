package com.wookidoki.profitlogic.service;

import com.wookidoki.profitlogic.dto.CalculateRequest;

public interface AiParseService {

    CalculateRequest parse(String text);
}
