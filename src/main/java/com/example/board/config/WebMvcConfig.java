package com.example.board.config;

import java.util.Locale;

import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;
import org.springframework.web.servlet.i18n.SessionLocaleResolver;

import com.example.board.common.filter.LoginInterceptor;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer{

	@Bean(value="localeResolver")
	LocaleResolver localeResolver() {
		SessionLocaleResolver slr = new SessionLocaleResolver();
		slr.setDefaultLocale(Locale.KOREAN);
		return slr;
	}
	
	@Bean(value="messageSource")
	MessageSource messageSource() {
		ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
		messageSource.setBasenames("i18n/message");
		messageSource.setDefaultEncoding("UTF-8");
		return messageSource;
	}
	
	@Bean(value="localeChangeInterceptor")
	LocaleChangeInterceptor localeChangeInterceptor() {
		LocaleChangeInterceptor lci = new LocaleChangeInterceptor();
		lci.setParamName("lang");
		return lci;
	}
	
	@Bean
    LoginInterceptor loginInterceptor() {
        return new LoginInterceptor();
    }
	
	@Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(localeChangeInterceptor());
        registry.addInterceptor(loginInterceptor())
                .addPathPatterns("/file/**")
                .addPathPatterns("/board/write/**")
                .addPathPatterns("/board/update/**")
                .addPathPatterns("/board/reply/**")
                .addPathPatterns("/board/delete/**");
    }
}
