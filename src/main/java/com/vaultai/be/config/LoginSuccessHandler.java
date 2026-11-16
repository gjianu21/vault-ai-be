package com.vaultai.be.config;

import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

/**
 * After login: sends the user to the frontend.
 * The demo data hook (WealthDemoDataService) is added here in plan step BE-5.
 */
@Component
public class LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    public LoginSuccessHandler(AppProperties properties) {
        setDefaultTargetUrl(properties.frontendUrl() + "/");
        setAlwaysUseDefaultTargetUrl(true);
    }
}
