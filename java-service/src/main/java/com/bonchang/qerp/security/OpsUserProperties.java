package com.bonchang.qerp.security;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "qerp.ops-auth")
@Getter
@Setter
public class OpsUserProperties {

    private List<OpsUser> users = new ArrayList<>();

    @Getter
    @Setter
    public static class OpsUser {
        private String username;
        private String password;
        private String role;
    }
}
