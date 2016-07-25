package com.liantuo.trade.bus.process.impl.single_payment;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;

import com.alibaba.druid.util.StringUtils;
import com.google.common.collect.Lists;
import com.liantuo.deposit.advanceaccount.bus.service.AdvanceAccountService;
import com.liantuo.deposit.advanceaccount.bus.service.CreditAccountService;
import com.liantuo.deposit.advanceaccount.bus.vo.AdvanceAccountVO;
import com.liantuo.payment.client.pay.PaymentResponse;
import com.liantuo.payment.client.pay.alipay.base.ExtendParams;
import com.liantuo.payment.client.pay.alipay.base.GoodsDetail;
import com.liantuo.payment.client.pay.alipay.vo1.req.AlipayTradePrecreateRequest;
import com.liantuo.payment.client.pay.alipay.vo1.rsp.AlipayTradePrecreateResponse;
import com.liantuo.payment.client.pay.head.PaymentHead;
import com.liantuo.payment.client.pay.weixin.vo1.agent.req.WeiXinUnifiedOrderRequest;
import com.liantuo.payment.client.pay.weixin.vo1.agent.rsp.WeiXinUnifiedOrderResponse;
import com.liantuo.trade.bus.process.TradeCreateSingleTxHasPaymentInterface;
import com.liantuo.trade.bus.service.BizAccountService;
import com.liantuo.trade.bus.service.ExceptionService;
import com.liantuo.trade.bus.service.LegderService;
import com.liantuo.trade.bus.service.ProfitLossLedgerService;
import com.liantuo.trade.bus.service.SinglePaymentService;
import com.liantuo.trade.bus.service.TradeRequestPaymentInterface;
import com.liantuo.trade.bus.template.impl.v1_1.create.ATradeCreateSingleTxHasPaymentTemp;
import com.liantuo.trade.client.trade.packet.TradeRequest;
import com.liantuo.trade.client.trade.packet.body.TradePacketReqBodyOtherPofitLossLedger;
import com.liantuo.trade.client.trade.packet.body.single_payment.TradePacketReqBodyExternalAsynPayment;
import com.liantuo.trade.client.trade.packet.body.single_payment_refund.OtherPofitLossLedgerForRefund;
import com.liantuo.trade.client.trade.packet.body.zf.AliPaymentAsynAttachment;
import com.liantuo.trade.client.trade.packet.body.zf.Goods_Detail;
import com.liantuo.trade.client.trade.packet.body.zf.WxPaymentAsynAttachment;
import com.liantuo.trade.common.constants.TradeConstants;
import com.liantuo.trade.common.util.amount.AmountUtils;
import com.liantuo.trade.common.util.json.ObjectJsonUtil;
import com.liantuo.trade.common.util.trade.TradeCommonValidation;
import com.liantuo.trade.common.util.trade.TradeUtilCommon;
import com.liantuo.trade.common.util.xml.ObjectXmlUtil;
import com.liantuo.trade.common.validate.TradeValidationUtil;
import com.liantuo.trade.common.validate.ValidationUtil;
import com.liantuo.trade.exception.AmountConvertException;
import com.liantuo.trade.orm.pojo.TradeLedger;
import com.liantuo.trade.orm.pojo.TradeProfitLossLedger;
import com.liantuo.trade.orm.pojo.TradeSinglePayment;
import com.liantuo.trade.orm.pojo.TradeSinglePaymentExample;
import com.liantuo.trade.orm.pojo.subvo.TradeSinglePaymentVo;
import com.liantuo.trade.seqno.IdFactory;
import com.liantuo.trade.spring.annotation.JobFlow;

/**
 * @ClassName: SingleExternalAsynPaymentOnlineProcess.java
 * @Description: 外部收款同步获取连接
 * @Company: 联拓金融信息服务有限公司
 * @author zoran.huang
 * @version V1.0
 * @date 2016年7月6日 下午3:11:39
 */
// TODO 修改错误码
@JobFlow(value = "0006_001_007", version = "1.0", template = ATradeCreateSingleTxHasPaymentTemp.class)
public class SingleExternalAsynPaymentOnlineProcess implements TradeCreateSingleTxHasPaymentInterface<TradePacketReqBodyExternalAsynPayment> {
	private static Logger logger = LoggerFactory.getLogger(SingleExternalAsynPaymentOnlineProcess.class);
	private final static int INIT_VERSION = 0;
	@Autowired
	private ExceptionService exceptionService;
	@Resource(name = "creditAccountServiceImpl")
	private CreditAccountService creditAccountService;
	@Resource(name = "idFactorySinglePaymentTradeNo")
	private IdFactory idFactorySinglePaymentTradeNo;

	@Resource(name = "idFactorySinglePaymentSerialNumber")
	private IdFactory idFactorySinglePaymentSerialNumber;
	@Resource(name = "legderServiceImpl")
	private LegderService legderService;
	@Resource(name = "profitLossLedgerServiceImpl")
	private ProfitLossLedgerService profitLossLedgerService;
	@Resource(name = "singlePaymentServiceImpl")
	private SinglePaymentService singlePaymentService;
	@SuppressWarnings("rawtypes")
	@Resource(name = "bizAccountServiceImpl")
	private BizAccountService bizAccountService;
	@Resource(name = "advanceAccountServiceImpl")
	private AdvanceAccountService advanceAccountService;

	private String tradeNo;// 交易编号
	private String reqNo;// 请求编号
	private String payLedgerNo;// 付款方台账编号
	private String otherProfitLossLedgerNo;// 其他损益台账编号
	private String receiveProfitLossLedgerNo;// 收款损益台账编号
	private String payMerchantNo;// 付款商户编号
	private String payFeeMerchantNo;// 付款方手续费账户商户编号
	private String receiveMerchantNo;// 收款方账户商户编号
	private String receiveFeeMerchantNo;// 收款方手续费账户商户编号
	private TradeSinglePayment tradeSinglePayment;// 支付交易记录
	private List<TradeProfitLossLedger> otherProfitLossLedgerList;// 其他损益台账list
	private String jsonList = "";// 其他损益台账ListJson

	private AliPaymentAsynAttachment aliAsynAttachment;// 支付宝其他组别属性
	private WxPaymentAsynAttachment wxAsynAttachment;// 微信其他组别属性
	private String payChannel;// 渠道编号
	private TradeLedger ledger;

	private String responseXml;// 返回报文xml字符串
	private AlipayTradePrecreateRequest aliPayRequest;
	private WeiXinUnifiedOrderRequest wxPayRequest;
	private String pay_url ; 
	@Autowired
	private TradeRequestPaymentInterface paymentInterface;

	@Override
	public void validate(TradeRequest<TradePacketReqBodyExternalAsynPayment> tradeRequest) throws Exception {
		// 校验输入参格式
		formatValidate(tradeRequest);
		// 参数正确性校验
		bizValidate(tradeRequest);
	}

	/**
	 * 校验输入参数格式
	 */
	private void formatValidate(TradeRequest<TradePacketReqBodyExternalAsynPayment> tradeRequest) throws Exception {
		String msg = TradeValidationUtil.validateRequestWeak(tradeRequest);
		if (StringUtils.isEmpty(msg)) {
			payChannel = tradeRequest.getBody().getPay_channel();
			
			try {
				if (TradeConstants.ALI_ASYN_PAY_CHANNEL.equals(payChannel)) {
					aliAsynAttachment = ObjectJsonUtil.string2Object(tradeRequest.getBody().getOther_attachment_group_json(), AliPaymentAsynAttachment.class);
					msg = ValidationUtil.validateRequiredWeaks(aliAsynAttachment);
				} else if (TradeConstants.WX_ASYN_PAY_CHANNEL.equals(payChannel)) {
					wxAsynAttachment = ObjectJsonUtil.string2Object(tradeRequest.getBody().getOther_attachment_group_json(), WxPaymentAsynAttachment.class);
					msg = ValidationUtil.validateRequiredWeaks(wxAsynAttachment);
				}
			} catch (Exception e) {
				logger.info("-->第三方参数输入异常，渠道编号为：" + payChannel);
				throw exceptionService.buildBusinessException("JY00060010071000100", "第三方参数输入异常");
			}
			
		}
		if (!StringUtils.isEmpty(msg)) {
			logger.info("-->输入参数校验不通过：" + msg);
			throw exceptionService.buildBusinessException("JY00060010071000100", msg);
		}
		// 当【收款方账户入账子账户类型】为【冻结】时，若【收款方手续费账户账户编号】与【收款方账户账户编号】不同，则【收款方手续费出账子账户类型】只能是【可用】，若相同，则能是【可用】或【冻结】
		if (TradeConstants.TRADE_ACCOUNT_FREEZE_TYPE.equals(tradeRequest.getBody().getReceive_account_in_accounting_type()) && TradeConstants.RECEIVE_BY_THIRD_ACCOUNT.equals(tradeRequest.getBody().getReceive_type())) {
			if (TradeConstants.TRADE_ACCOUNT_FREEZE_TYPE.equals(tradeRequest.getBody().getReceive_fee_account_out_accounting_type()) && !tradeRequest.getBody().getReceive_account_no().equals(tradeRequest.getBody().getReceive_fee_account_no())) {
				logger.info("-->收款账户和收款手续费账户不同时，收款子账户类型必须为可用");
				throw exceptionService.buildBusinessException("JY00060010071000100", "收款账户和收款手续费账户不同时，收款手续费子账户类型必须为可用");
			}
		} else if (TradeConstants.TRADE_ACCONUT_AVAILABLE_TYPE.equals(tradeRequest.getBody().getReceive_account_in_accounting_type()) && TradeConstants.RECEIVE_BY_THIRD_ACCOUNT.equals(tradeRequest.getBody().getReceive_type())) {
			if (TradeConstants.TRADE_ACCOUNT_FREEZE_TYPE.equals(tradeRequest.getBody().getReceive_fee_account_out_accounting_type())) {
				logger.info("-->当收款账户子类型为可用时，收款手续费账户子类型必须为可用");
				throw exceptionService.buildBusinessException("JY00060010071000100", "当收款账户子类型为可用时，收款手续费账户子类型必须为可用");
			}
		} else if (TradeConstants.RECEIVE_BY_THIRD_PROFIT_LOSS.equals(tradeRequest.getBody().getReceive_type()) && TradeConstants.TRADE_ACCOUNT_FREEZE_TYPE.equals(tradeRequest.getBody().getReceive_fee_account_out_accounting_type())) {
			logger.info("-->当收款方式为损益时，收款手续费账户子类型必须为可用");
			throw exceptionService.buildBusinessException("JY00060010071000100", "当收款方式为损益时，收款手续费账户子类型必须为可用");
		}
		// 收款方手续费账户出账金额,需小于等于【收款方收到金额】
		if (TradeConstants.RECEIVE_BY_THIRD_ACCOUNT.equals(tradeRequest.getBody().getReceive_type()) && !StringUtils.isEmpty(tradeRequest.getBody().getReceive_fee_account_out_accounting_amt())) {
			if (!compareAmt(tradeRequest.getBody().getReceive_fee_account_out_accounting_amt(), tradeRequest.getBody().getReceive_account_in_accounting_amt())) {
				logger.info("--> " + "收款方手续费账户出账金额需要小于或等于收款方收到金额");
				throw exceptionService.buildBusinessException("JY00060010071000100", "收款方手续费账户出账金额需要小于或等于收款方收到金额");
			}
		} else if (TradeConstants.RECEIVE_BY_THIRD_PROFIT_LOSS.equals(tradeRequest.getBody().getReceive_type()) && !StringUtils.isEmpty(tradeRequest.getBody().getReceive_fee_account_out_accounting_amt())) {
			if (!StringUtils.isEmpty(tradeRequest.getBody().getReceive_profit_loss_income_incr()) && !compareAmt(tradeRequest.getBody().getReceive_fee_account_out_accounting_amt(), tradeRequest.getBody().getReceive_profit_loss_income_incr())) {
				logger.info("--> " + "收款方手续费账户出账金额需要小于或等于收款方收到金额");
				throw exceptionService.buildBusinessException("JY00060010071000100", "收款方手续费账户出账金额需要小于或等于收款方收到金额");
			} else if (!StringUtils.isEmpty(tradeRequest.getBody().getReceive_profit_loss_cost_decr()) && !compareAmt(tradeRequest.getBody().getReceive_fee_account_out_accounting_amt(), tradeRequest.getBody().getReceive_profit_loss_cost_decr())) {
				logger.info("--> " + "收款方手续费账户出账金额需要小于或等于收款方收到金额");
				throw exceptionService.buildBusinessException("JY00060010071000100", "收款方手续费账户出账金额需要小于或等于收款方收到金额");
			}
		}
	}

	/**
	 * 校验【交易发起方发起请求编号】是否重复 校验【核心商户编号】、【资金池编号】对应关系 校验【合作商户编号】权限（暂不实现）
	 * 校验【付款方账户账户编号】正确性 校验【付款方手续费账户账户编号】正确性 校验【收款方账户账户编号】正确性 校验【收款方手续费账户账户编号】正确性
	 * 校验金额关系
	 *
	 * @throws Exception
	 */
	private void bizValidate(TradeRequest<TradePacketReqBodyExternalAsynPayment> tradeRequest) throws Exception {
		// 校验交易发起方请求编号是否重复
		checkDuplicate(tradeRequest);
		// 校验【核心商户编号】、【资金池编号】对应关系
		TradeCommonValidation.validateCoreMerchantNoAndFundPoolNo(tradeRequest, false, exceptionService.buildBusinessException("JY00060010071000200"));
		// 校验【付款方账户账户编号】正确性
		if (!StringUtils.isEmpty(tradeRequest.getBody().getPay_account_no())) {
			AdvanceAccountVO accountVo = getCreditAccountInfo(tradeRequest.getBody().getPay_account_no());
			if (accountVo == null || !(tradeRequest.getHead().getCore_merchant_no().equals(accountVo.getCoreMerchantNo()) && tradeRequest.getHead().getFund_pool_no().equals(accountVo.getPoolNo()))) {
				logger.info("--> " + "【付款方账户账户编号】与【核心商户编号】、【资金池编号】、【所属成员商户编号】 不对应");
				throw exceptionService.buildBusinessException("JY00060010071000500");
			} else {
				payMerchantNo = accountVo.getMerchantNo();
			}
		}
		// 校验【付款方手续费账户账户编号】正确性
		if (!StringUtils.isEmpty(tradeRequest.getBody().getPay_fee_account_no())) {
			AdvanceAccountVO accountVo = getCreditAccountInfo(tradeRequest.getBody().getPay_fee_account_no());
			if (accountVo == null || !(tradeRequest.getHead().getCore_merchant_no().equals(accountVo.getCoreMerchantNo()) && tradeRequest.getHead().getFund_pool_no().equals(accountVo.getPoolNo()))) {
				logger.info("--> " + "【付款方手续费账户账户编号】与【核心商户编号】、【资金池编号】、【所属成员商户编号】 不对应");
				throw exceptionService.buildBusinessException("JY00060010071000600");
			} else {
				payFeeMerchantNo = accountVo.getMerchantNo();
			}
		}
		// 校验【收款方账户账户编号】正确性
		if (!StringUtils.isEmpty(tradeRequest.getBody().getReceive_account_no())) {
			AdvanceAccountVO accountVo = getCreditAccountInfo(tradeRequest.getBody().getReceive_account_no());
			if (accountVo == null || !(tradeRequest.getHead().getCore_merchant_no().equals(accountVo.getCoreMerchantNo()) && tradeRequest.getHead().getFund_pool_no().equals(accountVo.getPoolNo()))) {
				logger.info("--> " + "【收款方账户账户编号】与【核心商户编号】、【资金池编号】、【所属成员商户编号】 不对应");
				throw exceptionService.buildBusinessException("JY00060010071000700");
			} else {
				receiveMerchantNo = accountVo.getMerchantNo();
			}
		}
		// 校验【收款方手续费账户账户编号】正确性
		if (!StringUtils.isEmpty(tradeRequest.getBody().getReceive_fee_account_no())) {
			AdvanceAccountVO accountVo = getCreditAccountInfo(tradeRequest.getBody().getReceive_fee_account_no());
			if (accountVo == null || !(tradeRequest.getHead().getCore_merchant_no().equals(accountVo.getCoreMerchantNo()) && tradeRequest.getHead().getFund_pool_no().equals(accountVo.getPoolNo()))) {
				logger.info("--> " + "【收款方手续费账户账户编号】与【核心商户编号】、【资金池编号】、【所属成员商户编号】 不对应");
				throw exceptionService.buildBusinessException("JY00060010071000800");
			} else {
				receiveFeeMerchantNo = accountVo.getMerchantNo();
			}
		}
		// 校验金额关系
		checkAmountRelation(tradeRequest);
	}

	/**
	 * 校验以下各项金额，按照正负向分布汇总后应相等，若不等，校验失败，返回【失败】 付款方外部收款金额 正向，【付款方付出方式】为【第三方充付】时不计算
	 * 付款方账户出账金额 正向【付款方付出方式】为【第三方直付】时不计算 付款方手续费账户出账金额 正向 收款方账户入账金额 负向
	 * 收款方损益收入增加金额 负向 收款方损益成本费用减少金额 负向 收款方手续费账户出账金额 正向 其他损益台账X收入增加金额 负向
	 * 其他损益台账X收入减少金额 正向 其他损益台账X成本费用增加金额 正向 其他损益台账X成本费用减少金额 负向
	 *
	 * @param tradeRequest
	 */
	private void checkAmountRelation(TradeRequest<TradePacketReqBodyExternalAsynPayment> tradeRequest) {
		long positiveTotal = 0;
		long negativeTotal = 0;
		if (!TradeConstants.PAY_BY_THIRD_ACCOUNT.equals(tradeRequest.getBody().getPay_type())) {
			positiveTotal += strToLong(tradeRequest.getBody().getTotal_amount());// +
		}
		if (!TradeConstants.PAY_BY_THIRD.equals(tradeRequest.getBody().getPay_type())) {
			positiveTotal += strToLong(tradeRequest.getBody().getPay_account_out_accounting_amt());// +
		}
		positiveTotal += strToLong(tradeRequest.getBody().getPay_fee_account_out_accounting_amt());// +
		if (TradeConstants.RECEIVE_BY_THIRD_ACCOUNT.equals(tradeRequest.getBody().getReceive_type())) {
			negativeTotal += strToLong(tradeRequest.getBody().getReceive_account_in_accounting_amt());// -
		} else if (TradeConstants.RECEIVE_BY_THIRD_PROFIT_LOSS.equals(tradeRequest.getBody().getReceive_type())) {
			negativeTotal += strToLong(tradeRequest.getBody().getReceive_profit_loss_income_incr());// -
			negativeTotal += strToLong(tradeRequest.getBody().getReceive_profit_loss_cost_decr());// -
		}
		positiveTotal += strToLong(tradeRequest.getBody().getReceive_fee_account_out_accounting_amt());// +
		List<TradePacketReqBodyOtherPofitLossLedger> list = tradeRequest.getBody().getProfit_loss_list();
		if (list != null) {
			for (TradePacketReqBodyOtherPofitLossLedger ledger : list) {
				negativeTotal += strToLong(ledger.getIncome_incr());
				positiveTotal += strToLong(ledger.getIncome_decr());
				positiveTotal += strToLong(ledger.getCost_incr());
				negativeTotal += strToLong(ledger.getCost_decr());
			}
		}
		if (positiveTotal != negativeTotal) {
			logger.info("-->金额合计校验不通过！");
			throw exceptionService.buildBusinessException("JY00060010071000900");
		}
	}

	@Override
	public void transaction(TradeRequest<TradePacketReqBodyExternalAsynPayment> tradeRequest) throws Exception {
		// 获取充值交易编号
		this.tradeNo = idFactorySinglePaymentTradeNo.generate().toString();
		Date currentTime = new Date();
		// 创建空白的付款方外部收款台账
		createBlankLegder(tradeRequest, currentTime);
		// 创建其他空白损益台账
		createExtendBlankProfitLossLegder(tradeRequest, currentTime);
		// 创建空白收款损益台账
		createBlankProfitLossLegder(tradeRequest, currentTime);
		// 创建等待付款交易记录
		createTradeForPayOfWait(tradeRequest, currentTime);

	}

	/**
	 * 创建等待入账交易记录
	 *
	 * @param tradeRequest
	 * @param currentTime
	 */
	private void createTradeForPayOfWait(TradeRequest<TradePacketReqBodyExternalAsynPayment> tradeRequest, Date currentTime) {
		TradeSinglePayment singlePayment = new TradeSinglePayment();
		try {
			/**
			 * 交易公共部分
			 */
			singlePayment.setTradeNo(this.tradeNo);
			singlePayment.setCoreMerchantNo(tradeRequest.getHead().getCore_merchant_no());// 核心商户编号
			singlePayment.setFundPoolNo(tradeRequest.getHead().getFund_pool_no());// 资金池编号
			singlePayment.setPartnerId(tradeRequest.getHead().getPartner_id());
			singlePayment.setMerchantExtendField1(tradeRequest.getBody().getMerchant_extend_field_1());// 交易客户保留字段1
			singlePayment.setMerchantExtendField2(tradeRequest.getBody().getMerchant_extend_field_2());// 交易客户保留字段2
			singlePayment.setMerchantExtendField3(tradeRequest.getBody().getMerchant_extend_field_3());// 交易客户保留字段3
			singlePayment.setMerchantExtendField4(tradeRequest.getBody().getMerchant_extend_field_4());// 交易客户保留字段4
			singlePayment.setMerchantExtendField5(tradeRequest.getBody().getMerchant_extend_field_5());// 交易客户保留字段5
			singlePayment.setMerchantExtendField6(tradeRequest.getBody().getMerchant_extend_field_6());// 交易客户保留字段6
			singlePayment.setMerchantExtendField7(tradeRequest.getBody().getMerchant_extend_field_7());// 交易客户保留字段7
			singlePayment.setMerchantExtendField8(tradeRequest.getBody().getMerchant_extend_field_8());// 交易客户保留字段8
			singlePayment.setMerchantExtendField9(tradeRequest.getBody().getMerchant_extend_field_9());// 交易客户保留字段9
			singlePayment.setMerchantExtendField10(tradeRequest.getBody().getMerchant_extend_field_10());// 交易客户保留字段10
			singlePayment.setOutTradeNoExt(tradeRequest.getBody().getOut_trade_no_ext());// 交易发起方发起请求编号
			singlePayment.setOutTradeNo(tradeRequest.getBody().getOut_trade_no());// 交易发起方业务系统订单号
			singlePayment.setGmtCreated(currentTime);// 交易创建日期时间
			singlePayment.setGmtModifiedLatest(currentTime);// 最后变更日期时间
			singlePayment.setLatestTradeReqType(tradeRequest.getHead().getRequest_code());// 最后变更交易请求类型
			singlePayment.setLatestReqNo(this.reqNo);// 最后变更交易请求编号
			singlePayment.setCloseStatus(TradeConstants.TRADE_CLOSE_STATUS_INIT);// 交易结束状态
																					// ---未结束
			singlePayment.setStatus(TradeConstants.TRADE_PAYMENT_WAIT_PAY);// 交易状态
																				// ---等待入账
			singlePayment.setTradeCatagory(TradeConstants.PAY_TYPE_BY_THIRD);// 交易组别
																				// 1.内部支付
																				// 2.外部支付
			singlePayment.setVersion(INIT_VERSION);// 交易版本
			/**
			 * 交易时间部分
			 */
			singlePayment.setGmtWaitPay(currentTime);// 等待入账日期时间
			/**
			 * 付款方基本部分
			 */
			// 付款方付出金额---账户、第三方充付、损益充付与付款方账户出账金额相同
			if (TradeConstants.PAY_BY_THIRD_ACCOUNT.equals(tradeRequest.getBody().getPay_type())) {
				singlePayment.setPayAmt(strToLong(tradeRequest.getBody().getPay_account_out_accounting_amt()));
				// 付款方付出金额---第三方支付、损益直付与台账付出金额相同
			} else if (TradeConstants.PAY_BY_THIRD.equals(tradeRequest.getBody().getPay_type())) {
				singlePayment.setPayAmt(strToLong(tradeRequest.getBody().getTotal_amount()));
			}
			singlePayment.setPayType(tradeRequest.getBody().getPay_type());// 付款方付出方式
			/**
			 * 付出方外部部分
			 */
			singlePayment.setExternalPayLedgerNo(this.payLedgerNo);// 付款方外部业务台账编号
			singlePayment.setExternalPayChannel(this.ledger.getClearChannel());// 付款方外部收付款渠道编号
			singlePayment.setExternalPayChannelFeeAmt(str2Long(this.ledger.getClearChannelAttr1()));// 付款方外部收付款属性1（付出手续费金额）
			singlePayment.setExternalPayChannelRefundFeeAmt(str2Long(this.ledger.getClearChannelAttr2()));// 付款方外部收付款属性2（退回手续费金额）
			singlePayment.setExternalPayReceivedAmt(this.ledger.getReceiveAmount());// 付款方外部收款金额
			singlePayment.setExternalPayChannelBatchNo(this.ledger.getExtendField4());// 付款方外部收付款备注4【提交第三方批次号】
			singlePayment.setExternalPayChannelSerialNo(this.ledger.getExtendField5());// 付款方外部收付款备注5【提交第三方请求流水号】
			/**
			 * 付出方账户部分
			 */
			singlePayment.setPayAccountNo(tradeRequest.getBody().getPay_account_no());// 付款方账户账户编号
			singlePayment.setPayAccountMerchantNo(this.payMerchantNo);// 付款方账户所属商户编号
			singlePayment.setPayAccountOutAccountingAmt(strToLong(tradeRequest.getBody().getPay_account_out_accounting_amt()));// 付款方账户出账金额
			singlePayment.setPayAccountOutAccountingType(TradeConstants.TRADE_ACCONUT_AVAILABLE_TYPE);// 付款方账户出账子账户类型
			singlePayment.setPayAccountOutAccountingHis1(tradeRequest.getBody().getPay_account_out_accounting_his1());// 付款方账户出账账务历史1
			singlePayment.setPayAccountOutAccountingHis2(tradeRequest.getBody().getPay_account_out_accounting_his2());// 付款方账户出账账务历史2
			singlePayment.setPayAccountOutAccountingHis3(tradeRequest.getBody().getPay_account_out_accounting_his3());// 付款方账户出账账务历史3
			singlePayment.setPayAccountInAccountingType(TradeConstants.TRADE_ACCONUT_AVAILABLE_TYPE);// 付款方账户入账子账户类型
			singlePayment.setPayAccountInAccountingHis1(tradeRequest.getBody().getPay_account_in_accounting_his1());// 付款方账户入账账务历史1
			singlePayment.setPayAccountInAccountingHis2(tradeRequest.getBody().getPay_account_in_accounting_his2());// 付款方账户入账账务历史2
			singlePayment.setPayAccountInAccountingHis3(tradeRequest.getBody().getPay_account_in_accounting_his3());// 付款方账户入账账务历史3
			/**
			 * 付款方手续费账户部分
			 */
			singlePayment.setPayFeeAccountNo(tradeRequest.getBody().getPay_fee_account_no());// 付款方手续费账户账户编号
			singlePayment.setPayFeeAccountMerchantNo(this.payFeeMerchantNo);// 付款方手续费账户所属商户编号
			singlePayment.setPayFeeAccountOutAccountingAmt(strToLong(tradeRequest.getBody().getPay_fee_account_out_accounting_amt()));// 付款方手续费账户出账金额
			singlePayment.setPayFeeAccountOutAccountingType(TradeConstants.TRADE_ACCONUT_AVAILABLE_TYPE);// 付款方手续费账户出账子账户类型
			singlePayment.setPayFeeAccountOutAccountingHis1(tradeRequest.getBody().getPay_fee_account_out_accounting_his1());// 付款方手续费账户出账账务历史1
			singlePayment.setPayFeeAccountOutAccountingHis2(tradeRequest.getBody().getPay_fee_account_out_accounting_his2());// 付款方手续费账户出账账务历史2
			singlePayment.setPayFeeAccountOutAccountingHis3(tradeRequest.getBody().getPay_fee_account_out_accounting_his3());// 付款方手续费账户出账账务历史3
			/**
			 * 收到方基本部分
			 */
			// 收款方收到金额
			if (TradeConstants.RECEIVE_BY_THIRD_ACCOUNT.equals(tradeRequest.getBody().getReceive_type())) {// 收款方式为账户
				singlePayment.setReceiveAmt(strToLong(tradeRequest.getBody().getReceive_account_in_accounting_amt()));
			} else if (TradeConstants.RECEIVE_BY_THIRD_PROFIT_LOSS.equals(tradeRequest.getBody().getReceive_type()) && null != tradeRequest.getBody().getReceive_profit_loss_income_incr()) {// 收款方式为损益
				singlePayment.setReceiveAmt(strToLong(tradeRequest.getBody().getReceive_profit_loss_income_incr()));
			} else if (TradeConstants.RECEIVE_BY_THIRD_PROFIT_LOSS.equals(tradeRequest.getBody().getReceive_type()) && null != tradeRequest.getBody().getReceive_profit_loss_cost_decr()) {// 收款方式为损益
				singlePayment.setReceiveAmt(strToLong(tradeRequest.getBody().getReceive_profit_loss_cost_decr()));
			}
			singlePayment.setReceiveType(tradeRequest.getBody().getReceive_type());// 收款方收到方式
			/**
			 * 收到方损益部分
			 */
			if (TradeConstants.RECEIVE_BY_THIRD_PROFIT_LOSS.equals(tradeRequest.getBody().getReceive_type())) {
				singlePayment.setReceiveProfitLossLedgerNo(this.receiveProfitLossLedgerNo);// 收款方损益业务台账编号
				singlePayment.setReceiveProfitLossIncomeIncr(str2Long(tradeRequest.getBody().getReceive_profit_loss_income_incr()));// 收款方损益收入增加金额
				singlePayment.setReceiveProfitLossCostDecr(str2Long(tradeRequest.getBody().getReceive_profit_loss_cost_decr()));// //收款方损益成本费用减少金额
			}
			/**
			 * 收到方账户部分
			 */
			if (TradeConstants.RECEIVE_BY_THIRD_ACCOUNT.equals(tradeRequest.getBody().getReceive_type())) {
				singlePayment.setReceiveAccountNo(tradeRequest.getBody().getReceive_account_no());// 收款方账户账户编号
				singlePayment.setReceiveAccountMerchantNo(this.receiveMerchantNo);// 收款方账户所属商户编号
				singlePayment.setReceiveAccountInAccountingAmt(strToLong(tradeRequest.getBody().getReceive_account_in_accounting_amt()));// 收款方账户入账金额
				singlePayment.setReceiveAccountInAccountingType(tradeRequest.getBody().getReceive_account_in_accounting_type());// 收款方账户入账子账户类型
				singlePayment.setReceiveAccountInAccountingHis1(tradeRequest.getBody().getReceive_account_in_accounting_his1());// 收款方账户入账账务历史1
				singlePayment.setReceiveAccountInAccountingHis2(tradeRequest.getBody().getReceive_account_in_accounting_his2());// 收款方账户入账账务历史2
				singlePayment.setReceiveAccountInAccountingHis3(tradeRequest.getBody().getReceive_account_in_accounting_his3());// 收款方账户入账账务历史3
			}
			/**
			 * 收款方手续费账户部分
			 */
			singlePayment.setReceiveFeeAccountNo(tradeRequest.getBody().getReceive_fee_account_no());// 收款方手续费账户账户编号
			singlePayment.setReceiveFeeAccountMerchantNo(this.receiveFeeMerchantNo);// 收款方手续费账户所属商户编号
			singlePayment.setReceiveFeeAccountOutAccountingAmt(strToLong(tradeRequest.getBody().getReceive_fee_account_out_accounting_amt()));// 收款方手续费账户出账金额
			singlePayment.setReceiveFeeAccountOutAccountingType(tradeRequest.getBody().getReceive_fee_account_out_accounting_type());// 收款方手续费账户出账子账户类型
			singlePayment.setReceiveFeeAccountOutAccountingHis1(tradeRequest.getBody().getReceive_fee_account_out_accounting_his1());// 收款方手续费账户出账账务历史1
			singlePayment.setReceiveFeeAccountOutAccountingHis2(tradeRequest.getBody().getReceive_fee_account_out_accounting_his2());// 收款方手续费账户出账账务历史2
			singlePayment.setReceiveFeeAccountOutAccountingHis3(tradeRequest.getBody().getReceive_fee_account_out_accounting_his3());// 收款方手续费账户出账账务历史3

			/**
			 * 其他损益台账List部分
			 */
			singlePayment.setExtendProfitLoss(this.jsonList);
			/**
			 * 累计金额部分
			 */
			singlePayment.setContinueTradeStatus(TradeConstants.CONTINUE_TRADE_STATUS_OPEN);// 后续交易开关
			this.tradeSinglePayment = singlePaymentService.createForPayOfWait(singlePayment);

		} catch (Exception e) {
			e.printStackTrace();
			logger.error("-->等待付款交易记录创建失败：" + e.getMessage(), e);
			throw exceptionService.buildBusinessException("JY00060010071001000");
		}
	}

	/**
	 * 创建空白收款损益台账
	 *
	 * @param tradeRequest
	 * @param currentTime
	 */
	private void createBlankProfitLossLegder(TradeRequest<TradePacketReqBodyExternalAsynPayment> tradeRequest, Date currentTime) {
		if (TradeConstants.RECEIVE_BY_THIRD_PROFIT_LOSS.equals(tradeRequest.getBody().getReceive_type())) {
			try {
				TradeProfitLossLedger receiveProfitLossLedger = new TradeProfitLossLedger();
				receiveProfitLossLedger.setCoreMerchantNo(tradeRequest.getHead().getCore_merchant_no());// 收款方损益核心商户编号
				receiveProfitLossLedger.setFundPoolNo(tradeRequest.getHead().getFund_pool_no());// 收款方损益资金池编号
				receiveProfitLossLedger.setMerchantExtendField1(tradeRequest.getBody().getReceive_profit_loss_extend_field_1());// 收款方损益业务台账客户保留字段1
				receiveProfitLossLedger.setMerchantExtendField2(tradeRequest.getBody().getReceive_profit_loss_extend_field_2());// 收款方损益业务台账客户保留字段2
				receiveProfitLossLedger.setMerchantExtendField3(tradeRequest.getBody().getReceive_profit_loss_extend_field_3());// 收款方损益业务台账客户保留字段3
				receiveProfitLossLedger.setTradeType(TradeUtilCommon.getTradeType(tradeRequest.getHead().getRequest_code()));// 收款方损益所属业务交易类型
				receiveProfitLossLedger.setTradeNo(this.tradeNo);// 收款方损益所属业务交易编号
				receiveProfitLossLedger.setGmtTradeCreated(currentTime);// 收款方损益所属业务交易创建日期v
				receiveProfitLossLedger.setCreateReqType(tradeRequest.getHead().getRequest_code());// 收款方损益创建交易请求类型
				receiveProfitLossLedger.setCreateReqNo(this.reqNo);// 收款方损益创建交易请求编号
				receiveProfitLossLedger.setOutTradeNoExt(tradeRequest.getBody().getOut_trade_no_ext());// 交易发起方发起请求编号
				receiveProfitLossLedger.setOutTradeNo(tradeRequest.getBody().getOut_trade_no());// 交易发起方业务系统订单号
				receiveProfitLossLedger.setProfitLossAttr1(tradeRequest.getBody().getReceive_profit_loss_attr_1());// 收款方损益损益属性1
				receiveProfitLossLedger.setProfitLossAttr2(tradeRequest.getBody().getReceive_profit_loss_attr_2());// 收款方损益损益属性2
				receiveProfitLossLedger.setProfitLossAttr3(tradeRequest.getBody().getReceive_profit_loss_attr_3());// 收款方损益损益属性3
				receiveProfitLossLedger.setProfitLossAttr4(tradeRequest.getBody().getReceive_profit_loss_attr_4());// 收款方损益损益属性4
				receiveProfitLossLedger.setProfitLossAttr5(tradeRequest.getBody().getReceive_profit_loss_attr_5());// 收款方损益损益属性5
				receiveProfitLossLedger.setProfitLossAttr6(tradeRequest.getBody().getReceive_profit_loss_attr_6());// 收款方损益损益属性6
				receiveProfitLossLedger.setProfitLossAttr7(tradeRequest.getBody().getReceive_profit_loss_attr_7());// 收款方损益损益属性7
				receiveProfitLossLedger.setProfitLossAttr8(tradeRequest.getBody().getReceive_profit_loss_attr_8());// 收款方损益损益属性8
				receiveProfitLossLedger.setProfitLossAttr9(tradeRequest.getBody().getReceive_profit_loss_attr_9());// 收款方损益损益属性9
				receiveProfitLossLedger.setProfitLossAttr10(tradeRequest.getBody().getReceive_profit_loss_attr_10());// 收款方损益损益属性10
				receiveProfitLossLedger.setIncomeIncr(str2Long(tradeRequest.getBody().getReceive_profit_loss_income_incr()));// 收款方损益收入增加金额
				receiveProfitLossLedger.setCostDecr(str2Long(tradeRequest.getBody().getReceive_profit_loss_cost_decr()));// 收款方损益成本费用减少金额
				this.receiveProfitLossLedgerNo = profitLossLedgerService.createBlankProfitLossLedger(receiveProfitLossLedger);
			} catch (Exception e) {
				e.printStackTrace();
				logger.error("-->创建空白收款损益台账失败：" + e.getMessage(), e);
				throw exceptionService.buildBusinessException("TZ000200201");
			}
		}
	}

	/**
	 * 创建其他空白损益台账
	 *
	 * @param tradeRequest
	 * @param currentTime
	 */
	private void createExtendBlankProfitLossLegder(TradeRequest<TradePacketReqBodyExternalAsynPayment> tradeRequest, Date currentTime) {
		try {
			String core_merchant_no = tradeRequest.getHead().getCore_merchant_no();
			String fund_pool_no = tradeRequest.getHead().getFund_pool_no();
			List<TradePacketReqBodyOtherPofitLossLedger> list = tradeRequest.getBody().getProfit_loss_list();
			List<OtherPofitLossLedgerForRefund> otherList = new ArrayList<OtherPofitLossLedgerForRefund>();
			this.otherProfitLossLedgerList = new ArrayList<TradeProfitLossLedger>();
			if (null != list && list.size() > 0) {
				for (TradePacketReqBodyOtherPofitLossLedger ledger : list) {
					TradeProfitLossLedger profitLossLedger = new TradeProfitLossLedger();
					profitLossLedger.setCoreMerchantNo(core_merchant_no);
					profitLossLedger.setFundPoolNo(fund_pool_no);
					profitLossLedger.setMerchantExtendField1(ledger.getMerchant_extend_field_1());
					profitLossLedger.setMerchantExtendField2(ledger.getMerchant_extend_field_2());
					profitLossLedger.setMerchantExtendField3(ledger.getMerchant_extend_field_3());
					profitLossLedger.setTradeType(TradeUtilCommon.getTradeType(tradeRequest.getHead().getRequest_code()));
					profitLossLedger.setTradeNo(this.tradeNo);
					profitLossLedger.setGmtTradeCreated(currentTime);// 交易创建日期
					profitLossLedger.setCreateReqType(tradeRequest.getHead().getRequest_code());// 创建交易请求类型
					profitLossLedger.setCreateReqNo(this.reqNo);// 创建交易请求编号
					profitLossLedger.setOutTradeNoExt(tradeRequest.getBody().getOut_trade_no_ext());// 交易发起方发起请求编号
					profitLossLedger.setOutTradeNo(tradeRequest.getBody().getOut_trade_no());// 交易发起方业务系统订单号
					profitLossLedger.setProfitLossAttr1(ledger.getProfit_loss_attr_1());// 损益属性1
					profitLossLedger.setProfitLossAttr2(ledger.getProfit_loss_attr_2());// 损益属性2
					profitLossLedger.setProfitLossAttr3(ledger.getProfit_loss_attr_3());// 损益属性3
					profitLossLedger.setProfitLossAttr4(ledger.getProfit_loss_attr_4());// 损益属性4
					profitLossLedger.setProfitLossAttr5(ledger.getProfit_loss_attr_5());// 损益属性5
					profitLossLedger.setProfitLossAttr6(ledger.getProfit_loss_attr_6());// 损益属性6
					profitLossLedger.setProfitLossAttr7(ledger.getProfit_loss_attr_7());// 损益属性7
					profitLossLedger.setProfitLossAttr8(ledger.getProfit_loss_attr_8());// 损益属性8
					profitLossLedger.setProfitLossAttr9(ledger.getProfit_loss_attr_9());// 损益属性9
					profitLossLedger.setProfitLossAttr10(ledger.getProfit_loss_attr_10());// 损益属性10
					profitLossLedger.setIncomeIncr(str2Long(ledger.getIncome_incr()));// 损益收入增加金额
					profitLossLedger.setIncomeDecr(str2Long(ledger.getIncome_decr()));// 损益收入减少金额
					profitLossLedger.setCostIncr(str2Long(ledger.getCost_incr()));// 损益成本费用减少金额
					profitLossLedger.setCostDecr(str2Long(ledger.getCost_decr()));// 损益成本费用减少金额
					this.otherProfitLossLedgerNo = profitLossLedgerService.createBlankProfitLossLedger(profitLossLedger);
					this.otherProfitLossLedgerList.add(profitLossLedger);
					// 交易记录其他损益台账
					OtherPofitLossLedgerForRefund other = new OtherPofitLossLedgerForRefund();
					other.setLedger_no(otherProfitLossLedgerNo);
					other.setIncome_incr(ledger.getIncome_incr());
					other.setIncome_decr(ledger.getIncome_decr());
					other.setCost_incr(ledger.getCost_incr());
					other.setCost_decr(ledger.getCost_decr());
					otherList.add(other);
				}
				this.jsonList = ObjectJsonUtil.object2String(otherList);
			}
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("-->创建其他空白损益台账失败：" + e.getMessage(), e);
			throw exceptionService.buildBusinessException("TZ000200201");
		}

	}

	/**
	 * 
	 * @Title:createBlankLegder
	 * @Description:创建空白的付款方外部收款台账
	 * @param tradeRequest
	 * @param currentTime
	 * @return:void
	 * @author:zoran.huang
	 * @date:2016年7月6日 下午4:51:49
	 */
	private void createBlankLegder(TradeRequest<TradePacketReqBodyExternalAsynPayment> tradeRequest, Date currentTime) {
		ledger = new TradeLedger();
		try {
			ledger.setCoreMerchantNo(tradeRequest.getHead().getCore_merchant_no());
			ledger.setFundPoolNo(tradeRequest.getHead().getFund_pool_no());
			ledger.setMerchantExtendField1(tradeRequest.getBody().getLedger_merchant_extend_field_1());
			ledger.setMerchantExtendField2(tradeRequest.getBody().getLedger_merchant_extend_field_2());
			ledger.setMerchantExtendField3(tradeRequest.getBody().getLedger_merchant_extend_field_3());
			ledger.setTradeType(TradeUtilCommon.getTradeType(tradeRequest.getHead().getRequest_code())); // 业务交易类型（例：0006_001）
			ledger.setTradeNo(this.tradeNo); // 业务交易编号
			ledger.setGmtTradeCreated(currentTime); // 业务交易创建日期
			ledger.setCreateReqType(tradeRequest.getHead().getRequest_code());// 创建交易请求类型
			ledger.setCreateReqNo(this.reqNo);// 创建交易请求编号
			ledger.setOutTradeNo(tradeRequest.getBody().getOut_trade_no());// 交易发起方业务系统订单号
			ledger.setOutTradeNoExt(tradeRequest.getBody().getOut_trade_no_ext());// 付款方外部交易发起方发起请求编号
			ledger.setClearChannel(payChannel); // 付款方外部收付款渠道编号 ZF0001_02_002
			ledger.setExtendField1(tradeRequest.getBody().getPay_transaction_id()); // 付款方外部收付款备注1【支付渠道身份编号】
			ledger.setReceiveAmount(strToLong(tradeRequest.getBody().getTotal_amount()));// 付款方外部收款金额
			ledger.setExtendField5(idFactorySinglePaymentSerialNumber.generate().toString());// 付款方外部收付款备注5【提交第三方请求流水号】
			if (TradeConstants.ALI_ASYN_PAY_CHANNEL.equals(payChannel)) {
				ledger.setExtendField2(aliAsynAttachment.getBuyer_logon_id());// 来自输入buyer_logon_id
				ledger.setExtendField6(aliAsynAttachment.getSeller_id());// 来自输入【卖方ID】
			} else if (TradeConstants.WX_ASYN_PAY_CHANNEL.equals(payChannel)) {
				ledger.setExtendField2(wxAsynAttachment.getSub_openid());// 来自输入【用户子标识】
			}
			this.payLedgerNo = legderService.createBlankLegder(ledger);
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("-->创建空白的外部台账失败：" + e.getMessage(), e);
			throw exceptionService.buildBusinessException("TZ000100301");
		}
	}

	@Override
	public PaymentResponse requestPayment() throws Exception {
		logger.info("开始请求支付中心......");
		if (TradeConstants.ALI_ASYN_PAY_CHANNEL.equals(payChannel)) {
			AlipayTradePrecreateResponse response = (AlipayTradePrecreateResponse) paymentInterface.synRequestPayment(aliPayRequest);
			TradeCommonValidation.isPaymentResponseSuccess(response, "JY00060010071001300");
			this.responseXml = ObjectXmlUtil.marshal(response);
			return response;
		} else if (TradeConstants.WX_ASYN_PAY_CHANNEL.equals(payChannel)) {
			WeiXinUnifiedOrderResponse response = (WeiXinUnifiedOrderResponse) paymentInterface.synRequestPayment(wxPayRequest);
			TradeCommonValidation.isPaymentResponseSuccess(response, "JY00060010071001300");
			this.responseXml = ObjectXmlUtil.marshal(response);
			return response;
		}
		return null;
	}

	

	@Override
	public void parseObject(PaymentResponse obj) throws Exception {
		TradeCommonValidation.isPaymentResponseSuccess(obj);
		if (TradeConstants.ALIPAY_PAY_INFO_SUCCESS.equals(obj.getZf_head().getRet_code())) {
			if (TradeConstants.ALI_ASYN_PAY_CHANNEL.equals(payChannel)) {
				AlipayTradePrecreateResponse response = (AlipayTradePrecreateResponse) obj;
				
				/*// 商户订单号	
				response.getOut_trade_no();*/
				
				//预下单链接
				this.pay_url = response.getQr_code();
				return ; 
			} else if (TradeConstants.WX_ASYN_PAY_CHANNEL.equals(payChannel)) {
				WeiXinUnifiedOrderResponse response = (WeiXinUnifiedOrderResponse)obj;
				/*//子商户公众账号ID	
				response.getSub_appid();
				//设备号	
				response.getDevice_info();
				//交易类型	
				response.getTrade_type();
				//预支付交易会话标识	
				response.getPrepay_id();*/
				
				if("NATIVE".equals(response.getTrade_type())){//二维码链接 
					this.pay_url = response.getCode_url();
				}else if("JSAPI".equals(response.getTrade_type())){	//支付链接
					this.pay_url = response.getPay_url();
				}
				
				
				return ;
			}
        }
        throw exceptionService.buildBusinessException("JY00060010071001400");
		
	}

	@Override
	public void createPayFail(TradeRequest<TradePacketReqBodyExternalAsynPayment> tradeRequest) throws Exception {
		TradeSinglePayment singlePayment = new TradeSinglePayment();
		Date currentTime = new Date();
		try {
			/**
			 * 交易公共部分
			 */
			singlePayment.setTradeNo(this.tradeNo);
			singlePayment.setCoreMerchantNo(tradeRequest.getHead().getCore_merchant_no());// 核心商户编号
			singlePayment.setFundPoolNo(tradeRequest.getHead().getFund_pool_no());// 资金池编号
			singlePayment.setPartnerId(tradeRequest.getHead().getPartner_id());
			singlePayment.setMerchantExtendField1(tradeRequest.getBody().getMerchant_extend_field_1());// 交易客户保留字段1
			singlePayment.setMerchantExtendField2(tradeRequest.getBody().getMerchant_extend_field_2());// 交易客户保留字段2
			singlePayment.setMerchantExtendField3(tradeRequest.getBody().getMerchant_extend_field_3());// 交易客户保留字段3
			singlePayment.setMerchantExtendField4(tradeRequest.getBody().getMerchant_extend_field_4());// 交易客户保留字段4
			singlePayment.setMerchantExtendField5(tradeRequest.getBody().getMerchant_extend_field_5());// 交易客户保留字段5
			singlePayment.setMerchantExtendField6(tradeRequest.getBody().getMerchant_extend_field_6());// 交易客户保留字段6
			singlePayment.setMerchantExtendField7(tradeRequest.getBody().getMerchant_extend_field_7());// 交易客户保留字段7
			singlePayment.setMerchantExtendField8(tradeRequest.getBody().getMerchant_extend_field_8());// 交易客户保留字段8
			singlePayment.setMerchantExtendField9(tradeRequest.getBody().getMerchant_extend_field_9());// 交易客户保留字段9
			singlePayment.setMerchantExtendField10(tradeRequest.getBody().getMerchant_extend_field_10());// 交易客户保留字段10
			singlePayment.setOutTradeNoExt(tradeRequest.getBody().getOut_trade_no_ext());// 交易发起方发起请求编号
			singlePayment.setOutTradeNo(tradeRequest.getBody().getOut_trade_no());// 交易发起方业务系统订单号
			singlePayment.setGmtCreated(currentTime);// 交易创建日期时间
			singlePayment.setGmtModifiedLatest(currentTime);// 最后变更日期时间
			singlePayment.setLatestTradeReqType(tradeRequest.getHead().getRequest_code());// 最后变更交易请求类型
			singlePayment.setLatestReqNo(this.reqNo);// 最后变更交易请求编号
			singlePayment.setCloseStatus(TradeConstants.TRADE_CLOSE_STATUS_END);// 交易结束状态
			singlePayment.setStatus(TradeConstants.TRADE_PAYMENT_PAY_FAIL);// 交易状态
			singlePayment.setTradeCatagory(TradeConstants.PAY_TYPE_BY_THIRD);// 交易组别
			singlePayment.setVersion(INIT_VERSION);// 交易版本
			/**
			 * 交易失败时间部分
			 */
			singlePayment.setGmtFailPay(currentTime);// 支付失败日期时间
			/**
			 * 付款方基本部分
			 */
			// 付款方付出金额---账户、第三方充付、损益充付与付款方账户出账金额相同
			if (TradeConstants.PAY_BY_THIRD_ACCOUNT.equals(tradeRequest.getBody().getPay_type())) {
				singlePayment.setPayAmt(strToLong(tradeRequest.getBody().getPay_account_out_accounting_amt()));
				// 付款方付出金额---第三方支付、损益直付与台账付出金额相同
			} else if (TradeConstants.PAY_BY_THIRD.equals(tradeRequest.getBody().getPay_type())) {
				singlePayment.setPayAmt(strToLong(tradeRequest.getBody().getTotal_amount()));
			}
			singlePayment.setPayType(tradeRequest.getBody().getPay_type());// 付款方付出方式
			/**
			 * 付出方外部部分
			 */

			singlePayment.setExternalPayChannel(this.ledger.getClearChannel());// 付款方外部收付款渠道编号
			singlePayment.setExternalPayChannelFeeAmt(str2Long(this.ledger.getClearChannelAttr1()));// 付款方外部收付款属性1（付出手续费金额）
			singlePayment.setExternalPayChannelRefundFeeAmt(str2Long(this.ledger.getClearChannelAttr2()));// 付款方外部收付款属性2（退回手续费金额）
			singlePayment.setExternalPayReceivedAmt(this.ledger.getReceiveAmount());// 付款方外部收款金额
			// singlePayment.setGmtSuccessExternalPayChannel();//
			// 付款方外部渠道收付款成功日期时间
			// singlePayment.setExternalPayChannelTradeNo();// 付款方外部收付款渠道订单号
			singlePayment.setExternalPayChannelBatchNo(this.ledger.getExtendField4());// 付款方外部收付款备注4【提交第三方批次号】
			singlePayment.setExternalPayChannelSerialNo(this.ledger.getExtendField5());// 付款方外部收付款备注5【提交第三方请求流水号】

			/**
			 * 付出方账户部分
			 */
			singlePayment.setPayAccountNo(tradeRequest.getBody().getPay_account_no());// 付款方账户账户编号
			singlePayment.setPayAccountMerchantNo(this.payMerchantNo);// 付款方账户所属商户编号
			singlePayment.setPayAccountOutAccountingAmt(strToLong(tradeRequest.getBody().getPay_account_out_accounting_amt()));// 付款方账户出账金额
			singlePayment.setPayAccountOutAccountingType(TradeConstants.TRADE_ACCONUT_AVAILABLE_TYPE);// 付款方账户出账子账户类型
			singlePayment.setPayAccountOutAccountingHis1(tradeRequest.getBody().getPay_account_out_accounting_his1());// 付款方账户出账账务历史1
			singlePayment.setPayAccountOutAccountingHis2(tradeRequest.getBody().getPay_account_out_accounting_his2());// 付款方账户出账账务历史2
			singlePayment.setPayAccountOutAccountingHis3(tradeRequest.getBody().getPay_account_out_accounting_his3());// 付款方账户出账账务历史3
			singlePayment.setPayAccountInAccountingType(TradeConstants.TRADE_ACCONUT_AVAILABLE_TYPE);// 付款方账户入账子账户类型
			singlePayment.setPayAccountInAccountingHis1(tradeRequest.getBody().getPay_account_in_accounting_his1());// 付款方账户入账账务历史1
			singlePayment.setPayAccountInAccountingHis2(tradeRequest.getBody().getPay_account_in_accounting_his2());// 付款方账户入账账务历史2
			singlePayment.setPayAccountInAccountingHis3(tradeRequest.getBody().getPay_account_in_accounting_his3());// 付款方账户入账账务历史3
			/**
			 * 付款方手续费账户部分
			 */
			singlePayment.setPayFeeAccountNo(tradeRequest.getBody().getPay_fee_account_no());// 付款方手续费账户账户编号
			singlePayment.setPayFeeAccountMerchantNo(this.payFeeMerchantNo);// 付款方手续费账户所属商户编号
			singlePayment.setPayFeeAccountOutAccountingAmt(strToLong(tradeRequest.getBody().getPay_fee_account_out_accounting_amt()));// 付款方手续费账户出账金额
			singlePayment.setPayFeeAccountOutAccountingType(TradeConstants.TRADE_ACCONUT_AVAILABLE_TYPE);// 付款方手续费账户出账子账户类型
			singlePayment.setPayFeeAccountOutAccountingHis1(tradeRequest.getBody().getPay_fee_account_out_accounting_his1());// 付款方手续费账户出账账务历史1
			singlePayment.setPayFeeAccountOutAccountingHis2(tradeRequest.getBody().getPay_fee_account_out_accounting_his2());// 付款方手续费账户出账账务历史2
			singlePayment.setPayFeeAccountOutAccountingHis3(tradeRequest.getBody().getPay_fee_account_out_accounting_his3());// 付款方手续费账户出账账务历史3
			/**
			 * 收到方基本部分
			 */
			// 收款方收到金额
			if (TradeConstants.RECEIVE_BY_THIRD_ACCOUNT.equals(tradeRequest.getBody().getReceive_type())) {// 收款方式为账户
				singlePayment.setReceiveAmt(strToLong(tradeRequest.getBody().getReceive_account_in_accounting_amt()));
			} else if (TradeConstants.RECEIVE_BY_THIRD_PROFIT_LOSS.equals(tradeRequest.getBody().getReceive_type()) && null != tradeRequest.getBody().getReceive_profit_loss_income_incr()) {// 收款方式为损益
				singlePayment.setReceiveAmt(strToLong(tradeRequest.getBody().getReceive_profit_loss_income_incr()));
			} else if (TradeConstants.RECEIVE_BY_THIRD_PROFIT_LOSS.equals(tradeRequest.getBody().getReceive_type()) && null != tradeRequest.getBody().getReceive_profit_loss_cost_decr()) {// 收款方式为损益
				singlePayment.setReceiveAmt(strToLong(tradeRequest.getBody().getReceive_profit_loss_cost_decr()));
			}
			singlePayment.setReceiveType(tradeRequest.getBody().getReceive_type());// 收款方收到方式
			/**
			 * 收到方损益部分
			 */
			singlePayment.setReceiveProfitLossIncomeIncr(str2Long(tradeRequest.getBody().getReceive_profit_loss_income_incr()));// 收款方损益收入增加金额
			singlePayment.setReceiveProfitLossCostDecr(str2Long(tradeRequest.getBody().getReceive_profit_loss_cost_decr()));// //收款方损益成本费用减少金额
			/**
			 * 收到方账户部分
			 */
			singlePayment.setReceiveAccountNo(tradeRequest.getBody().getReceive_account_no());// 收款方账户账户编号
			singlePayment.setReceiveAccountMerchantNo(this.receiveMerchantNo);// 收款方账户所属商户编号
			singlePayment.setReceiveAccountInAccountingAmt(strToLong(tradeRequest.getBody().getReceive_account_in_accounting_amt()));// 收款方账户入账金额
			singlePayment.setReceiveAccountInAccountingType(tradeRequest.getBody().getReceive_account_in_accounting_type());// 收款方账户入账子账户类型
			singlePayment.setReceiveAccountInAccountingHis1(tradeRequest.getBody().getReceive_account_in_accounting_his1());// 收款方账户入账账务历史1
			singlePayment.setReceiveAccountInAccountingHis2(tradeRequest.getBody().getReceive_account_in_accounting_his2());// 收款方账户入账账务历史2
			singlePayment.setReceiveAccountInAccountingHis3(tradeRequest.getBody().getReceive_account_in_accounting_his3());// 收款方账户入账账务历史3
			/**
			 * 收款方手续费账户部分
			 */
			singlePayment.setReceiveFeeAccountNo(tradeRequest.getBody().getReceive_fee_account_no());// 收款方手续费账户账户编号
			singlePayment.setReceiveFeeAccountMerchantNo(this.receiveFeeMerchantNo);// 收款方手续费账户所属商户编号
			singlePayment.setReceiveFeeAccountOutAccountingAmt(strToLong(tradeRequest.getBody().getReceive_fee_account_out_accounting_amt()));// 收款方手续费账户出账金额
			singlePayment.setReceiveFeeAccountOutAccountingType(tradeRequest.getBody().getReceive_fee_account_out_accounting_type());// 收款方手续费账户出账子账户类型
			singlePayment.setReceiveFeeAccountOutAccountingHis1(tradeRequest.getBody().getReceive_fee_account_out_accounting_his1());// 收款方手续费账户出账账务历史1
			singlePayment.setReceiveFeeAccountOutAccountingHis2(tradeRequest.getBody().getReceive_fee_account_out_accounting_his2());// 收款方手续费账户出账账务历史2
			singlePayment.setReceiveFeeAccountOutAccountingHis3(tradeRequest.getBody().getReceive_fee_account_out_accounting_his3());// 收款方手续费账户出账账务历史3
			/**
			 * 其他损益台账List部分
			 */
			singlePayment.setExtendProfitLoss(this.jsonList);
			/**
			 * 累计金额部分
			 */
			singlePayment.setContinueTradeStatus(TradeConstants.CONTINUE_TRADE_STATUS_END);// 后续交易开关-关
			this.tradeSinglePayment = singlePaymentService.createOuterTradeFailureRecode(singlePayment);
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("-->创建支付失败交易记录失败：" + e.getMessage(), e);
			throw exceptionService.buildBusinessException("JY00060010071001100");
		}
	}

	@Override
	public String createPaymentRequest(TradeRequest<TradePacketReqBodyExternalAsynPayment> tradeRquest) throws Exception {
		logger.info("开始组装请求支付中心数据......");
		if (TradeConstants.ALI_ASYN_PAY_CHANNEL.equals(payChannel)) {
			aliPayRequest = initAlipayTradePayRequest(tradeRquest);
			// AlipayTradePrecreateResponse response =
			// (AlipayTradePrecreateResponse)paymentInterface.synRequestPayment(payRequest);
			return ObjectXmlUtil.marshal(aliPayRequest);
		} else if (TradeConstants.WX_ASYN_PAY_CHANNEL.equals(payChannel)) {
			wxPayRequest = initWXpayTradePayRequest(tradeRquest);
			// WeiXinUnifiedOrderResponse response =
			// (WeiXinUnifiedOrderResponse)paymentInterface.synRequestPayment(payRequest);
			return ObjectXmlUtil.marshal(wxPayRequest);
		}
		return null;
	}

	private WeiXinUnifiedOrderRequest initWXpayTradePayRequest(TradeRequest<TradePacketReqBodyExternalAsynPayment> tradeRquest) {
		WeiXinUnifiedOrderRequest request = new WeiXinUnifiedOrderRequest();
		PaymentHead head = setBasicParam();
		request.setZf_head(head);
		// 子商户公众账号ID
		request.setSub_appid(wxAsynAttachment.getSub_appid());
		// 设备号
		request.setDevice_info(wxAsynAttachment.getDevice_info());
		// 商品描述
		request.setBody(tradeRquest.getBody().getSubject());
		// 商品详情
		request.setDetail(tradeRquest.getBody().getBody());
		// 附加数据
		request.setAttach(wxAsynAttachment.getAttach());
		// 商户订单号
		request.setOut_trade_no(ledger.getExtendField5());
		// 货币类型
		request.setFee_type(wxAsynAttachment.getFee_type());
		// 总金额
		request.setTotal_fee(AmountUtils.bizYuan2Fen(tradeRquest.getBody().getTotal_amount()));
		// 终端IP
		request.setSpbill_create_ip(wxAsynAttachment.getSpbill_create_ip());
		// 交易起始时间
		request.setTime_start(wxAsynAttachment.getTime_start());
		// 交易结束时间
		request.setTime_expire(wxAsynAttachment.getTime_expire());
		// 商品标记
		request.setGoods_tag(wxAsynAttachment.getGoods_tag());
		// 通知地址
		request.setNotify_url("0006_01_008:1.0");
		// 交易类型
		request.setTrade_type(wxAsynAttachment.getTrade_type());
		// 商品ID
		request.setProduct_id(wxAsynAttachment.getProduct_id());
		// 指定支付方式
		request.setLimit_pay(wxAsynAttachment.getLimit_pay());
		// 用户标识
		// request.setOpenid(wxAsynAttachment.getopenid);
		// 用户子标识
		request.setSub_openid(wxAsynAttachment.getSub_openid());
		return request;
	}

	/**
	 * @Title:initAlipayTradePayRequest
	 * @Description: 组装支付中心扫码付参数
	 * @param tradeRquest
	 * @param payRequest
	 * @return
	 * @throws Exception
	 * @return:AlipayTradePrecreateRequest
	 * @author:zoran.huang
	 * @date:2016年7月7日 下午1:01:17
	 */
	private AlipayTradePrecreateRequest initAlipayTradePayRequest(TradeRequest<TradePacketReqBodyExternalAsynPayment> tradeRquest) throws Exception {
		AlipayTradePrecreateRequest request = new AlipayTradePrecreateRequest();
		request.setNotify_url("0006_01_008:1.0");
		PaymentHead head = setBasicParam();
		request.setZf_head(head);
		// 支付宝
		head.setApp_auth_token(aliAsynAttachment.getApp_auth_token());
		// 商户订单号
		request.setOut_trade_no(ledger.getExtendField5());
		// 卖方支付宝用户ID
		// 订单总金额
		request.setTotal_amount(tradeRquest.getBody().getTotal_amount());
		// 可打折金额
		request.setDiscountable_amount("0");
		// 不可打折金额
		request.setUndiscountable_amount(tradeRquest.getBody().getTotal_amount());
		// 买家支付宝账号
		request.setBuyer_logon_id(aliAsynAttachment.getBuyer_logon_id());
		// 订单标题
		request.setSubject(tradeRquest.getBody().getSubject());
		// 订单描述
		request.setBody(tradeRquest.getBody().getBody());
		// 订单包含的商品列表信息
		if(aliAsynAttachment.getGoods_detail() != null && aliAsynAttachment.getGoods_detail().size() > 0){
			List<GoodsDetail> goodsDetailList = Lists.newArrayList();
			for(Goods_Detail  goods_Detail: aliAsynAttachment.getGoods_detail()){
				GoodsDetail  goodsDetail = new GoodsDetail();
				BeanUtils.copyProperties(goods_Detail, goodsDetail);
				goodsDetailList.add(goodsDetail);
			}
			request.setGoods_detail(goodsDetailList);
		}
		// 商户操作员编号
		request.setOperator_id(aliAsynAttachment.getOperator_id());
		// 商户门店编号

		request.setStore_id(aliAsynAttachment.getStore_id());
		// 商户机具终端编号
		request.setTerminal_id(aliAsynAttachment.getTerminal_id());
		// 业务扩展参数
		if(aliAsynAttachment.getExtend_params() != null ){
			ExtendParams  extendParams = new ExtendParams();
			BeanUtils.copyProperties(aliAsynAttachment.getExtend_params(), extendParams);
			request.setExtend_params(extendParams);
		}
		
		// 该笔订单允许的最晚付款时间
		request.setTimeout_express(aliAsynAttachment.getTimeout_express());
		return request;
	}

	public PaymentHead setBasicParam() {
		PaymentHead head = new PaymentHead();
		// 交易编号
		head.setTrade_no(ledger.getTradeNo());
		// 核心商户编号
		head.setCore_merchant_no(ledger.getCoreMerchantNo());
		// 外部收付款台账ID
		head.setLedger_no(ledger.getLedgerNo());
		// 交易请求编号
		head.setTrade_req_no(this.reqNo);
		// 交易发起方业务系统订单号
		head.setOut_trade_no(ledger.getOutTradeNo());
		// 版本号
		head.setVersion("1.0");
		// 渠道编号
		head.setPay_channel(ledger.getClearChannel());
		// 渠道身份编号
		head.setPay_transaction_id(ledger.getExtendField1());
		return head;
	}

	@Override
	public void setReqNo(String reqNo) {
		this.reqNo = reqNo;
	}

	@Override
	public Object getTradeDetails() {
		TradeSinglePaymentVo paymentVo = new TradeSinglePaymentVo();
        if (tradeSinglePayment != null) {
            BeanUtils.copyProperties(tradeSinglePayment, paymentVo);
            return paymentVo;
        }
        return null;
	}

	@Override
	public String getPaymentResult(Object obj) {
		return ObjectXmlUtil.marshal(obj);
	}

	@Override
	public void updateTradeRecord(TradeRequest<TradePacketReqBodyExternalAsynPayment> tradeRquest) throws Exception {
		try {
			TradeSinglePayment updatePayment = new TradeSinglePayment();
			updatePayment.setTradeNo(this.tradeSinglePayment.getTradeNo());
			updatePayment.setId(this.tradeSinglePayment.getId());
			updatePayment.setVersion(this.tradeSinglePayment.getVersion());
			//最后变更交易请求类型	本次请求类型
			updatePayment.setLatestTradeReqType(tradeRquest.getHead().getRequest_code());
			//最后变更交易请求编号	来自交易请求记录
			updatePayment.setLatestReqNo(this.reqNo);
			//付款方同步支付链接	来自支付中心返回数据
			updatePayment.setPayUrl(this.pay_url);
			this.tradeSinglePayment = singlePaymentService.updateTrade(updatePayment);
		} catch (Exception e) {
			 e.printStackTrace();
			 logger.error("-->更新预下单链接失败:"+e.getMessage(),e);
	         throw exceptionService.buildBusinessException("JY00060010071001200");
		}
	}

	@Override
	public void updateTradeToFailure(TradeRequest<TradePacketReqBodyExternalAsynPayment> tradeRquest) {
		//do nothing
	}

	@Override
	public String getResponseXml() {
		return this.responseXml;
	}

	/**
	 * @Description: 将字符串转换为数字
	 */
	private long strToLong(String str) {
		long parseNum = 0;
		if (!StringUtils.isEmpty(str)) {
			try {
				parseNum = AmountUtils.bizAmountConvertToLong(str);
			} catch (AmountConvertException e) {
				e.printStackTrace();
				logger.error("-->金额转换异常，输入金额：" + str, e);
				throw exceptionService.buildBusinessException("JY000000000000201");
			}
		}
		return parseNum;
	}

	private Long str2Long(String str) {
		if (str != null) {
			try {
				return AmountUtils.bizAmountConvertToLong(str);
			} catch (AmountConvertException e) {
				e.printStackTrace();
				logger.error("-->金额转换异常，输入金额：" + str, e);
				throw exceptionService.buildBusinessException("JY000000000000201");
			}
		}
		return null;
	}

	/**
	 * @Description: 金额比较
	 */
	private boolean compareAmt(String receiveFeeAccountOutAccountingAmt, String receiveAccountInAccountingAmt) {
		long receive_fee_amt = strToLong(receiveFeeAccountOutAccountingAmt);
		long receive_account_amt = strToLong(receiveAccountInAccountingAmt);
		if (receive_fee_amt <= receive_account_amt) {
			return true;
		}
		return false;
	}

	/**
	 * @param tradeRequest
	 * @throws Exception
	 * @Title:checkDuplicate
	 * @Description: 根据资金池标号和 交易发起方发起请求编号，检查交易发起方发起请求编号是否重复
	 * @return:void
	 */
	protected void checkDuplicate(TradeRequest<TradePacketReqBodyExternalAsynPayment> tradeRequest) throws Exception {
		TradeSinglePaymentExample tradeRechargeExample = new TradeSinglePaymentExample();
		tradeRechargeExample.createCriteria().andFundPoolNoEqualTo(tradeRequest.getHead().getFund_pool_no()).andOutTradeNoExtEqualTo(tradeRequest.getBody().getOut_trade_no_ext());
		List<TradeSinglePayment> list = singlePaymentService.queryByExample(tradeRechargeExample);
		if (!CollectionUtils.isEmpty(list)) {
			logger.info("-->交易发起方发起请求编号重复");
			throw exceptionService.buildBusinessException("JY00060010071000400");
		}
	}

	/**
	 * 验证【核心商户编号】、【资金池编号】与【CA账户编号】对应关系
	 *
	 * @param //coreMerchantNo 核心商户编号
	 * @param //fundPoolNo 资金池编号
	 * @param accountNo
	 *            CA账户编号
	 * @Title:correspond
	 * @Description:验证【核心商户编号】、【资金池编号】与【CA账户编号】对应关系
	 * @return:void
	 * @author:zoran.huang
	 * @date:2016年5月6日 上午10:12:10
	 */
	private AdvanceAccountVO getCreditAccountInfo(String accountNo) {
		AdvanceAccountVO accountVo = advanceAccountService.selectByAccountNo(accountNo);
		if (accountVo != null) {
			return accountVo;
		}
		return null;
	}
}
