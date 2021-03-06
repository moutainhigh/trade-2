package com.liantuo.trade.bus.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import com.liantuo.payment.client.pay.PaymentClientV3;
import com.liantuo.payment.client.pay.PaymentRequest;
import com.liantuo.payment.client.pay.PaymentResponse;
import com.liantuo.payment.client.pay.alipay.vo1.req.AlipayTradePayRequest;
import com.liantuo.payment.client.pay.alipay.vo1.req.AlipayTradePrecreateRequest;
import com.liantuo.payment.client.pay.alipay.vo1.req.AlipayTradeQueryRequest;
import com.liantuo.payment.client.pay.alipay.vo1.rsp.AlipayTradePayResponse;
import com.liantuo.payment.client.pay.alipay.vo1.rsp.AlipayTradePrecreateResponse;
import com.liantuo.payment.client.pay.alipay.vo1.rsp.AlipayTradeQueryResponse;
import com.liantuo.payment.client.pay.weixin.vo1.agent.req.WeiXinQueryRequest;
import com.liantuo.payment.client.pay.weixin.vo1.agent.req.WeiXinScanPayRequest;
import com.liantuo.payment.client.pay.weixin.vo1.agent.req.WeiXinUnifiedOrderRequest;
import com.liantuo.payment.client.pay.weixin.vo1.agent.rsp.WeiXinQueryResponse;
import com.liantuo.payment.client.pay.weixin.vo1.agent.rsp.WeiXinScanPayResponse;
import com.liantuo.payment.client.pay.weixin.vo1.agent.rsp.WeiXinUnifiedOrderResponse;
import com.liantuo.trade.bus.service.ExceptionService;
import com.liantuo.trade.bus.service.TradeRequestPaymentInterface;
import com.liantuo.trade.common.constants.TradeConstants;
import com.liantuo.trade.common.util.xml.ObjectXmlUtil;

/**
 * 请求支付中心service
 * @author yangting
 *2016年7月5日 下午2:09:24
 */
@Service
public class TradeRequestPaymentImpl implements TradeRequestPaymentInterface {
	 private static Logger logger = LoggerFactory.getLogger(TradeRequestPaymentImpl.class);
	 
     @Autowired
     private ExceptionService exceptionService;
	/**
	 *  支付宝当面付2.0条码|声波支付
	 */
	@Override
	public PaymentResponse synRequestPayment(PaymentRequest payRequest){
		String pay_channel = payRequest.getZf_head().getPay_channel();
		Assert.notNull(pay_channel, "pay_channel is required!");
		if(TradeConstants.ZF0001_02_001.equals(pay_channel)){
			AlipayTradePayRequest request = (AlipayTradePayRequest) payRequest;
			AlipayTradePayResponse response = null;
			try {
				response = PaymentClientV3.alipayTradePay(request, AlipayTradePayResponse.class);
			} catch (Exception e) {
				logger.info("请求支付中心超时！");
				e.printStackTrace();
//				throw exceptionService.buildBusinessException(errorCode);
			}
			return response;
		}else if(TradeConstants.ZF0003_01_001.equals(pay_channel)){
			WeiXinScanPayRequest request=(WeiXinScanPayRequest) payRequest;
			WeiXinScanPayResponse response = null;
			try {
				response = PaymentClientV3.weixinScanPay(request, WeiXinScanPayResponse.class);
			} catch (Exception e) {
				logger.info("请求支付中心超时！");
				e.printStackTrace();
//				throw exceptionService.buildBusinessException(errorCode);
			}
			return response;
		}else if(TradeConstants.ALI_ASYN_PAY_CHANNEL.equals(pay_channel)){
			AlipayTradePrecreateRequest request=(AlipayTradePrecreateRequest) payRequest;
			AlipayTradePrecreateResponse response = null ;
			try {
				response = PaymentClientV3.alipayTradePrecreate(request, AlipayTradePrecreateResponse.class);
			} catch (Exception e) {
				logger.info("请求支付中心【ZF0001_02_002】系统异常....");
				e.printStackTrace();
			}
			return response;
		}else if(TradeConstants.WX_ASYN_PAY_CHANNEL.equals(pay_channel)){
			WeiXinUnifiedOrderRequest request=(WeiXinUnifiedOrderRequest) payRequest;
			WeiXinUnifiedOrderResponse response = null ;
			try {
				response = PaymentClientV3.weixinUnifiedOrder(request, WeiXinUnifiedOrderResponse.class);
			} catch (Exception e) {
				logger.info("请求支付中心【ZF0003_01_002】系统异常....");
				e.printStackTrace();
			}
			return response;
		}else if(TradeConstants.ZF0001_02_003.equals(pay_channel)){
			AlipayTradeQueryRequest request=(AlipayTradeQueryRequest) payRequest;
			AlipayTradeQueryResponse response = null ;
			try {
				response = PaymentClientV3.alipayTradeQuery(request, AlipayTradeQueryResponse.class);
			} catch (Exception e) {
				logger.info("请求支付中心【ZF0001_02_003】系统异常....");
				e.printStackTrace();
			}
			return response;
		}else if(TradeConstants.ZF0003_01_003.equals(pay_channel)){
			WeiXinQueryRequest request=(WeiXinQueryRequest) payRequest;
			WeiXinQueryResponse response = null ;
			try {
				 response=PaymentClientV3.weixinQuery(request, WeiXinQueryResponse.class);
			} catch (Exception e) {
				logger.info("请求支付中心【ZF0003_01_003】系统异常....");
				e.printStackTrace();
			}
			return response;
		}
		return null;
	}

	@Override
	public String getPaymentResult(PaymentResponse obj) {
		return ObjectXmlUtil.marshal(obj);
	}

	@Override
	public String parseObject(PaymentResponse response) throws Exception {
		String pay_channel = response.getZf_head().getPay_channel();
		Assert.notNull(pay_channel, "pay_channel is required!");
		String ret_code = response.getZf_head().getRet_code();
        return ret_code;
	}

}
