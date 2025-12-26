package com.company.bank_system.security;

import com.company.bank_system.entity.User;
import com.company.bank_system.repo.UserRepository;
import com.company.bank_system.service.JWTService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;


//catch all the requests and check jwt


@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter { //OncePerRequestFilter every requests

    private final JWTService jwtService;
    private final UserRepository userRepository;

    public JwtAuthenticationFilter(JWTService jwtService, UserRepository userRepository) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // 1. get jwt from http
        String authHeader = request.getHeader("Authorization");

        // 2. Если заголовка нет или он не начинается с "Bearer " → пропускаем
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // 3. Вырезаем токен (убираем "Bearer ")
        String token = authHeader.substring(7);

        // 4. Проверяем токен
        if (jwtService.isValid(token)) {
            String email = jwtService.extractEmail(token);
            User user = userRepository.findByEmail(email)
                    .orElse(null);

            List<GrantedAuthority> authorities =
                    List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));

            // 5. Говорим Spring Security: "Этот юзер авторизован"
            UsernamePasswordAuthenticationToken authToken =
                    new UsernamePasswordAuthenticationToken(
                            email,
                            null,
                            authorities
                    );

            authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authToken); //SecurityContextHolder global
        }



        filterChain.doFilter(request, response);
    }

}