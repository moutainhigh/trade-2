package com.liantuo.trade.common.util;

import static org.mockito.Mockito.when;

import org.powermock.api.mockito.PowerMockito;

import com.liantuo.trade.common.util.amount.AmountUtils;
import com.liantuo.trade.exception.AmountConvertException;

public class AmountUtilsMock {
	public static void mockLongConvertToBizAmountException1(Long amount) {
		PowerMockito.mockStatic(AmountUtils.class);
		when(AmountUtils.longConvertToBizAmount(amount)).thenThrow(new NumberFormatException());
	}
	
	public static void mockLongConvertToBizAmountException2(Long amount) {
		PowerMockito.mockStatic(AmountUtils.class);
		when(AmountUtils.longConvertToBizAmount(amount)).thenThrow(new AmountConvertException());
	}
	
	public static void mockBizAmountConvertToLongAmountConverException(String bizAmount){
		PowerMockito.mockStatic(AmountUtils.class);
		when(AmountUtils.bizAmountConvertToLong(bizAmount)).thenThrow(new AmountConvertException());
	}
	
	public static void mockBizAmountConvertToLongNumberFormatException(String bizAmount){
		PowerMockito.mockStatic(AmountUtils.class);
		when(AmountUtils.bizAmountConvertToLong(bizAmount)).thenThrow(new NumberFormatException());
	}
}
