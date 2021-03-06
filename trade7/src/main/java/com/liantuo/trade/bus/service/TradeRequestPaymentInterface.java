package com.liantuo.trade.bus.service;

import com.liantuo.payment.client.pay.PaymentRequest;
import com.liantuo.payment.client.pay.PaymentResponse;

public interface TradeRequestPaymentInterface {

	/**
	 * 同步请求支付中心
	 * @param payRequest
	 * @param errorCode  异常错误码
	 * @return
	 */
	public PaymentResponse synRequestPayment(PaymentRequest payRequest);
	
	/**
	 * xml序列化返回对象 用于更新请求支付中心日志
	 * @param obj
	 * @return
	 */
	public String getPaymentResult(PaymentResponse obj);
	
	/**
	 * 解析调用结果 获取状态    如果获取不到链接 则抛出异常
	 * @param obj
	 * @return
	 * @throws Exception
	 */
	public String parseObject(PaymentResponse obj) throws Exception;
}
