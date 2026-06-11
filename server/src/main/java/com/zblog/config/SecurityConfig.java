package com.zblog.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zblog.common.api.ApiResponse;
import com.zblog.security.JwtAuthenticationFilter;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.FormLoginConfigurer;
import org.springframework.security.config.annotation.web.configurers.HttpBasicConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
public class SecurityConfig {

  // API 采用无状态 JWT，除公开读接口和认证入口外默认都需要认证。
  @Bean
  SecurityFilterChain securityFilterChain(
      HttpSecurity http, JwtAuthenticationFilter jwtAuthenticationFilter, ObjectMapper objectMapper)
      throws Exception {
    return http.cors(cors -> {})
        .csrf(AbstractHttpConfigurer::disable)
        .httpBasic(HttpBasicConfigurer::disable)
        .formLogin(FormLoginConfigurer::disable)
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .exceptionHandling(
            exceptions ->
                exceptions.authenticationEntryPoint(
                    (request, response, authException) -> {
                      response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                      response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                      objectMapper.writeValue(
                          response.getOutputStream(),
                          ApiResponse.failure(401, "Unauthorized"));
                    }))
        .authorizeHttpRequests(
            requests ->
                requests
                    // 白名单覆盖公开内容、采集、订阅、上传和文档入口，其余 API 走 JWT。
                    .requestMatchers(
                        "/api/v1/health",
                        "/api/v1/auth/login",
                        "/api/v1/auth/register",
                        "/api/v1/auth/forgot-password",
                        "/api/v1/auth/reset-password",
                        "/api/v1/auth/github",
                        "/api/v1/auth/google",
                        "/api/v1/auth/qq",
                        "/api/v1/auth/microsoft",
                        "/api/v1/feedback/**",
                        "/api/v1/upload",
                        "/api/v1/collect",
                        "/api/v1/subscribe/**",
                        "/api/v1/front/**",
                        "/api/v1/menus",
                        "/api/v1/categories/**",
                        "/api/v1/tags/**",
                        "/api/v1/articles/**",
                        "/api/v1/stats/**",
                        "/api/v1/comments/**",
                        "/api/v1/friends/**",
                        "/api/v1/moments/**",
                        "/api/v1/albums/**",
                        "/api/v1/guestbook/**",
                        "/api/v1/settings/**",
                        "/rss.xml",
                        "/atom.xml",
                        "/sitemap.xml",
                        "/robots.txt",
                        "/uploads/**",
                        "/actuator/health",
                        "/v3/api-docs/**",
                        "/swagger-ui/**",
                        "/swagger-ui.html")
                    .permitAll()
                    .anyRequest()
                    .authenticated())
        .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
        .build();
  }

  @Bean
  UserDetailsService userDetailsService() {
    return username -> {
      throw new UsernameNotFoundException(username);
    };
  }

  @Bean
  PasswordEncoder passwordEncoder() {
    return PasswordEncoderFactories.createDelegatingPasswordEncoder();
  }

  @Bean
  CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();
    configuration.setAllowedOriginPatterns(
        List.of("http://localhost:*", "http://127.0.0.1:*", "http://host.docker.internal:*"));
    configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
    configuration.setAllowedHeaders(List.of("*"));
    configuration.setAllowCredentials(true);
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
  }
}
