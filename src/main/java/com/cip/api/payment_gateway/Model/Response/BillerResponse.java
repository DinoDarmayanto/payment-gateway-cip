package com.cip.api.payment_gateway.Model.Response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BillerResponse {

    private String billerReference;

    private String status;

    private String message;
}