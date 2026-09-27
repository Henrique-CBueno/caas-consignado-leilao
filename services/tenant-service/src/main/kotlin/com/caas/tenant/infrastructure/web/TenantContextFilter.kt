package com.caas.tenant.infrastructure.web

import com.caas.tenant.domain.TenantId
import com.caas.tenant.infrastructure.TenantContextHolder
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.util.UUID

// Confia no header X-Tenant-Id porque, na arquitetura aprovada, o api-gateway é a
// única borda que valida o JWT e roteia para os serviços internos — o tenant-service
// nunca é exposto diretamente. A validação do JWT em si é o Seam 3 (api-gateway).
@Component
class TenantContextFilter : OncePerRequestFilter() {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        try {
            request.getHeader("X-Tenant-Id")?.let {
                TenantContextHolder.set(TenantId(UUID.fromString(it)))
            }
            filterChain.doFilter(request, response)
        } finally {
            TenantContextHolder.clear()
        }
    }
}
