package com.khylo.apigateway;

import lombok.extern.java.Log;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.RouteToRequestUrlFilter;
import org.springframework.cloud.gateway.handler.predicate.AbstractRoutePredicateFactory;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.http.HttpCookie;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
@Slf4j
public class ServiceLookupRoutePredicateFactory extends
        AbstractRoutePredicateFactory<PredicateConfig> {

    public static final String LookupUrl = "/look";
    CustomerService customerService;

    AntPathMatcher pathMatcher = new AntPathMatcher();
    String format = LookupUrl+"/{customer}/{otherParam}";

    public ServiceLookupRoutePredicateFactory(CustomerService cs){
        super(PredicateConfig.class);
        customerService=cs;
    }

    @Override
    public Predicate<ServerWebExchange> apply(PredicateConfig config) {
        return (ServerWebExchange exchange) -> {
            String path  = exchange.getRequest().getPath().value();
            if(pathMatcher.match(format, path)) {
                log.info("Path matches ServiceLookup "+path);
                Map<String, String> pathVariables = pathMatcher.extractUriTemplateVariables(format, path);
                int start = path.indexOf(LookupUrl);
                if (pathVariables != null) {
                    String customer = pathVariables.get("customer");
                    String param = pathVariables.get("otherParam");
                    if (customer != null) {
                        String id = customerService.reverse(customer);
                        log.info("!!! Found Mapping !!! for" + customer);
                        log.info("Need to rewrite now. id = " + id);
                        try {
                            exchange.getAttributes().put(ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR, new URI("http://www.google.com"));
                            exchange.getAttributes().put(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR, new URI("http://www.google.com/maps"));
                            log.info("Reset url = "+ RouteToRequestUrlFilter.ROUTE_TO_URL_FILTER_ORDER);
                            return true;
                        } catch (URISyntaxException e) {
                            log.error("Invalid target URI ",e);
                            return false;
                        }
                    }
                }
            }
            return false;
        };
    }
}
