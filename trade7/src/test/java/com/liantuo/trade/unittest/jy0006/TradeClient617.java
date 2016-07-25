package com.liantuo.trade.unittest.jy0006;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Random;

import org.apache.poi.hssf.record.FooterRecord;
import org.junit.Before;
import org.junit.Test;

import com.liantuo.trade.client.trade.TradeClient;
import com.liantuo.trade.client.trade.packet.TradeRequest;
import com.liantuo.trade.client.trade.packet.TradeResponse;
import com.liantuo.trade.client.trade.packet.body.TradePacketReqBodyOtherPofitLossLedger;
import com.liantuo.trade.client.trade.packet.body.single_payment.TradePacketReqBodyExternalAsynPayment;
import com.liantuo.trade.client.trade.packet.body.zf.AliPaymentAsynAttachment;
import com.liantuo.trade.client.trade.packet.body.zf.Attachment;
import com.liantuo.trade.client.trade.packet.body.zf.Extend_params;
import com.liantuo.trade.client.trade.packet.body.zf.Goods_Detail;
import com.liantuo.trade.client.trade.packet.body.zf.WxPaymentAsynAttachment;
import com.liantuo.trade.client.trade.packet.head.TradePacketHead;
import com.liantuo.trade.common.util.json.ObjectJsonUtil;

public class TradeClient617 {
	TradeRequest<TradePacketReqBodyExternalAsynPayment> bizReq = new TradeRequest<TradePacketReqBodyExternalAsynPayment>();

	@Before
	public void test0006001007() {
		TradePacketHead head = new TradePacketHead();
		TradePacketReqBodyExternalAsynPayment body = new TradePacketReqBodyExternalAsynPayment();
		head.setSign("f644f5387aaeadae26bf45bf6d4759c2");
		head.setRequest_time(new SimpleDateFormat("yyyy-MM-dd hh:mm:ss.SSS").format(Calendar.getInstance().getTime()));
		head.setRequest_code("0006_001_007");
		head.setVersion("1.0");

		head.setPartner_id("16070615445815544");
		// 核心商户编号 来自输入
		head.setCore_merchant_no("EW_N5496622881");
		// 资金池编号 来自输入
		head.setFund_pool_no("PN02000000000003860");

		// //交易客户保留字段1 来自输入
		body.setMerchant_extend_field_1("617_mef1");
		// //交易客户保留字段2 来自输入
		body.setMerchant_extend_field_2("617_mef2");
		// //交易客户保留字段3 来自输入
		body.setMerchant_extend_field_3("617_mef3");
		// //交易客户保留字段4 来自输入
		body.setMerchant_extend_field_4("617_mef4");
		// //交易客户保留字段5 来自输入
		body.setMerchant_extend_field_5("617_mef5");
		body.setMerchant_extend_field_6("617_mef6");
		body.setMerchant_extend_field_7("617_mef7");
		body.setMerchant_extend_field_8("617_mef8");
		body.setMerchant_extend_field_9("617_mef6");
		body.setMerchant_extend_field_10("617_mef10");

		// 交易发起方发起请求编号 来自输入
		body.setOut_trade_no_ext("20160718_no_ext" + new Random().nextInt(1000));
		// 交易发起方业务系统订单号 来自输入
		body.setOut_trade_no("20160718_no" + new Random().nextInt(1000));
		// 异步通知地址
		body.setNotify_url("617_notify_url");
		/**
		 * 付款方数据
		 */
		// 付款方付出方式
		body.setPay_type("5");// 4-支付 5-充付

		body.setPay_account_no("CA31000000000660");
		// 付款方账户出账金额-----【付款方付出方式】为第三方充付必填
		body.setPay_account_out_accounting_amt("200");
		body.setPay_account_out_accounting_his1("617_poh1");
		body.setPay_account_out_accounting_his2("617_poh2");
		body.setPay_account_out_accounting_his3("617_poh3");
		body.setPay_account_in_accounting_his1("617_pih1");
		body.setPay_account_in_accounting_his2("617_pih2");
		body.setPay_account_in_accounting_his3("617_pih3");

		// 付款方手续费账户出账金额不为空时，不可为空
		body.setPay_fee_account_no("CA31000000000661");
		body.setPay_fee_account_out_accounting_amt("1");
		body.setPay_fee_account_out_accounting_his1("617_pfoh3");
		body.setPay_fee_account_out_accounting_his2("617_pfoh2");
		body.setPay_fee_account_out_accounting_his3("617_pfoh3");

		/**
		 * 收款方数据
		 */
		body.setReceive_type("1");// 1-账户 2-损益

		// 收款方式为损益时，需有值
		// body.setReceive_profit_loss_income_incr("99");
		// body.setReceive_profit_loss_cost_decr("");
		// 收款方式为账户时，需有值

		body.setReceive_account_no("CA31000000000663");
		body.setReceive_account_in_accounting_amt("200");
		body.setReceive_account_in_accounting_type("1");// 2-冻结 1-可用
		body.setReceive_account_in_accounting_his1("617_rih1");
		body.setReceive_account_in_accounting_his2("617_rih2");
		body.setReceive_account_in_accounting_his3("617_rih3");

		// 收款方手续费账户出账金额不为空时，不可为空
		body.setReceive_fee_account_no("CA31000000000662");
		body.setReceive_fee_account_out_accounting_type("1");
		body.setReceive_fee_account_out_accounting_amt("1");
		body.setReceive_fee_account_out_accounting_his1("617_rfoh1");
		body.setReceive_fee_account_out_accounting_his2("617_rfoh2");
		body.setReceive_fee_account_out_accounting_his3("617_rfoh3");

		TradePacketReqBodyOtherPofitLossLedger profit = new TradePacketReqBodyOtherPofitLossLedger();
		profit.setIncome_incr("2");
		List<TradePacketReqBodyOtherPofitLossLedger> profit_list = new ArrayList<TradePacketReqBodyOtherPofitLossLedger>();
		profit_list.add(profit);
		body.setProfit_loss_list(profit_list);
		// 订单金额
		body.setTotal_amount("200");
		// 商品标题
		body.setSubject("订单标题");
		// 商品描述
		body.setBody("订单描述");
		// 业务台账客户保留字段1
		body.setLedger_merchant_extend_field_1("lg_mef1");
		// 业务台账客户保留字段2
		body.setLedger_merchant_extend_field_2("lg_mef2");
		// 业务台账客户保留字段3
		body.setLedger_merchant_extend_field_3("lg_mef3");
		bizReq.setHead(head);
		bizReq.setBody(body);

	}

	private void initAlipay(TradePacketReqBodyExternalAsynPayment body) throws Exception {
		AliPaymentAsynAttachment attachment = new AliPaymentAsynAttachment();
		// attachment.setApp_auth_token("");
		attachment.setBuyer_logon_id("2088912405619142");
		// attachment.setGoods_detail(goods_detail);
		attachment.setOperator_id("oper_20160616");
		attachment.setSeller_id("2088101568353491");
		attachment.setStore_id("stor_20160707");
		attachment.setTerminal_id("term_20160707");
		attachment.setTimeout_express("1h");
		body.setOther_attachment_group_json(ObjectJsonUtil.object2String(attachment));
		body.setPay_channel("ZF0001_02_002");
		body.setPay_transaction_id("000000000000109");

	}

	private void initWxpay(TradePacketReqBodyExternalAsynPayment body) throws Exception {
		WxPaymentAsynAttachment wx = new WxPaymentAsynAttachment();
		wx.setAttach("attach20160707");
		wx.setDevice_info("decive20160707");
		wx.setFee_type("CNY");
		wx.setGoods_tag("goods20160707");
		wx.setLimit_pay("limit20160707");
		wx.setProduct_id("prod20160707");
		wx.setSpbill_create_ip("192.168.8.133");
		wx.setSub_appid("wx41a886877df49e6a");
		wx.setSub_openid("oOALutwjvSAUuaZh2xARZfcDQSwE");
		wx.setTime_expire("20160715174320");
		wx.setTime_start("20160715101820");
		wx.setTrade_type("JSAPI");
//		body.setOther_attachment_group_json(ObjectJsonUtil.object2String(wx));
		body.setPay_transaction_id("000000000000021");
		body.setPay_channel("ZF0003_01_002");
	}

	@Test
	public void test0006001007AliPay() {
		try {
			initAlipay(bizReq.getBody());
			TradeResponse response = TradeClient.excute(bizReq);
			if (null != response) {
				System.err.println(response.marshal());
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	@Test
	public void test0006001007WxPay() {
		try {
			initWxpay(bizReq.getBody());
			TradeResponse response = TradeClient.excute(bizReq);
			if (null != response) {
				System.err.println(response.marshal());
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public static void main(String[] args) throws Exception {
		AliPaymentAsynAttachment attachment = new AliPaymentAsynAttachment();
		attachment.setApp_auth_token("");
		attachment.setBuyer_logon_id("2088912405619142");
		List<Goods_Detail> details = new ArrayList<Goods_Detail>();
		Goods_Detail gd = new  Goods_Detail();
		gd.setAlipay_goods_id("alipayGoodsId");
		gd.setBody("body");
		gd.setGoods_category("goodsCategory");
		gd.setGoods_id("goodsId");
		gd.setGoods_name("goodsName");
		gd.setPrice("1");
		gd.setQuantity("2");
		details.add(gd);
		attachment.setGoods_detail(details);
		attachment.setOperator_id("oper_20160616");
		attachment.setSeller_id("2088101568353491");
		attachment.setStore_id("stor_20160707");
		attachment.setTerminal_id("term_20160707");
		attachment.setTimeout_express("1h");
		Extend_params ep = new Extend_params();
		ep.setHb_fq_num("1");
		ep.setHb_fq_seller_percent("per");
		ep.setSys_service_provider_id("sysServiceProviderId");
		attachment.setExtend_params(ep);
		System.out.println("ZF0001_02_002:  "+ObjectJsonUtil.object2String(attachment));
		WxPaymentAsynAttachment wx = new WxPaymentAsynAttachment();
		wx.setSub_appid("wx41a886877df49e6a");
		wx.setAttach("attach20160707");
		wx.setDevice_info("decive20160707");
		wx.setAttach("attach");
		wx.setFee_type("CNY");
		wx.setSpbill_create_ip("192.168.8.133");
		wx.setTime_expire("20160715174320");
		wx.setTime_start("20160715101820");
		wx.setGoods_tag("goods20160707");
		wx.setProduct_id("prod20160707");
		wx.setLimit_pay("limit20160707");
		wx.setSub_openid("oOALutwjvSAUuaZh2xARZfcDQSwE");
		wx.setTrade_type("JSAPI");
		System.out.println("ZF0003_01_002:  "+ObjectJsonUtil.object2String(wx));
	}
}
