package com.cip.api.payment_gateway.Client;

import com.cip.api.payment_gateway.Model.Request.Client.BillerRequest;
import com.cip.api.payment_gateway.Model.Response.BillerResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "billerClient", url = "${integration.biller.url}")
public interface BillerClient {

    @PostMapping("/api/biller/pay")
    BillerResponse pay(@RequestBody BillerRequest request);
}
