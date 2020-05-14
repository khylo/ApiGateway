package com.khylo.apigateway;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CustomerData {
    List<Customer> customers;

    public Map<String,String> toMap(){
        return toMap(customers);
    }
    public static Map<String,String> toMap(List<Customer> customers){
        return customers.stream().collect(Collectors.toMap(Customer::getId, Customer::getName));
    }
    public static Map<String,String> toMap(Customer[] customers){
        return Arrays.stream(customers).collect(Collectors.toMap(Customer::getId, Customer::getName));
    }
}

@Data
@NoArgsConstructor
@AllArgsConstructor
class Customer{
    String id;
    String name;
}
