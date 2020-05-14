package com.khylo.apigateway;

import lombok.extern.java.Log;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.stereotype.Service;
import lombok.Data;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;

import javax.naming.ServiceUnavailableException;
import java.net.URI;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;

@Data
@Service
@Slf4j
public class CustomerService {
    public final static String LookupUrl = "/look";

    AntPathMatcher pathMatcher = new AntPathMatcher();
    String format = LookupUrl+"/{customer}/{otherParam}";

    private Map<String, String> dataStore;

    public String lookup(String id){
        log.info("Looking up "+id+" from "+dataStore);
        return dataStore.get(id);
    }

    /**
     * Only for demo. If used in reality shoudl use something like Guava BiMap https://guava.dev/releases/snapshot/api/docs/com/google/common/collect/BiMap.html
     * @param value
     * @return
     */
    public String reverse(String value){
        for (Map.Entry<String, String> entry : dataStore.entrySet()) {
            if (Objects.equals(value, entry.getValue())) {
                return entry.getKey();
            }
        }
        return null;
    }

    public Optional<String> convertPath(String path){
        if(pathMatcher.match(format, path)) {
            log.info("Path matches ServiceLookup "+path);
            Map<String, String> pathVariables = pathMatcher.extractUriTemplateVariables(format, path);
            int start = path.indexOf(LookupUrl);
            if (pathVariables != null) {
                String customer = pathVariables.get("customer");
                String param = pathVariables.get("otherParam");
                if (customer != null) {
                    String id = reverse(customer);
                    log.info("!!! Found Mapping !!! for" + customer);
                    log.info("Need to rewrite now. id = " + id);
                    return Optional.of(id);
                }
            }
        }
        return Optional.empty();
    }
}
