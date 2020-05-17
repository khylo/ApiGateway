package com.khylo.apigateway;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.RouteToRequestUrlFilter;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.Ordered;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.Optional;
@Slf4j
public class ServiceLookupFilter implements GatewayFilter, Ordered {

    @Override
    public int getOrder() {
        return RouteToRequestUrlFilter.ROUTE_TO_URL_FILTER_ORDER + 1;
    }

    CustomerService customerService;

    ServiceLookupFilter(CustomerService cs){
        customerService = cs;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path  = exchange.getRequest().getPath().value();
        Optional<String> newPath = customerService.convertPath(path);
        newPath.ifPresent(target -> {
            log.info("filtering "+path+". New Path = "+newPath.get());
            try {
                if(path.contains("A")){
                    exchange.getAttributes().put(ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR, new URI("http://www.google.com"));
                    //exchange.getAttributes().put(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR, new URI("http://www.google.com/maps"));
                }else{
                    exchange.getAttributes().put(ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR, new URI("http://www.amazon.com"));
                }
            } catch (URISyntaxException e) {
                log.error("Invalid target URI ", e);
            }
        });
        return chain.filter(exchange);
    }

}
