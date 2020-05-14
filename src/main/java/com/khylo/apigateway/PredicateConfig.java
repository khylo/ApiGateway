package com.khylo.apigateway;

import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;

@Data
public class PredicateConfig {
    @Autowired
    private CustomerService customerService;

    public String lookup(String name){
        return customerService.lookup(name);
    }
}
