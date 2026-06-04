package com.cip.api.payment_gateway.Controller.Client;

import com.cip.api.payment_gateway.Model.Request.Client.BillerRequest;
import com.cip.api.payment_gateway.Model.Response.BillerResponse;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/biller")
public class MockBillerController {

    @PostMapping("/pay")
    public BillerResponse pay(@RequestBody BillerRequest request) {
        return BillerResponse.builder()
                .billerReference("BILLER" + System.currentTimeMillis())
                .status("SUCCESS")
                .build();
    }
}