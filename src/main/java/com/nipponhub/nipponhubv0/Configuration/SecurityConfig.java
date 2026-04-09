package com.nipponhub.nipponhubv0.Configuration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.nipponhub.nipponhubv0.Services.OurUserDetailsService;

/**
 * Access matrix
 * ─────────────────────────────────────────────────────────────────────────
 *  /auth/**                         PUBLIC  (register, login, refresh)
 *  /file/**                         PUBLIC  (image streaming + upload)
 *
 *  GET  /api/v0/product/**          PUBLIC  (catalogue browsing, search)
 *  GET  /api/v0/categories/**       PUBLIC
 *  GET  /api/v0/country/**          PUBLIC
 *
 *  POST/PUT/DELETE /api/v0/product/**      ADMIN | OWNER
 *  POST/PUT/DELETE /api/v0/categories/**   ADMIN | OWNER
 *  POST/DELETE     /api/v0/country/**      ADMIN | OWNER
 *  /api/v0/achat/**                        ADMIN | OWNER
 *  /api/v0/vente/**                        ADMIN | OWNER  (direct sales)
 *
 *  POST /api/v0/commande/new               Any authenticated user
 *  GET  /api/v0/commande/my-orders/**      Any authenticated user
 *  /api/v0/commande/**  (other)            ADMIN | OWNER
 *
 *  /api/admin/**                    ADMIN | OWNER
 *  /api/adminuser/**                ADMIN | OWNER
 *  PUT /api/user/update             Any authenticated user
 *  /api/user/**                     Any authenticated user
 *  Everything else                  Any authenticated user
 * ─────────────────────────────────────────────────────────────────────────
 *
 * Fine-grained @PreAuthorize annotations on individual methods provide an
 * extra layer of enforcement beyond this filter chain.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    @Autowired
    private OurUserDetailsService ourUserDetailsService;
    @Autowired
    private JWTAuthFilter jwtAuthFilter;


    /**
     * Security filter chain security filter chain.
     *
     * @param httpSecurity the http security
     * @return the security filter chain
     * @throws Exception the exception
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity) throws Exception{
        httpSecurity.addFilterBefore(new SimpleRateLimitFilter(), UsernamePasswordAuthenticationFilter.class);
        httpSecurity.csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .authorizeHttpRequests(request-> request.requestMatchers("/auth/**", 
                                                                        "/api/V0/**", 
                                                                        "/file/**", 
                                                                        "/swagger-ui/**",
                                                                        "/v3/**",
                                                                        "/nipponhub-test-console.html/**").permitAll()
// ── Public: auth & files ───────────────────────────────
                .requestMatchers("/auth/**").permitAll()
                .requestMatchers("/file/**").permitAll()

                // ── Public: catalogue reads ────────────────────────────
                .requestMatchers(HttpMethod.GET,  "/api/v0/product/**").permitAll()
                .requestMatchers(HttpMethod.GET,  "/api/v0/city/**").permitAll()
                .requestMatchers(HttpMethod.GET,  "/api/v0/product/**").permitAll()
                .requestMatchers(HttpMethod.GET,  "/api/v0/categories/**").permitAll()
                .requestMatchers(HttpMethod.GET,  "/api/v0/country/**").permitAll()

                // ── ADMIN / OWNER: product management ─────────────────
                .requestMatchers(HttpMethod.POST,   "/api/v0/product/**").hasAnyRole("ADMIN","OWNER")
                .requestMatchers(HttpMethod.PUT,    "/api/v0/product/**").hasAnyRole("ADMIN","OWNER")
                .requestMatchers(HttpMethod.DELETE, "/api/v0/product/**").hasAnyRole("ADMIN","OWNER")

                // ── ADMIN / OWNER: category management ────────────────
                .requestMatchers(HttpMethod.POST,   "/api/v0/categories/**").hasAnyRole("ADMIN","OWNER")
                .requestMatchers(HttpMethod.PUT,    "/api/v0/categories/**").hasAnyRole("ADMIN","OWNER")
                .requestMatchers(HttpMethod.DELETE, "/api/v0/categories/**").hasAnyRole("ADMIN","OWNER")

                // ── ADMIN / OWNER: country management ─────────────────
                .requestMatchers(HttpMethod.POST,   "/api/v0/country/**").hasAnyRole("ADMIN","OWNER")
                .requestMatchers(HttpMethod.POST,   "/api/v0/city/**").hasAnyRole("ADMIN","OWNER")
                .requestMatchers(HttpMethod.PUT,    "/api/v0/city/**").hasAnyRole("ADMIN","OWNER")
                .requestMatchers(HttpMethod.DELETE, "/api/v0/country/**").hasAnyRole("ADMIN","OWNER")
                .requestMatchers(HttpMethod.DELETE, "/api/v0/city/**").hasAnyRole("ADMIN","OWNER")

                // ── ADMIN / OWNER: achats & direct ventes ─────────────
                .requestMatchers("/api/v0/achat/**").hasAnyRole("ADMIN","OWNER")
                .requestMatchers("/api/v0/vente/**").hasAnyRole("ADMIN","OWNER")

                // ── Any authenticated: place order & own history ───────
                .requestMatchers(HttpMethod.POST, "/api/v0/commande/new").authenticated()
                .requestMatchers("/api/v0/commande/my-orders/**").authenticated()

                // ── ADMIN / OWNER: all other commande operations ───────
                .requestMatchers("/api/v0/commande/**").hasAnyRole("ADMIN","OWNER")

                // ── User management ────────────────────────────────────
                .requestMatchers("/api/admin/**").hasAnyRole("ADMIN","OWNER")
                .requestMatchers("/api/adminuser/**").hasAnyRole("ADMIN","OWNER","CLIENT")
                .requestMatchers(HttpMethod.PUT, "/api/user/update").authenticated()
                .requestMatchers("/api/user/**").authenticated()
                        .anyRequest().authenticated())

                .sessionManagement(manager->manager.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(
                        jwtAuthFilter, UsernamePasswordAuthenticationFilter.class
                );
        return httpSecurity.build();
    }

    /**
     * Authentication provider authentication provider.
     *
     * @return the authentication provider
     */
    @Bean
    public AuthenticationProvider authenticationProvider(){
        DaoAuthenticationProvider daoAuthenticationProvider = new DaoAuthenticationProvider(ourUserDetailsService);
        daoAuthenticationProvider.setPasswordEncoder(passwordEncoder());
        return daoAuthenticationProvider;
    }

    /**
     * Password encoder password encoder.
     *
     * @return the password encoder
     */
    @Bean
    public PasswordEncoder passwordEncoder(){
        return new BCryptPasswordEncoder();
    }

    /**
     * Authentication manager authentication manager.
     *
     * @param authenticationConfiguration the authentication configuration
     * @return the authentication manager
     * @throws Exception the exception
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception{
        return authenticationConfiguration.getAuthenticationManager();
    }

}

