package com.example.shop_management.config;

import com.example.shop_management.service.CustomSuccessHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final UserDetailsService userDetailsService;

    public SecurityConfig(UserDetailsService userDetailsService) {
        this.userDetailsService = userDetailsService;
    }

    @Autowired
    private CustomSuccessHandler customSuccessHandler;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // Tích hợp CustomUserDetailsService + PasswordEncoder
    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // Cho phép truy cập public vào 1 số URL
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/auth/register", "/auth/login", "/css/**", "/js/**", "/fonts/**", "/images/**", "/libs/**").permitAll()
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .requestMatchers("/user/**").hasAnyRole("USER")
                        .anyRequest().authenticated()
                )

                // Form login
                .formLogin(form -> form
                        .loginPage("/auth/login") // Nếu có custom login page (phải có controller trả về view login.html)
                        .loginProcessingUrl("/perform_login") // Spring Security sẽ xử lý POST /perform_login
                        .usernameParameter("username") // match với input name="username" trong form
                        .passwordParameter("password") // match với input name="password" trong form
                        .successHandler(customSuccessHandler)// sau login thành công
                        .permitAll()
                )
                // Logout
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout")
                        .permitAll()
                )
                // Nếu test API bằng Postman, tắt CSRF
                .csrf(csrf -> csrf.disable());

        // Gắn DaoAuthenticationProvider
        http.authenticationProvider(authenticationProvider());

        return http.build();
    }
}
