package com.liantuo.mc.web.client;

import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.*;

import org.powermock.api.mockito.PowerMockito;

import com.liantuo.mc.web.req.MerchantOutRequest;
import com.liantuo.mc.web.rsp.MerchantResponse;
import com.liantuo.unittest.mockito.IsTClass;

public class MerchantClientMock {

	public static void mockGetMerchant(MerchantResponse response) {
		PowerMockito.mockStatic(MerchantClient.class);
		when(MerchantClient.getMerchant(argThat(new IsTClass<MerchantOutRequest>(new MerchantOutRequest())))).thenReturn(response);
	}
}
