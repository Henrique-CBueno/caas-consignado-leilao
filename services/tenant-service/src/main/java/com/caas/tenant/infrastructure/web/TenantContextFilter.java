package com.caas.tenant.infrastructure.web;

import com.caas.tenant.domain.TenantId;
import com.caas.tenant.infrastructure.TenantContextHolder;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

// Confia no header X-Tenant-Id porque, na arquitetura aprovada, o api-gateway é a
// única borda que valida o JWT e roteia para os serviços internos — o tenant-service
// nunca é exposto diretamente. A validação do JWT em si é o Seam 3 (api-gateway).
@Component
public class TenantContextFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {
        try {
            String header = request.getHeader("X-Tenant-Id");
            if (header != null) {
                TenantContextHolder.set(new TenantId(UUID.fromString(header)));
            }
            filterChain.doFilter(request, response);
        } finally {
            TenantContextHolder.clear();
        }
    }
}
