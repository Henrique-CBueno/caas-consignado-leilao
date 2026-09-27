package com.caas.disbursement.infrastructure.web;

import com.caas.disbursement.domain.TenantId;
import com.caas.disbursement.infrastructure.TenantContextHolder;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

// Ver TenantContextFilter do proposal-service: confia no header X-Tenant-Id porque
// o api-gateway é a única borda que valida o JWT antes de rotear internamente.
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
