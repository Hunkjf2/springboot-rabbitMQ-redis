package com.example.log.service.log;

import com.example.log.model.Log;
import com.example.log.repository.LogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LogService {
    
    private final LogRepository logRepository;

    public void cadastrarLog(Log log) {
        logRepository.save(log);
    }
    
}