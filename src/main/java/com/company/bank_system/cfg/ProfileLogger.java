package com.company.bank_system.cfg;


import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Slf4j
@Component
public class ProfileLogger {

    public ProfileLogger(Environment env) {
        log.info("Active profiles: {}", Arrays.toString(env.getActiveProfiles()));
    }

}
