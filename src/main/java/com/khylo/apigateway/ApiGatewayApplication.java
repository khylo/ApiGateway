package com.khylo.apigateway;

import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.client.discovery.ReactiveDiscoveryClient;
import org.springframework.cloud.gateway.discovery.DiscoveryClientRouteDefinitionLocator;
import org.springframework.cloud.gateway.discovery.DiscoveryLocatorProperties;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.server.ServerRequest;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

@SpringBootApplication
@EnableConfigurationProperties(ApiGatewayApplication.UriConfiguration.class)
@RestController
@EnableDiscoveryClient
public class ApiGatewayApplication {

    Logger log = LoggerFactory.getLogger(ApiGatewayApplication.class);


    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }

    /**
     * Proxies all consul services
     * @param dc
     * @param dlp
     * @return
     */
    @Bean
    DiscoveryClientRouteDefinitionLocator discoverRoutes(ReactiveDiscoveryClient dc, DiscoveryLocatorProperties dlp){
        return new DiscoveryClientRouteDefinitionLocator(dc,dlp);
    }

    @Bean
    RestTemplate getRestTemplate(){
        return new RestTemplate();
    }

    @Bean
    ApplicationRunner init(RestTemplate rt, DiscoveryClient dc, CustomerService cs){
        return f -> {
            List<ServiceInstance> services = dc.getInstances("customer-service");
            log.info("RT: Found customer-service instances "+services.get(0));
            if(services!=null && services.size()>0){
                String url = services.get(0).getUri()+"/customers";
                log.info("RT: About to call on "+url);
                ResponseEntity entity = rt.getForEntity(url, Object.class);
                log.info("RT: Entityt  "+entity);
                Object data = rt.getForObject(url, Object.class);
                log.info("RT: got  "+data);
                List list = rt.getForObject(url, List.class);
                log.info("RT: got list  "+list);
                Customer[] customers = rt.getForObject(url, Customer[].class);
                log.info("RT: got customers  "+customers);
                cs.setDataStore( CustomerData.toMap(rt.getForObject(url, Customer[].class)));
            }
        };
    }

    /**
     * Using WebClient
     * @param dc
     * @return
     */
    @Bean
    ApplicationRunner init(DiscoveryClient dc){
        return f -> {
            List<ServiceInstance> services = dc.getInstances("customer-service");
            log.info("WC: Found customer-service instances "+services.get(0));
            if(services!=null && services.size()>0){
                String url = services.get(0).getUri()+"/customers";
                log.info("About to call on "+url);
                Flux<Map> data = WebClient.create()
                     .get()
                     .uri(url)
                    .retrieve()
                    .bodyToFlux(Map.class);
                data.subscribe(d-> log.info("WC: got "+d));
                log.info("WC: "+data.blockFirst().toString());
            }
        };
    }

    @Bean
    ServiceLookupRoutePredicateFactory getServiceLookupRoutePredicateFactory(CustomerService cs){
        return new ServiceLookupRoutePredicateFactory(cs);
    }

    @Bean
    PredicateConfig getPredicateConfig(){
        return new PredicateConfig();
    }

    @Autowired
    CustomerService customerService;

    /**
     * Rewriting Josh Longs tutorial with updated fluent api
     * https://www.youtube.com/watch?v=TwVtlNX-2Hs
     *
     * @param builder
     * @param uriConfiguration
     * @return
     */
    @Bean
    public RouteLocator myRoutes(RouteLocatorBuilder builder,
                                 UriConfiguration uriConfiguration,
                                 ServiceLookupRoutePredicateFactory slrpf,
                                 PredicateConfig predicateConfig ) {
        String httpUri = uriConfiguration.getHttpbin();
        return builder.routes()
                .route(p -> p
                    .path("/get")
                    .filters(f -> f.addRequestHeader("Hello", "World"))
                    .uri(httpUri))
                //basic proxy
                .route(p -> p.path("/start**").filters(f ->f.rewritePath("/start/.*", "")).uri("http://start.spring.io/"))
                //github proxy 1 // will automatically append entire url.. so localhost:8081/gh/ghqp -> https://github.com/gh/ghqp  (github isues 302)
                //.route(p -> p.path("/gh").uri("http://github.com/"))
                //github proxy 2 http://localhost:8081/gh/ghqp -> https://github.com/gh/ghqp
                .route(p -> p.path("/gh/**").filters(f ->f.rewritePath("/gh/(?<gh>.*)", "/${gh}")).uri("http://github.com/"))
                // Github 3  http://localhost:8081/khylo goes straight to http://github.com/khylo with 302 to https
                .route(p -> p.path("/khylo").uri("http://github.com/"))
                // Lookup.. Using PredicateFactory.. Doesn't seem to be able to map urls
                //.route(p -> p.path("/look/**").uri("na://no-op").predicate(slrpf.apply(predicateConfig)))

                // Using custom filter /fil/B/* -> Google  /fil/B/* -> Amazon
                .route(p-> p.path("/fil/**").filters(f-> f.filter(new ServiceLookupFilter(customerService))).uri("no://op"))

                // Load Balanced proxy (using service discovery in this case consul)
                .route(p -> p.path("/lb/**").uri("lb://customer-service/customers"))
                //Add custom header
                .route(p ->p.path("/h").filters(f->f.addRequestHeader("ApiGateway", "CustomeHeader")).uri("lb://customer-service/customers"))
                //Custom filter 1. Set sytatus  and content-type
                .route(p ->p.path("/cf1").filters(f->f.setStatus(HttpStatus.BANDWIDTH_LIMIT_EXCEEDED).setResponseHeader("content-type",MediaType.IMAGE_GIF.toString())).uri("lb://customer-service/customers"))
                // Custom filter 2. Re-write url with customer Id taken form original
                .route(p-> p.path("/cf2").filters(f ->f.rewritePath("/cf2/(?<CID>).*", "/customers/${CID}")).uri("lb://customer-service"))
                // Custom filter 2. Re-write url with customer Id taken form original
                .route(p-> p.path("/cf3").filters(f ->f.stripPrefix(1)).uri("lb://customer-service/customers"))
                // Hystrix old.. resilience 4j recommended now.. uses hystrix.command.default.execution.isolation.thread.timeoutInMilliseconds=3000
                .route(p->p.path("/hy").filters(f->f.hystrix(config -> config.setName("myconfig"))).uri("http://github.com"))
                .route(p->p.path("/hy2").filters(f->f.hystrix(config -> config.setName("myconfig"))).uri("lb://customer-service/customers"))
                //Circuit Breaker (using resilieance 4j https://github.com/resilience4j/resilience4j)
                //.route(p-> p.path("/cb").filters(f -> f.circuitBreaker()
                .build();
    }

    // tag::uri-configuration[]
    @ConfigurationProperties
    class UriConfiguration {

        private String httpbin = "http://httpbin.org:80";

        public String getHttpbin() {
            return httpbin;
        }

        public void setHttpbin(String httpbin) {
            this.httpbin = httpbin;
        }

    }
}