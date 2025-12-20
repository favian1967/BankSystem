package com.company.bank_system.security;

import com.company.bank_system.service.JWTService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;


//catch all the requests and check jwt


@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter { //OncePerRequestFilter every requests

    private final JWTService jwtService;

    public JwtAuthenticationFilter(JWTService jwtService) {
        this.jwtService = jwtService;
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

            // 5. Говорим Spring Security: "Этот юзер авторизован" ya xz che za trash
            UsernamePasswordAuthenticationToken authToken =
                    new UsernamePasswordAuthenticationToken(email, null, null);

            authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authToken); //SecurityContextHolder глобальное хранилище
        }
        //end trash

        // 6. Передаём запрос дальше
        filterChain.doFilter(request, response);
    }
}