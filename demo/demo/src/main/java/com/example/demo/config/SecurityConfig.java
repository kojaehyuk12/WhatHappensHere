package com.example.demo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf().disable() // CSRF 보호 비활성화 (개발용으로만 사용)
                .authorizeHttpRequests()
                .requestMatchers("/", "/index.html", "/static/**", "/css/**", "/js/**", "/images/**").permitAll() // 인증 없이 접근 허용
                .anyRequest().permitAll() // 모든 요청에 대해 접근 허용 (일시적으로 모든 권한 허용)
                .and()
                .formLogin().disable(); // 기본 로그인 폼 비활성화

        return http.build();
    }
}
