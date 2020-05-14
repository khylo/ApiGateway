package com.khylo.apigateway;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.pattern.PathPattern;

import java.net.URI;
import java.util.Map;

/**
 * See information here
 * https://github.com/spring-cloud/spring-cloud-gateway/issues/276
 */
@Component
public class DBLookupFilter extends AbstractGatewayFilterFactory {
    @Override
    public GatewayFilter apply(Object config) {
        return (exchange, chain) -> {
            Route route = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR);
            PathPattern.PathMatchInfo variables = exchange.getAttribute(ServerWebExchangeUtils.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
            if ((variables == null) || (route == null)) {
                return chain.filter(exchange);
            }
            Map<String, String> uriVariables = variables.getUriVariables();
            URI uri = route.getUri();
            String host = uri.getHost();
            if ((host != null) && uriVariables.containsKey(host)) {
                host = uriVariables.get(host);
            }
            if (host == null) {
                return chain.filter(exchange);
            }
            URI newUri = UriComponentsBuilder.fromUri(uri).host(host).build().toUri();
            Route newRoute = Route.builder()
                    .id(route.getId())
                    .uri(newUri)
                    .order(route.getOrder())
                    //.predicate(route.getPredicate())
                    .filters(route.getFilters())
                    .build();
            exchange.getAttributes().put(ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR, newRoute);
            return chain.filter(exchange);
        };
        //.route(p -> p.path("/look/**").uri("http://github.com/").predicate(slrpf.apply(predicateConfig)))
    }
}
