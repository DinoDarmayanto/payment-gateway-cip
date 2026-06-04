package com.cip.api.payment_gateway.Config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(info = @Info(title = "Payment Gateway CIP API", version = "1.0.0", description = "Payment Gateway Service for processing payment transactions from multiple channels such as Mobile Banking, Internet Banking, and ATM. "
                + "The service validates requests, debits customer balance through Core Banking, forwards successful transactions to Biller Aggregator, "
                + "stores transaction history, and provides transaction status inquiry APIs.", contact = @Contact(name = "Dino Darmayanto", email = "dinodarmayanto22@gmail.com"), license = @License(name = "Payment Gateway CIP License")), servers = {
                                @Server(url = "http://localhost:8080", description = "Local Development")
                })
public class SwaggerConfig {
}
