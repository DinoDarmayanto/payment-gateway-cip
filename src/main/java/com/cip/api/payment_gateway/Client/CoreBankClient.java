package com.cip.api.payment_gateway.Client;

import com.cip.api.payment_gateway.Model.Request.Client.CoreBankRequest;
import com.cip.api.payment_gateway.Model.Response.CoreBankResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "coreBankClient", url = "${integration.corebank.url}")
public interface CoreBankClient {

    @PostMapping("/api/corebank/debit")
    CoreBankResponse debit(@RequestBody CoreBankRequest request);
}
