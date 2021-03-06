package com.liantuo.trade.bus.process.impl.single_payment.asyn_service.impl;

import java.util.Date;

import javax.annotation.Resource;

import org.dozer.DozerBeanMapper;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.liantuo.payment.client.pay.PaymentNotify;
import com.liantuo.payment.client.pay.weixin.vo1.agent.rsp.WeiXinUnifiedOrderNotifyResponse;
import com.liantuo.trade.bus.process.TradeState;
import com.liantuo.trade.bus.process.impl.single_payment.asyn_service.AsynNotifyService;
import com.liantuo.trade.client.trade.packet.body.TradeNotifyRequestBody;
import com.liantuo.trade.client.trade.packet.body.TradeRequestBody;
import com.liantuo.trade.client.trade.packet.body.single_payment.WeiXinUnifiedOrderNotifyPacket;
import com.liantuo.trade.common.util.amount.AmountUtils;
import com.liantuo.trade.common.util.date.DateUtil;
import com.liantuo.trade.orm.pojo.TradeLedger;

@Component(value = "weiXinUnifiedOrderNotifyService")
@Scope(value="prototype")
public class WeiXinUnifiedOrderNotifyService implements AsynNotifyService {
	@Resource(name = "beanMapper")
	private DozerBeanMapper mapper;

	@Override
	public TradeNotifyRequestBody parsePaymentNotify(PaymentNotify notify) throws Exception {
		TradeNotifyRequestBody requestBody = new TradeNotifyRequestBody();
		WeiXinUnifiedOrderNotifyResponse response = (WeiXinUnifiedOrderNotifyResponse) notify;
		WeiXinUnifiedOrderNotifyPacket body = mapper.map(response, WeiXinUnifiedOrderNotifyPacket.class);
		requestBody.setBody(body);
		return requestBody;
	}

	@Override
	public String getThirdOutTradeNo(TradeRequestBody body) throws Exception {
		WeiXinUnifiedOrderNotifyPacket wxBody = (WeiXinUnifiedOrderNotifyPacket) body;
		String merchOrderNo = wxBody.getOut_trade_no();
		if (merchOrderNo == null) {
			throw new Exception("微信返回的【商户订单号】为空");
		}
		return merchOrderNo;
	}

	@Override
	public long getThirdReceiveAmount(TradeRequestBody body) throws Exception {
		WeiXinUnifiedOrderNotifyPacket wxBody = (WeiXinUnifiedOrderNotifyPacket) body;
		// 微信返回的是分，需要转换为元，在乘以1000000
		String total_fee = AmountUtils.bizFen2Yuan(wxBody.getTotal_fee());
		return AmountUtils.str2long(total_fee);
	}

	@Override
	public TradeState getTradePaymentStatus(TradeNotifyRequestBody tradeRquest) throws Exception {
		// 该交易只有成功异步通知，支付中心已校验,如果以后有其他状态需要有其他的判断
		return TradeState.SUCCESS;
	}

	@Override
	public void prepareEffectLedger(TradeRequestBody body, TradeLedger ledger) {
		WeiXinUnifiedOrderNotifyPacket wxBody = (WeiXinUnifiedOrderNotifyPacket) body;
		//收付款备注2【付款方账户】	来自微信异步返回【用户子标识】
		ledger.setExtendField2(wxBody.getSub_openid());
		//收付款备注3【付款方账户名称】	暂空
		ledger.setExtendField3(null);
		//收付款备注6【收款账户】	暂空
		ledger.setExtendField6(null);
		//收付款备注7【收款账户名称】	暂空
		ledger.setExtendField7(null);
		//外部渠道收付款成功日期时间	来自微信异步返回【支付完成时间】
		Date gmtSuccessClearChannel = DateUtil.format(wxBody.getTime_end(),DateUtil.YYYYMMDDHHMMSS);
		ledger.setGmtSuccessClearChannel(gmtSuccessClearChannel);
		//收付款渠道订单号	来自微信异步返回【微信支付订单号】
		ledger.setThirdTradeNo(wxBody.getTransaction_id());
	}

	@Override
	public void prepareFailureLedger(TradeRequestBody body, TradeLedger ledger) {
		//TODO 预留，组装失效的台账数据
		/*WeiXinUnifiedOrderPacket wxBody = (WeiXinUnifiedOrderPacket) body;
		//收付款备注2【付款方账户】	来自微信异步返回【用户子标识】
		ledger.setExtendField2(wxBody.getSub_openid());
		//收付款备注3【付款方账户名称】	暂空
		ledger.setExtendField3(null);
		//收付款备注6【收款账户】	暂空
		ledger.setExtendField6(null);
		//收付款备注7【收款账户名称】	暂空
		ledger.setExtendField7(null);
		//外部渠道收付款成功日期时间	来自微信异步返回【支付完成时间】
		Date gmtSuccessClearChannel = DateUtil.format(wxBody.getTime_end(),DateUtil.YYYYMMDDHHMMSS);
		ledger.setGmtSuccessClearChannel(gmtSuccessClearChannel);
		//收付款渠道订单号	来自微信异步返回【微信支付订单号】
		ledger.setThirdTradeNo(wxBody.getTransaction_id());*/
	}
}
