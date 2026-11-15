package com.vaultai.be.config;

import java.io.IOException;

import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import com.vaultai.be.account.DemoDataService;
import com.vaultai.be.auth.CurrentUser;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/** After login: creates the demo accounts if needed, then sends the user to the frontend. */
@Component
public class LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final DemoDataService demoDataService;
    private final AppProperties properties;

    public LoginSuccessHandler(DemoDataService demoDataService, AppProperties properties) {
        this.demoDataService = demoDataService;
        this.properties = properties;
        setDefaultTargetUrl(properties.frontendUrl() + "/");
        setAlwaysUseDefaultTargetUrl(true);
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        if (properties.demoData().enabled()) {
            demoDataService.ensureDemoData(CurrentUser.id(authentication));
        }
        super.onAuthenticationSuccess(request, response, authentication);
    }
}