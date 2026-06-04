package com.cip.api.payment_gateway.Service;

import com.cip.api.payment_gateway.Model.Request.Client.BillerRequest;
import com.cip.api.payment_gateway.Model.Response.BillerResponse;

public interface BillerGatewayService {

    BillerResponse pay(BillerRequest request);
}
