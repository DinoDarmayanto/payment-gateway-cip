package com.cip.api.payment_gateway.Controller.Client;

import com.cip.api.payment_gateway.Model.Request.Client.CoreBankRequest;
import com.cip.api.payment_gateway.Model.Response.CoreBankResponse;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/corebank")
public class MockCoreBankController {

    @PostMapping("/debit")
    public CoreBankResponse debit(@RequestBody CoreBankRequest request) {
        if (request.getAmount().doubleValue() > 1_000_000) {
            return CoreBankResponse.builder()
                    .status("FAILED")
                    .message("Insufficient balance")
                    .build();
        }

        return CoreBankResponse.builder()
                .corebankReference("CB" + System.currentTimeMillis())
                .status("SUCCESS")
                .build();
    }
}