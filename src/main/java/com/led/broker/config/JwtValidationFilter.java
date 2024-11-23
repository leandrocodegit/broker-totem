package com.led.broker.config;

import com.led.broker.service.AuthService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;


public class JwtValidationFilter extends OncePerRequestFilter {

   // private final AuthService authService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String token = null;
        try {

            String requestURI = request.getRequestURI();
            String httpMethod = request.getMethod();
            boolean isWebSocket = false;


            Map<String, String[]> queryParams = request.getParameterMap();
            for (Map.Entry<String, String[]> entry : queryParams.entrySet()) {
                String[] paramValues = entry.getValue();
                if (!String.join(", ", paramValues).isEmpty()) {
                    token = String.join(", ", paramValues);
                    break;
                }
            }
            if (token == null) {
                throw new RuntimeException("Token inválido.");
            }
            if (httpMethod.equals("GET") && requestURI.equals("/ws")) {
                isWebSocket = true;
            }
            Boolean isValid = validateTokenWithExternalService(token, isWebSocket);

            if (Boolean.FALSE.equals(isValid)) {
                throw new RuntimeException("Token inválido.");
            }
        } catch (Exception ex) {
            // Token inválido ou erro na validação
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("Token inválido ou erro na validação.");
            return;
        }

        // Prossegue com a cadeia de filtros
        filterChain.doFilter(request, response);
    }

    private String recoveryToken(HttpServletRequest request) {

        String authorizationHeader = request.getHeader("Authorization");
        if (authorizationHeader != null) {
            return authorizationHeader.replace("Bearer ", "");
        }
        return null;
    }

    private Boolean validateTokenWithExternalService(String token, boolean isWebSocket) {
        try {

            if (isWebSocket) {
                System.out.println("Validando token socker");
                System.err.println(token);
               // authService.validarwebSocker(token);
            } else {
                System.out.println("Validando token access");
                System.err.println(token);
              //  authService.validarToken(token);
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
