package com.tplu.config;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FilterConfig {

    @Bean
    public FilterRegistrationBean<SimpleCorsFilter> corsFilter() {
        FilterRegistrationBean<SimpleCorsFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(new SimpleCorsFilter());
        registrationBean.addUrlPatterns("/*");
        return registrationBean;
    }
}
