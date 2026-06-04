package com.cip.api.payment_gateway.Model.Response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CoreBankResponse {

    private String corebankReference;

    private String status;

    private String message;
}