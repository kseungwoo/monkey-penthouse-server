package com.monkeypenthouse.core.config;

import com.monkeypenthouse.core.security.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.net.Authenticator;

@EnableWebSecurity
@RequiredArgsConstructor
@Configuration
@Log4j2
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    private final TokenProvider tokenProvider;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final JwtAccessDeniedHandler jwtAccessDeniedHandler;

    private static final String[] EXCLUDE_SWAGGER_REQUEST_PATH = {
            "/v2/api-docs/**", "/swagger*/**", "/webjars/**",
            "/swagger-resources/configuration/ui", "/swagger-resources", "/swagger-resources/configuration/security",
            "/swagger-ui/",
    };

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        JwtFilter customFilter = new JwtFilter(tokenProvider);

        // csrf ?????????
        http.httpBasic().disable()
                .cors().configurationSource(corsConfigurationSource())
                .and()
                .csrf().disable()

                // exception handling??? ??? ????????? ????????? ??????
                .exceptionHandling()
                .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                .accessDeniedHandler(jwtAccessDeniedHandler)

                // ???????????? ?????? ????????? Stateless??? ??????
                .and()
                .sessionManagement()
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)

                // ?????????, ???????????? API??? ????????? ?????? ???????????? ????????? ???????????? ????????? permitAll ??????
                .and()
                .authorizeRequests()
                .antMatchers(EXCLUDE_SWAGGER_REQUEST_PATH).permitAll()
                .antMatchers("/").permitAll()
                .antMatchers("/user/all/**").permitAll()
                .antMatchers("/amenity/**").permitAll()
                .antMatchers("/photo/**").permitAll()
                .antMatchers("/user/guest/**").hasAnyAuthority("GUEST")
//                .antMatchers(HttpMethod.POST,"/photo/carousel").hasAnyAuthority("ADMIN")
//                .antMatchers(HttpMethod.POST, "/amenity/").hasAnyAuthority("ADMIN")
                .anyRequest().authenticated()

                // JwtFilter??? addFilterBefore??? ???????????? JwtSecurityConfig ???????????? ??????
                .and()
                .addFilterBefore(customFilter, UsernamePasswordAuthenticationFilter.class);

        http.formLogin();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        configuration.addAllowedOrigin("localhost:8080");
        configuration.addAllowedHeader("*");
        configuration.addAllowedMethod("*");
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

}
