# ApiGateway

Simple implementation of the SpringCloud Gateway to perform some simple route examples

Based off tutorial from an older version by josh Long here. https://www.youtube.com/watch?v=TwVtlNX-2Hs
It also has some extra examples such as a custom filter for routing to doifferent urls based on some input (in this case it matches the url against results from a service)

## PreReqs
Must have consul, and vault running.. I use simple script to start these up in dev mode
```
consul agent -dev
vault server --dev --dev-root-token-id=
```

It relies on a Customer service running and registered in consul. See 


## Todo
* Circuit breaker
* Integrate with consul tags to get specific versions of service.
 * BackPressure
 * Integrate with Vault
 * Vault logins.
 