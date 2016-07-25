package com.liantuo.trade.bus.process.impl.single_payment_refund;

import com.alibaba.druid.util.StringUtils;
import com.liantuo.deposit.advanceaccount.bus.vo.RealTimeAccountingRsp;
import com.liantuo.deposit.advanceaccount.bus.vo.RealTimeAccountingVo;
import com.liantuo.trade.bus.process.TradeCreateSingleTxNoPaymentInterface;
import com.liantuo.trade.bus.service.BizAccountService;
import com.liantuo.trade.bus.service.ExceptionService;
import com.liantuo.trade.bus.service.SinglePaymentRefundService;
import com.liantuo.trade.bus.service.SinglePaymentService;
import com.liantuo.trade.bus.service.impl.ProfitLossLedgerServiceImpl;
import com.liantuo.trade.bus.template.impl.v1_1.create.ATradeCreateSingleTxNoPaymentTemp;
import com.liantuo.trade.bus.vo.RealTimeAccountVO;
import com.liantuo.trade.client.trade.packet.TradeRequest;
import com.liantuo.trade.client.trade.packet.body.TradePacketReqBodyOtherPofitLossLedger;
import com.liantuo.trade.client.trade.packet.body.single_payment_refund.OtherPofitLossLedgerForRefund;
import com.liantuo.trade.client.trade.packet.body.single_payment_refund.TradePacketReqBodyInnerRefund;
import com.liantuo.trade.common.constants.TradeConstants;
import com.liantuo.trade.common.util.amount.AmountUtils;
import com.liantuo.trade.common.util.json.ObjectJsonUtil;
import com.liantuo.trade.common.util.trade.TradeCommonValidation;
import com.liantuo.trade.common.util.trade.TradeUtilCommon;
import com.liantuo.trade.common.validate.TradeValidationUtil;
import com.liantuo.trade.exception.AmountConvertException;
import com.liantuo.trade.exception.BusinessException;
import com.liantuo.trade.orm.pojo.*;
import com.liantuo.trade.orm.pojo.TradeSinglePaymentRefundExample.Criteria;
import com.liantuo.trade.orm.pojo.subvo.TradeSinglePaymentRefundVo;
import com.liantuo.trade.seqno.IdFactory;
import com.liantuo.trade.spring.annotation.JobFlow;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Vector;

/**
 *0006——002——001 内部支付退款
 * @author yangting
 *2016年6月6日 上午9:51:38
 */
@JobFlow(value = "0006_002_001", version = "1.0", template = ATradeCreateSingleTxNoPaymentTemp.class)
public class SinglePaymentRefundInnerProcess implements TradeCreateSingleTxNoPaymentInterface<TradePacketReqBodyInnerRefund> {

	private final Log LOGGER = LogFactory.getLog(SinglePaymentRefundInnerProcess.class);
	
	@Resource(name="singlePaymentServiceImpl")
	private SinglePaymentService paymentService;//原交易Service
	
	@Resource(name="singlePaymentRefundServiceImpl")
	private SinglePaymentRefundService refundService;//退款交易Service
	
	@Resource(name = "idFactorySinglePaymentRefundTradeNo")
	private IdFactory idFactorySinglePaymentRefundTradeNo;//序列号生成器
	
	@Resource(name = "bizAccountServiceImpl")
    private BizAccountService bizAccountService;//账务Service
	
	@Resource
    protected ExceptionService exceptionService;//异常Service
	
	//损益台账service
    @Resource(name = "profitLossLedgerServiceImpl")
    private ProfitLossLedgerServiceImpl profitLossLedgerServiceImpl;
	
	private TradeSinglePaymentRefund paymentRefund;//创建退款对象
	
	private TradeSinglePayment originalTradeSinglePayment;//原交易对象
	
	private String reqNo;
	
	private String tradeNo ;//本次交易编号
	private Date gmtCreated;//本次交易创建时间
	
	private String refundReceivedLossLedgerNo;//收款损益退回台账编号
	
	private boolean ca_in_account_flag = false;//账户入账标识
	
	private String jsonList = "";//其他损益台账List Json

	private String ledger_no_effe_pay_pr_refund;//生效付款损益退回台账
	
//	private long refund_pay_effe_ledger_amt;//付款方损益退回收到金额

	@Override
	public void transaction(TradeRequest<TradePacketReqBodyInnerRefund> tradeRquest) throws Exception {
		this.tradeNo = idFactorySinglePaymentRefundTradeNo.generate().toString();
		this.gmtCreated = new Date();
		
		TradePacketReqBodyInnerRefund body = tradeRquest.getBody();
		//1	创建生效收款损益退回台账	原交易【收款方收到方式】为【损益】且【收款方退回付出金额】不为0时创建
		String receiveType = this.originalTradeSinglePayment.getReceiveType();
		long refund_receive_amt = str2Long(body.getRefund_receive_amt());
		if(TradeConstants.RECEIVE_BY_THIRD_PROFIT_LOSS.equals(receiveType)&&refund_receive_amt>0){
			createEffecitveRecRefundLeger(tradeRquest);
		}
		//2	账务退回处理
		RefundAcctHandler(tradeRquest);
		//3	创建生效其他损益退回台账
		createEffecitveRefundLegerList(tradeRquest);
		//4	创建生效付款损益退回台账
		long refund_pay_profit_loss_amt = str2Long(tradeRquest.getBody().getRefund_pay_profit_loss_amt());
		String refund_pay_type = tradeRquest.getBody().getRefund_pay_type();
		if((TradeConstants.TRADE_REFUND_PAY_LOSS.equals(refund_pay_type)||TradeConstants.TRADE_REFUND_PAY_LOSS_ACCOUNT.equals(refund_pay_type))&&refund_pay_profit_loss_amt>0){
			createEffecitvePayRefundLeger(tradeRquest);
		}
		//5	修改原支付交易记录
		updateOriginalTradeRecord(tradeRquest);
		//6	创建退款成功交易记录
		createRefundSuccRecord(tradeRquest);
		this.ledger_no_effe_pay_pr_refund=null;
		ca_in_account_flag = false;
	}
	
	
	private void createEffecitveRecRefundLeger(TradeRequest<TradePacketReqBodyInnerRefund> tradeRquest) {
		TradeProfitLossLedger ledger = new TradeProfitLossLedger();
		try {
			//收款方损益退回核心商户编号	来自输入
            ledger.setCoreMerchantNo(tradeRquest.getHead().getCore_merchant_no());
            //收款方损益退回资金池编号	来自输入
            ledger.setFundPoolNo(tradeRquest.getHead().getFund_pool_no());
            //收款方损益退回业务台账客户保留字段1	来自输入
            ledger.setMerchantExtendField1(tradeRquest.getBody().getRefund_receive_profit_loss_extend_field_1());
            //收款方损益退回业务台账客户保留字段2	来自输入
            ledger.setMerchantExtendField2(tradeRquest.getBody().getRefund_receive_profit_loss_extend_field_2());
			//收款方损益退回业务台账客户保留字段3	来自输入
            ledger.setMerchantExtendField3(tradeRquest.getBody().getRefund_receive_profit_loss_extend_field_3());
            //收款方损益退回所属业务交易类型	来自交易
            ledger.setTradeType(TradeConstants.TRADE_REFUND);
            //收款方损益退回所属业务交易编号	来自交易
            ledger.setTradeNo(this.tradeNo);
            //收款方损益退回所属业务交易创建日期 来自交易
            ledger.setGmtTradeCreated(this.gmtCreated);
            //收款方损益退回创建交易请求类型	来自交易
            ledger.setCreateReqType(TradeConstants.TRADE_INNER_PR);
            //收款方损益退回创建交易请求编号	来自交易
            ledger.setCreateReqNo(this.reqNo);
            //收款方损益退回生效交易请求类型	来自交易
            ledger.setEffectiveReqType(TradeConstants.TRADE_INNER_PR);
            //收款方损益退回生效交易请求编号	来自交易
            ledger.setEffectiveReqNo(this.reqNo);
            //收款方损益退回交易发起方发起请求编号	来自输入
            ledger.setOutTradeNoExt(tradeRquest.getBody().getOut_trade_no_ext());
			//收款方损益退回交易发起方业务系统订单号	来自输入
            ledger.setOutTradeNo(tradeRquest.getBody().getOut_trade_no());
            
            TradeProfitLossLedger lossledger =profitLossLedgerServiceImpl.queryByLedgerNoAndTradeNo(originalTradeSinglePayment.getReceiveProfitLossLedgerNo(), originalTradeSinglePayment.getTradeNo()) ;
			if(lossledger == null ){
				throw exceptionService.buildBusinessException("TZ000200101");
			}
            //收款方损益退回损益属性1	来自原交易【收款方损益台账】对应属性
            ledger.setProfitLossAttr1(lossledger.getProfitLossAttr1());
			//收款方损益退回损益属性2	来自原交易【收款方损益台账】对应属性
            ledger.setProfitLossAttr2(lossledger.getProfitLossAttr2());
			//收款方损益退回损益属性3	来自原交易【收款方损益台账】对应属性
            ledger.setProfitLossAttr3(lossledger.getProfitLossAttr3());
			//收款方损益退回损益属性4	来自原交易【收款方损益台账】对应属性
            ledger.setProfitLossAttr4(lossledger.getProfitLossAttr4());
			//收款方损益退回损益属性5	来自原交易【收款方损益台账】对应属性
            ledger.setProfitLossAttr5(lossledger.getProfitLossAttr5());
			//收款方损益退回损益属性6	来自原交易【收款方损益台账】对应属性
            ledger.setProfitLossAttr6(lossledger.getProfitLossAttr6());
			//收款方损益退回损益属性7	来自原交易【收款方损益台账】对应属性
            ledger.setProfitLossAttr7(lossledger.getProfitLossAttr7());
			//收款方损益退回损益属性8	来自原交易【收款方损益台账】对应属性
            ledger.setProfitLossAttr8(lossledger.getProfitLossAttr8());
			//收款方损益退回损益属性9	来自原交易【收款方损益台账】对应属性
            ledger.setProfitLossAttr9(lossledger.getProfitLossAttr9());
			//收款方损益退回损益属性10	来自原交易【收款方损益台账】对应属性
            ledger.setProfitLossAttr10(lossledger.getProfitLossAttr10());
			
            if(originalTradeSinglePayment.getReceiveProfitLossIncomeIncr() != null && originalTradeSinglePayment.getReceiveProfitLossIncomeIncr() != 0 ){
            	//收款方损益退回收入减少金额	若原交易为【收款方损益台账】为【收入增加】时，来自输入【收款方退回付出金额】
            	ledger.setIncomeDecr(str2Long(tradeRquest.getBody().getRefund_receive_amt()));
            }else if(originalTradeSinglePayment.getReceiveProfitLossCostDecr() != null &&originalTradeSinglePayment.getReceiveProfitLossCostDecr() != 0  ){
            	//收款方损益退回成本费用增加金额	若原交易为【收款方损益台账】为【成本费用减少】时，来自输入【收款方退回付出金额】
                ledger.setCostIncr(str2Long(tradeRquest.getBody().getRefund_receive_amt()));
            }
            this.refundReceivedLossLedgerNo = profitLossLedgerServiceImpl.createEffectiveProfitLossLedger(ledger);
		} catch (Exception e) {
			e.printStackTrace();
			LOGGER.error("-->创建生效收款损益退回台账失败："+e.getMessage(),e);
			throw exceptionService.buildBusinessException("TZ000200101");
		}
	}

	private void RefundAcctHandler(TradeRequest<TradePacketReqBodyInnerRefund> tradeRquest) throws Exception {
		List<RealTimeAccountingVo> allList = new ArrayList<RealTimeAccountingVo>();
		//1	原收款方手续费账户退回
		RealTimeAccountingVo recFeeAccountVo = receiveFeeAccountRefund(tradeRquest);
		allList.add(recFeeAccountVo);
		//2	原收款方账户退回
		List<RealTimeAccountingVo> list=receiveAccountRefund(tradeRquest);
		if(!CollectionUtils.isEmpty(list)){
			allList.addAll(list);
		}
		//3.原付款方手续费账户退回
		long  refundPayFeeAmt = str2Long(tradeRquest.getBody().getRefund_pay_fee_account_in_accounting_amt());
		if(refundPayFeeAmt > 0 ){
			RealTimeAccountingVo payFeeAccountVo =payAccountRefundInAccounting(tradeRquest,1,refundPayFeeAmt);
			String pfih1 = tradeRquest.getBody().getRefund_pay_fee_account_in_accounting_his1();
			String pfih2 = tradeRquest.getBody().getRefund_pay_fee_account_in_accounting_his2();
			String pfih3 = tradeRquest.getBody().getRefund_pay_fee_account_in_accounting_his3();
			payFeeAccountVo.setReservedFields1(pfih1);
			payFeeAccountVo.setReservedFields2(pfih2);
			payFeeAccountVo.setReservedFields3(pfih3);
			//来自原交易【付款方手续费账户账户编号】
			String payFeeAccountNo = originalTradeSinglePayment.getPayFeeAccountNo();
			payFeeAccountVo.setAccountNo(payFeeAccountNo);
			allList.add(payFeeAccountVo);
		}
		//4.原付款方账户入账
		//付款方账户退回入账金额 refund_pay_account_in_accounting_amt
		long payAccountInAccountingAmt = str2Long(tradeRquest.getBody().getRefund_pay_account_in_accounting_amt());
		//付款方退回收到方式
		String refundPayType = tradeRquest.getBody().getRefund_pay_type();
		//付款方退回收到退回方式为【账户】、【损益充退】且【付款方账户退回入账金额】不为空且不为0时执行：
		if((TradeConstants.TRADE_REFUND_PAY_ACCOUNT.equals(refundPayType)
			||TradeConstants.TRADE_REFUND_PAY_LOSS_ACCOUNT.equals(refundPayType)||TradeConstants.TRADE_REFUND_PAY_THIRD_ACCOUNT.equals(refundPayType)) && payAccountInAccountingAmt > 0){
			RealTimeAccountingVo payAccountInnerVo  = payAccountRefundInAccounting(tradeRquest,1,payAccountInAccountingAmt);
			String refund_pay_account_in_accounting_his1 = tradeRquest.getBody().getRefund_pay_account_in_accounting_his1();
			String refund_pay_account_in_accounting_his2 = tradeRquest.getBody().getRefund_pay_account_in_accounting_his2();
			String refund_pay_account_in_accounting_his3 = tradeRquest.getBody().getRefund_pay_account_in_accounting_his3();
			payAccountInnerVo.setReservedFields1(refund_pay_account_in_accounting_his1);
			payAccountInnerVo.setReservedFields2(refund_pay_account_in_accounting_his2);
			payAccountInnerVo.setReservedFields3(refund_pay_account_in_accounting_his3);
			allList.add(payAccountInnerVo);
			ca_in_account_flag = true;
		}
		//5	原付款方账户充退出账
		long refund_pay_profit_loss_amt = str2Long(tradeRquest.getBody().getRefund_pay_profit_loss_amt());
		if(TradeConstants.TRADE_REFUND_PAY_LOSS_ACCOUNT.equals(refundPayType) && refund_pay_profit_loss_amt > 0){
			RealTimeAccountingVo payAccountPRInnerVo  = payAccountRefundInAccounting(tradeRquest,2,refund_pay_profit_loss_amt);
			String refund_pay_account_out_accounting_his1 = tradeRquest.getBody().getRefund_pay_account_out_accounting_his1();
			String refund_pay_account_out_accounting_his2 = tradeRquest.getBody().getRefund_pay_account_out_accounting_his2();
			String refund_pay_account_out_accounting_his3 = tradeRquest.getBody().getRefund_pay_account_out_accounting_his3();
			payAccountPRInnerVo.setReservedFields1(refund_pay_account_out_accounting_his1);
			payAccountPRInnerVo.setReservedFields2(refund_pay_account_out_accounting_his2);
			payAccountPRInnerVo.setReservedFields3(refund_pay_account_out_accounting_his3);
			allList.add(payAccountPRInnerVo);
		}
		try {
			Collection nuCon = new Vector(); 
			nuCon.add(null); 
			//去除allList中所有null
			allList.removeAll(nuCon);
            List<RealTimeAccountingRsp> rsp_list = this.bizAccountService.senderRequestToAccount(allList);
            RealTimeAccountingRsp rsp;
            for (RealTimeAccountingRsp aList : rsp_list) {
                rsp = aList;
                if ("F".equals(rsp.getSuccess())) {
                    throw new BusinessException(rsp.getRetCode(), rsp.getErrMessage());
                }
            }
        } catch (BusinessException e) {
            throw new BusinessException(e.getErrorCode(), e.getErrorMessage());
        } catch (Exception e) {
        	e.printStackTrace();
            throw exceptionService.buildBusinessException("JY000000000000401");
        }
	}
	
	/**
	 * 通用账户处理方法
	 * @param tradeRquest
	 * @param type	1 可用余额增加，2可用余额减少，3 冻结余额增加，4 冻结余额减少
	 * @param money	钱
	 * @return
	 * @throws Exception
	 */
	private RealTimeAccountingVo payAccountRefundInAccounting(TradeRequest<TradePacketReqBodyInnerRefund> tradeRquest,int type,long money) throws Exception{
		RealTimeAccountVO accountVo = new RealTimeAccountVO();
		//目标账户编号	来自原交易【收款方手续费账户账户编号】
		accountVo.setAccountNo(originalTradeSinglePayment.getPayAccountNo());
		//核心商户编号	来自原交易【核心商户编号】
		accountVo.setCoreMerchantNo(originalTradeSinglePayment.getCoreMerchantNo());
		//资金池编号	来自原交易【资金池编号】
		accountVo.setPoolNo(originalTradeSinglePayment.getFundPoolNo());
		//用户账务历史保留字段1	来自输入【收款方手续费退回账户入账账务历史1】
		accountVo.setReservedFields1(tradeRquest.getBody().getRefund_pay_account_in_accounting_his1());
		//用户账务历史保留字段2	来自输入【收款方手续费退回账户入账账务历史2】
		accountVo.setReservedFields2(tradeRquest.getBody().getRefund_pay_account_in_accounting_his2());
		//用户账务历史保留字段3	来自输入【收款方手续费退回账户入账账务历史3】
		accountVo.setReservedFields3(tradeRquest.getBody().getRefund_pay_account_in_accounting_his3());
		//所属业务交易类型	本交易类型
		accountVo.setTradeCode(TradeUtilCommon.getTradeType(tradeRquest.getHead().getRequest_code())); // 交易类型0006_002
		//所属业务交易编号	本交易编号
		accountVo.setTradeNo(this.tradeNo);
		//所属业务交易创建日期	本交易创建日期
		accountVo.setTradeGmtCreated(this.gmtCreated);
		//所属业务交易请求类型	本交易请求类型
		accountVo.setTradeReqCode(TradeConstants.TRADE_INNER_PR); // 交易请求类型0006_002_001
		//所属业务交易请求编号	本交易请求编号
		accountVo.setTradeStepNo(this.reqNo);// 
		//所属业务业务系统订单号	本交易业务系统订单号
		accountVo.setSequenceNo(tradeRquest.getBody().getOut_trade_no()); // 本交易业务系统订单号
		accountVo.setAmount(money);
		switch (type) {
		case 1:
			return bizAccountService.avlBalIncrWrapper(accountVo);
		case 2:
			return bizAccountService.avlBalDecrWrapper(accountVo);
		case 3:
			return bizAccountService.FrozenBalIncrAmtWrapper(accountVo);
		case 4:
			return bizAccountService.FrozenBalDecrAmtWrapper(accountVo);
		} 
		return  null ;
		
	}

	private List<RealTimeAccountingVo> receiveAccountRefund(TradeRequest<TradePacketReqBodyInnerRefund> tradeRquest) {
		List<RealTimeAccountingVo> accountingVoList = new ArrayList<RealTimeAccountingVo>();
		long refund_receive_amt = str2Long(tradeRquest.getBody().getRefund_receive_amt());
		//收款方手续费账户退回入账金额
		long refundReceivefeeAmt =  0l;
		//原交易中当前【收到方冻结余额】	
		long receiveFreezeBal = AmountUtils.ifNullOrElse(originalTradeSinglePayment.getReceiveFreezeBal());
		//如果原交易为冻结出账 则存在	收款方手续费账户退回入账金额
		if(TradeConstants.TRADE_ACCOUNT_FREEZE_TYPE.equals(originalTradeSinglePayment.getReceiveFeeAccountOutAccountingType())){
			refundReceivefeeAmt = str2Long(tradeRquest.getBody().getRefund_receive_fee_account_in_accounting_amt());
		}
//		String refund_receive_fee_account_in_accounting_his1 = tradeRquest.getBody().getRefund_receive_fee_account_in_accounting_his1();
//		String refund_receive_fee_account_in_accounting_his2 = tradeRquest.getBody().getRefund_receive_fee_account_in_accounting_his2();
//		String refund_receive_fee_account_in_accounting_his3 = tradeRquest.getBody().getRefund_receive_fee_account_in_accounting_his3();
		//原交易【收款方收到方式】为【账户】且【收款方退回付出金额】不为0时处理。
		if(TradeConstants.RECEIVE_BY_THIRD_ACCOUNT.equals(originalTradeSinglePayment.getReceiveType()) && refund_receive_amt > 0 ){
			//若原交易【收款方账户入账子账户类型】为【冻结】，优先退回冻结金额，若冻结金额不足，可用余额补充
			if(TradeConstants.TRADE_ACCOUNT_FREEZE_TYPE.equals(originalTradeSinglePayment.getReceiveAccountInAccountingType())){
				//若【收款方退回付出金额】=<原交易中当前【收到方冻结余额】+冻结退回的【收款方手续费账户退回入账金额】
				if(refund_receive_amt <= (receiveFreezeBal+refundReceivefeeAmt)){
					RealTimeAccountVO accountVo = initRealTimeAccountVO(tradeRquest,refund_receive_amt);
//					accountVo.setReservedFields1(refund_receive_fee_account_in_accounting_his1);
//					accountVo.setReservedFields2(refund_receive_fee_account_in_accounting_his2);
//					accountVo.setReservedFields3(refund_receive_fee_account_in_accounting_his3);
					//所属业务冻结交易类型	来自原交易类型
					accountVo.setFrozenTradeCode(TradeUtilCommon.getTradeType(originalTradeSinglePayment.getLatestTradeReqType()));
					//所属业务冻结交易编号	来自原交易编号
					accountVo.setFrozenTradeNo(originalTradeSinglePayment.getTradeNo());
					// 调用 冻结余额减少金额
					RealTimeAccountingVo accountingVo =  bizAccountService.FrozenBalDecrAmtWrapper(accountVo);
					accountingVoList.add(accountingVo);
					return accountingVoList ;
				}else if(refund_receive_amt > (receiveFreezeBal+refundReceivefeeAmt)){
					if((receiveFreezeBal+refundReceivefeeAmt) == 0 ){//原交易中当前【收到方冻结余额】+【收款方手续费账户退回入账金额】=0
						RealTimeAccountVO accountVo = initRealTimeAccountVO(tradeRquest,refund_receive_amt);
//						accountVo.setReservedFields1(refund_receive_fee_account_in_accounting_his1);
//						accountVo.setReservedFields2(refund_receive_fee_account_in_accounting_his2);
//						accountVo.setReservedFields3(refund_receive_fee_account_in_accounting_his3);
						// 调用 可用余额减少金额
						RealTimeAccountingVo accountingVo =  bizAccountService.avlBalDecrWrapper(accountVo);
						accountingVoList.add(accountingVo);
						return accountingVoList ;
					}else{//原交易中当前【收到方冻结余额】+【收款方手续费账户退回入账金额】!=0
						//1.先退冻结
						RealTimeAccountVO accountFrozenVo = initRealTimeAccountVO(tradeRquest,receiveFreezeBal+refundReceivefeeAmt);
//						accountFrozenVo.setReservedFields1(refund_receive_fee_account_in_accounting_his1);
//						accountFrozenVo.setReservedFields2(refund_receive_fee_account_in_accounting_his2);
//						accountFrozenVo.setReservedFields3(refund_receive_fee_account_in_accounting_his3);
						//所属业务冻结交易类型	来自原交易类型
						accountFrozenVo.setFrozenTradeCode(TradeUtilCommon.getTradeType(originalTradeSinglePayment.getLatestTradeReqType()));
						//所属业务冻结交易编号	来自原交易编号
						accountFrozenVo.setFrozenTradeNo(originalTradeSinglePayment.getTradeNo());
						// 调用 冻结余额减少金额
						RealTimeAccountingVo accountingFrozenVo =  bizAccountService.FrozenBalDecrAmtWrapper(accountFrozenVo);
						accountingVoList.add(accountingFrozenVo);
						//2.冻结不够可用补可用余额减少金额
						RealTimeAccountVO accountVo = initRealTimeAccountVO(tradeRquest,refund_receive_amt -(receiveFreezeBal+refundReceivefeeAmt));
						//调用 可用余额减少金额
						RealTimeAccountingVo accountingVo =  bizAccountService.avlBalDecrWrapper(accountVo);
						accountingVoList.add(accountingVo);
						return accountingVoList ;
					}
				}
			}else if(TradeConstants.TRADE_ACCONUT_AVAILABLE_TYPE.equals(originalTradeSinglePayment.getReceiveAccountInAccountingType())){
				RealTimeAccountVO accountVo = initRealTimeAccountVO(tradeRquest,refund_receive_amt);
				//调用 可用余额减少金额
				RealTimeAccountingVo accountingVo =  bizAccountService.avlBalDecrWrapper(accountVo);
				accountingVoList.add(accountingVo);
				return accountingVoList ;
			}
		}
		return null; 
	}

	private RealTimeAccountVO initRealTimeAccountVO(TradeRequest<TradePacketReqBodyInnerRefund> tradeRquest ,long amt){
		RealTimeAccountVO accountVo = new RealTimeAccountVO();
		//目标账户编号	来自原交易【收款方账户账户编号】
		accountVo.setAccountNo(originalTradeSinglePayment.getReceiveAccountNo());
		//核心商户编号	来自原交易【核心商户编号】
		accountVo.setCoreMerchantNo(originalTradeSinglePayment.getCoreMerchantNo());
		//资金池编号	来自原交易【资金池编号】
		accountVo.setPoolNo(originalTradeSinglePayment.getFundPoolNo());
		//用户账务历史保留字段1	来自输入【收款方账户退回出账账务历史1】
		accountVo.setReservedFields1(tradeRquest.getBody().getRefund_receive_account_out_accounting_his1());
		//用户账务历史保留字段2	来自输入【收款方账户退回出账账务历史2】
		accountVo.setReservedFields2(tradeRquest.getBody().getRefund_receive_account_out_accounting_his2());
		//用户账务历史保留字段3	来自输入【收款方账户退回出账账务历史3】
		accountVo.setReservedFields3(tradeRquest.getBody().getRefund_receive_account_out_accounting_his3());
		//所属业务交易类型	本交易类型
		accountVo.setTradeCode(TradeUtilCommon.getTradeType(tradeRquest.getHead().getRequest_code())); // 交易类型0006_002
		//所属业务交易编号	本交易编号
		accountVo.setTradeNo(this.tradeNo);
		//所属业务交易创建日期	本交易创建日期
		accountVo.setTradeGmtCreated(this.gmtCreated);
		//所属业务交易请求类型	本交易请求类型
		accountVo.setTradeReqCode(TradeConstants.TRADE_INNER_PR); // 交易请求类型0006_002_001
		//所属业务交易请求编号	本交易请求编号
		accountVo.setTradeStepNo(this.reqNo);// 
		//所属业务业务系统订单号	本交易业务系统订单号
		accountVo.setSequenceNo(tradeRquest.getBody().getOut_trade_no()); // 本交易业务系统订单号
		accountVo.setAmount(amt);
		return accountVo;
	}
	
	private RealTimeAccountingVo receiveFeeAccountRefund(TradeRequest<TradePacketReqBodyInnerRefund> tradeRquest) {
		long refund_receive_fee_account_in_accounting_amt = str2Long(tradeRquest.getBody().getRefund_receive_fee_account_in_accounting_amt());
		if(refund_receive_fee_account_in_accounting_amt>0){
			RealTimeAccountVO accountVo = new RealTimeAccountVO();
			//目标账户编号	来自原交易【收款方手续费账户账户编号】
			accountVo.setAccountNo(originalTradeSinglePayment.getReceiveFeeAccountNo());
			//核心商户编号	来自原交易【核心商户编号】
			accountVo.setCoreMerchantNo(originalTradeSinglePayment.getCoreMerchantNo());
			//资金池编号	来自原交易【资金池编号】
			accountVo.setPoolNo(originalTradeSinglePayment.getFundPoolNo());
			//用户账务历史保留字段1	来自输入【收款方手续费退回账户入账账务历史1】
			accountVo.setReservedFields1(tradeRquest.getBody().getRefund_receive_fee_account_in_accounting_his1());
			//用户账务历史保留字段2	来自输入【收款方手续费退回账户入账账务历史2】
			accountVo.setReservedFields2(tradeRquest.getBody().getRefund_receive_fee_account_in_accounting_his2());
			//用户账务历史保留字段3	来自输入【收款方手续费退回账户入账账务历史3】
			accountVo.setReservedFields3(tradeRquest.getBody().getRefund_receive_fee_account_in_accounting_his3());
			//所属业务交易类型	本交易类型
			accountVo.setTradeCode(TradeUtilCommon.getTradeType(tradeRquest.getHead().getRequest_code())); // 交易类型0006_002
			//所属业务交易编号	本交易编号
			accountVo.setTradeNo(this.tradeNo);
			//所属业务交易创建日期	本交易创建日期
			accountVo.setTradeGmtCreated(this.gmtCreated);
			//所属业务交易请求类型	本交易请求类型
			accountVo.setTradeReqCode(TradeConstants.TRADE_INNER_PR); // 交易请求类型0006_002_001
			//所属业务交易请求编号	本交易请求编号
			accountVo.setTradeStepNo(this.reqNo); 
			//所属业务业务系统订单号	本交易业务系统订单号
			accountVo.setSequenceNo(tradeRquest.getBody().getOut_trade_no()); // 本交易业务系统订单号
			accountVo.setAmount(refund_receive_fee_account_in_accounting_amt);
			//所属业务冻结交易类型	来自原交易类型
			//所属业务冻结交易编号	来自原交易编号
			if(TradeConstants.TRADE_ACCOUNT_FREEZE_TYPE.equals(originalTradeSinglePayment.getReceiveFeeAccountOutAccountingType())){
				//冻结账户类型
				//所属业务冻结交易类型	来自原交易类型
				accountVo.setFrozenTradeCode(TradeUtilCommon.getTradeType(originalTradeSinglePayment.getLatestTradeReqType()));
				//所属业务冻结交易编号	来自原交易编号
				accountVo.setFrozenTradeNo(originalTradeSinglePayment.getTradeNo());
				return bizAccountService.FrozenBalIncrAmtWrapper(accountVo);
			}else if(TradeConstants.TRADE_ACCONUT_AVAILABLE_TYPE.equals(originalTradeSinglePayment.getReceiveFeeAccountOutAccountingType())){
				//可用账户  单边可用余额增加请求对象组织
				return bizAccountService.avlBalIncrWrapper(accountVo);
			}
			return null ; 
		}
		return null;
	}


	private void createEffecitveRefundLegerList(TradeRequest<TradePacketReqBodyInnerRefund> tradeRquest) throws Exception {
		List<TradePacketReqBodyOtherPofitLossLedger> otherLossLedgerList = tradeRquest.getBody().getProfit_loss_list();
		if(!CollectionUtils.isEmpty(otherLossLedgerList) ){
			List<OtherPofitLossLedgerForRefund> jsonList = new ArrayList<OtherPofitLossLedgerForRefund>();
			for(TradePacketReqBodyOtherPofitLossLedger otherPofitLossLedger : otherLossLedgerList){
				TradeProfitLossLedger ledger = new TradeProfitLossLedger();
				OtherPofitLossLedgerForRefund other = new OtherPofitLossLedgerForRefund();
				//收款方损益业务台账编号	receive_profit_loss_ledger_no
				try {
					//其他损益台账List退回核心商户编号
		            ledger.setCoreMerchantNo(tradeRquest.getHead().getCore_merchant_no());
		            //其他损益台账List退回资金池编号	来自输入
		            ledger.setFundPoolNo(tradeRquest.getHead().getFund_pool_no());
		            //其他损益台账List退回业务台账客户保留字段1	来自输入
		            ledger.setMerchantExtendField1(otherPofitLossLedger.getMerchant_extend_field_1());
		            //其他损益台账List退回业务台账客户保留字段2	来自输入
		            ledger.setMerchantExtendField2(otherPofitLossLedger.getMerchant_extend_field_2());
					//其他损益台账List退回业务台账客户保留字段3	来自输入
		            ledger.setMerchantExtendField3(otherPofitLossLedger.getMerchant_extend_field_3());
		            //其他损益台账List退回交易发起方发起请求编号
		            ledger.setOutTradeNoExt(tradeRquest.getBody().getOut_trade_no_ext());
		            //其他损益台账List退回所属业务交易类型	来自交易
	                ledger.setTradeType(TradeConstants.TRADE_REFUND);
		            //其他损益台账List退回所属业务交易编号	来自交易
		            ledger.setTradeNo(this.tradeNo);
		            //其他损益台账List退回所属业务交易创建日期	来自交易
		            ledger.setGmtTradeCreated(this.gmtCreated);
		            //其他损益台账List退回创建交易请求类型	来自交易
		            ledger.setCreateReqType(TradeConstants.TRADE_INNER_PR);
		            //其他损益台账List退回创建交易请求编号	来自交易
		            ledger.setCreateReqNo(this.reqNo);
		            //其他损益台账List退回生效交易请求类型	来自交易
		            ledger.setEffectiveReqType(TradeConstants.TRADE_INNER_PR);
		            //其他损益台账List退回生效交易请求编号	来自交易
		            ledger.setEffectiveReqNo(this.reqNo);
		            //其他损益台账List退回交易发起方发起请求编号	来自输入
		            ledger.setOutTradeNoExt(tradeRquest.getBody().getOut_trade_no_ext());
		            //其他损益台账List退回交易发起方业务系统订单号	来自输入
		            ledger.setOutTradeNo(tradeRquest.getBody().getOut_trade_no());
		            //其他损益台账List退回损益属性1	来自输入
		            ledger.setProfitLossAttr1(otherPofitLossLedger.getProfit_loss_attr_1());
		            //其他损益台账List退回损益属性2	来自输入
		            ledger.setProfitLossAttr2(otherPofitLossLedger.getProfit_loss_attr_2());
		            //其他损益台账List退回损益属性3	来自输入
		            ledger.setProfitLossAttr3(otherPofitLossLedger.getProfit_loss_attr_3());
		            //其他损益台账List退回损益属性4	来自输入
		            ledger.setProfitLossAttr4(otherPofitLossLedger.getProfit_loss_attr_4());
		            //其他损益台账List退回损益属性5	来自输入
		            ledger.setProfitLossAttr5(otherPofitLossLedger.getProfit_loss_attr_5());
		            //其他损益台账List退回损益属性6	来自输入
		            ledger.setProfitLossAttr6(otherPofitLossLedger.getProfit_loss_attr_6());
		            //其他损益台账List退回损益属性7	来自输入
		            ledger.setProfitLossAttr7(otherPofitLossLedger.getProfit_loss_attr_7());
		            //其他损益台账List退回损益属性8	来自输入
		            ledger.setProfitLossAttr8(otherPofitLossLedger.getProfit_loss_attr_8());
		            //其他损益台账List退回损益属性9	来自输入
		            ledger.setProfitLossAttr9(otherPofitLossLedger.getProfit_loss_attr_9());
		            //其他损益台账List退回损益属性10	来自输入
		            ledger.setProfitLossAttr10(otherPofitLossLedger.getProfit_loss_attr_10());
		            //其他损益台账List退回收入增加金额	来自输入
		            ledger.setIncomeIncr(str2Long(otherPofitLossLedger.getIncome_incr()));
		            other.setIncome_incr(otherPofitLossLedger.getIncome_incr());
		            //其他损益台账List退回收入减少金额	来自输入
		            ledger.setIncomeDecr(str2Long(otherPofitLossLedger.getIncome_decr()));
		            other.setIncome_decr(otherPofitLossLedger.getIncome_decr());
		            //其他损益台账List退回成本费用增加金额	来自输入
		            ledger.setCostIncr(str2Long(otherPofitLossLedger.getCost_incr()));
		            other.setCost_incr(otherPofitLossLedger.getCost_incr());
		            //其他损益台账List退回成本费用减少金额	来自输入
		            ledger.setCostDecr(str2Long(otherPofitLossLedger.getCost_decr()));
		            other.setCost_decr(otherPofitLossLedger.getCost_decr());
		            String other_ledger_no = profitLossLedgerServiceImpl.createEffectiveProfitLossLedger(ledger);
		            other.setLedger_no(other_ledger_no);
		            jsonList.add(other);
				} catch (Exception e) {
					e.printStackTrace();
					LOGGER.error("-->创建生效收款损益退回台账失败："+e.getMessage(),e);
					throw exceptionService.buildBusinessException("TZ000200101");
				}
			}
			this.jsonList = ObjectJsonUtil.object2String(jsonList);
		}
		
	}

	private void createEffecitvePayRefundLeger(TradeRequest<TradePacketReqBodyInnerRefund> tradeRquest) {
		TradePacketReqBodyInnerRefund body = tradeRquest.getBody();
		TradeProfitLossLedger ledger = new TradeProfitLossLedger();
        ledger.setCoreMerchantNo(tradeRquest.getHead().getCore_merchant_no());
        ledger.setFundPoolNo(tradeRquest.getHead().getFund_pool_no());
        ledger.setMerchantExtendField1(body.getRefund_pay_profit_loss_extend_field_1());
        ledger.setMerchantExtendField2(body.getRefund_pay_profit_loss_extend_field_2());
        ledger.setMerchantExtendField3(body.getRefund_pay_profit_loss_extend_field_3());
        ledger.setOutTradeNoExt(body.getOut_trade_no_ext());
        ledger.setTradeType(TradeConstants.TRADE_REFUND);
        ledger.setTradeNo(this.tradeNo);
        ledger.setGmtTradeCreated(this.gmtCreated);
        ledger.setCreateReqType(TradeConstants.TRADE_INNER_PR);
        ledger.setCreateReqNo(this.reqNo);
        ledger.setEffectiveReqType(TradeConstants.TRADE_INNER_PR);
        ledger.setEffectiveReqNo(this.reqNo);
        ledger.setOutTradeNoExt(body.getOut_trade_no_ext());
        ledger.setOutTradeNo(body.getOut_trade_no());
        TradeProfitLossLedger lossledger =profitLossLedgerServiceImpl.queryByLedgerNoAndTradeNo(originalTradeSinglePayment.getPayProfitLossLedgerNo(), originalTradeSinglePayment.getTradeNo()) ;
		if(lossledger == null ){
			throw exceptionService.buildBusinessException("TZ000200101");
		}
        ledger.setProfitLossAttr1(lossledger.getProfitLossAttr1());
        ledger.setProfitLossAttr2(lossledger.getProfitLossAttr2());
        ledger.setProfitLossAttr3(lossledger.getProfitLossAttr3());
        ledger.setProfitLossAttr4(lossledger.getProfitLossAttr4());
        ledger.setProfitLossAttr5(lossledger.getProfitLossAttr5());
        ledger.setProfitLossAttr6(lossledger.getProfitLossAttr6());
        ledger.setProfitLossAttr7(lossledger.getProfitLossAttr7());
        ledger.setProfitLossAttr8(lossledger.getProfitLossAttr8());
        ledger.setProfitLossAttr9(lossledger.getProfitLossAttr9());
        ledger.setProfitLossAttr10(lossledger.getProfitLossAttr10());
        if(originalTradeSinglePayment.getPayProfitLossIncomeDecr() != null && originalTradeSinglePayment.getPayProfitLossIncomeDecr() != 0 ){
        	//若原交易为【付款方损益台账】为【收入减少】时，来自输入【付款方损益退回收到金额】
        	ledger.setIncomeIncr(str2Long(body.getRefund_pay_profit_loss_amt()));
        }else if(originalTradeSinglePayment.getPayProfitLossCostIncr() != null &&originalTradeSinglePayment.getPayProfitLossCostIncr() != 0  ){
        	//若原交易为【付款方损益台账】为【成本费用增加】时，来自输入【付款方损益退回收到金额】
            ledger.setCostDecr(str2Long(body.getRefund_pay_profit_loss_amt()));
        }
		String  ledger_no_effe_pay_pr_refund= profitLossLedgerServiceImpl.createEffectiveProfitLossLedger(ledger);
		this.ledger_no_effe_pay_pr_refund = ledger_no_effe_pay_pr_refund;
//		this.refund_pay_effe_ledger_amt = ledger.getCostIncr();
	}
	
	/**
	 * 修改原交易记录
	 * @param tradeRquest
	 */
	private void updateOriginalTradeRecord(TradeRequest<TradePacketReqBodyInnerRefund> tradeRquest) {
		try {
			TradeSinglePayment tradeSinglePayment = this.originalTradeSinglePayment;
			//最后变更日期时间	系统时间
			tradeSinglePayment.setGmtModifiedLatest(this.gmtCreated);
			//最后变更交易请求类型	本次请求类型
			tradeSinglePayment.setLatestTradeReqType(tradeRquest.getHead().getRequest_code());
			//最后变更交易请求编号	来自本次交易请求记录
			tradeSinglePayment.setLatestReqNo(this.reqNo);
			//交易版本	原版本+1
			//收到方冻结余额	执行STEP5中收款方账户实际冻结余额变化
			tradeSinglePayment.setReceiveFreezeBal(calculateReceiveFreezeBal(tradeRquest));
			//累计收款方退款金额	原值+输入【收款方退回付出金额】
			long sumRefundedReceiveAccountAmt = tradeSinglePayment.getSumRefundedReceiveAccountAmt()+str2Long(tradeRquest.getBody().getRefund_receive_amt());
			tradeSinglePayment.setSumRefundedReceiveAccountAmt(sumRefundedReceiveAccountAmt);
			//累计付款方账户手续费退回金额	原值+输入【付款方手续费账户退回入账金额】
			long sumRefundedPayFeeAccountAmt = tradeSinglePayment.getSumRefundedPayFeeAccountAmt()+str2Long(tradeRquest.getBody().getRefund_pay_fee_account_in_accounting_amt());
			tradeSinglePayment.setSumRefundedPayFeeAccountAmt(sumRefundedPayFeeAccountAmt);
			//累计收款方账户手续费退回金额	原值+输入【收款方手续费账户退回入账金额】
			long sumRefundedReceiveFeeAccountAmt = tradeSinglePayment.getSumRefundedReceiveFeeAccountAmt()+str2Long(tradeRquest.getBody().getRefund_receive_fee_account_in_accounting_amt());
			tradeSinglePayment.setSumRefundedReceiveFeeAccountAmt(sumRefundedReceiveFeeAccountAmt);
			//输入付款方退回收到退回方式为【账户】、【损益充退】时：原值+输入【付款方账户退回入账金额】
			String oiginalTradePayType = tradeRquest.getBody().getRefund_pay_type();
			if(TradeConstants.TRADE_REFUND_PAY_ACCOUNT.equals(oiginalTradePayType)||TradeConstants.TRADE_REFUND_PAY_LOSS_ACCOUNT.equals(oiginalTradePayType)){
				long sumRefundedPayAccountOutAccountingAmt = tradeSinglePayment.getSumRefundedPayAccountOutAccountingAmt()+str2Long(tradeRquest.getBody().getRefund_pay_account_in_accounting_amt());
				tradeSinglePayment.setSumRefundedPayAccountOutAccountingAmt(sumRefundedPayAccountOutAccountingAmt);
			}
			//输入付款方退回收到退回方式为【损益直退】、【损益充退】时：原值+输入【付款方损益退回收到金额】
			if(TradeConstants.TRADE_REFUND_PAY_LOSS.equals(oiginalTradePayType)||TradeConstants.TRADE_REFUND_PAY_LOSS_ACCOUNT.equals(oiginalTradePayType)){
				long sumRefundedPayLedgerAmt = tradeSinglePayment.getSumRefundedPayLedgerAmt()+str2Long(tradeRquest.getBody().getRefund_pay_profit_loss_amt());
				tradeSinglePayment.setSumRefundedPayLedgerAmt(sumRefundedPayLedgerAmt);
			}
			tradeSinglePayment = paymentService.updateTrade(tradeSinglePayment);
			LOGGER.info("version:"+tradeSinglePayment.getVersion());
			this.originalTradeSinglePayment = tradeSinglePayment;
		} catch (Exception e) {
			e.printStackTrace();
			LOGGER.error("-->修改原支付交易记录出错:"+e.getMessage(),e);
			throw exceptionService.buildBusinessException("JY00060020011001300");
		}
	}

	/**
	 * @Title:calculateReceiveFreezeBal 
	 * @Description:计算收款方账户实际冻结余额变化 
	 * @param tradeRquest
	 * @return
	 * @throws Exception
	 * @return:long
	 * @date:2016年6月8日 上午10:12:50
	 */
	private long calculateReceiveFreezeBal(TradeRequest<TradePacketReqBodyInnerRefund> tradeRquest) throws Exception{
		long lastReceiveFreezeBal = 0l ;
		//收款方退回付出金额
		long refundReceiveAmt = str2Long(tradeRquest.getBody().getRefund_receive_amt());
		//收款方手续费账户退回入账金额
		long refundReceivefeeAmt =  0l ; 
		//原交易中当前【收到方冻结余额】	
		long receiveFreezeBal = AmountUtils.ifNullOrElse(originalTradeSinglePayment.getReceiveFreezeBal());
		//如果原交易收款方手续出账类型为 ：冻结账户类型
		if(TradeConstants.TRADE_ACCOUNT_FREEZE_TYPE.equals(originalTradeSinglePayment.getReceiveFeeAccountOutAccountingType())){
			refundReceivefeeAmt = str2Long(tradeRquest.getBody().getRefund_receive_fee_account_in_accounting_amt());
		}
		if(TradeConstants.TRADE_ACCOUNT_FREEZE_TYPE.equals(originalTradeSinglePayment.getReceiveAccountInAccountingType())){
			//若【收款方退回付出金额】=<原交易中当前【收到方冻结余额】+冻结退回的【收款方手续费账户退回入账金额】
			if(refundReceiveAmt <= (receiveFreezeBal+refundReceivefeeAmt)){
				lastReceiveFreezeBal = (receiveFreezeBal+refundReceivefeeAmt)-refundReceiveAmt;
			}else if(refundReceiveAmt > (receiveFreezeBal+refundReceivefeeAmt)){
				lastReceiveFreezeBal =  0l ; 
			}
		}else if(TradeConstants.TRADE_ACCONUT_AVAILABLE_TYPE.equals(originalTradeSinglePayment.getReceiveAccountInAccountingType())){
			lastReceiveFreezeBal = (receiveFreezeBal+refundReceivefeeAmt);
		}
		return lastReceiveFreezeBal;
	}
	
	private void createRefundSuccRecord(TradeRequest<TradePacketReqBodyInnerRefund> tradeRquest) {
		TradeSinglePaymentRefund refund = new TradeSinglePaymentRefund();
		try {
			TradePacketReqBodyInnerRefund body = tradeRquest.getBody();
			//合作商户编号
			refund.setPartnerId(tradeRquest.getHead().getPartner_id());
			//交易编号	系统自动产生
			refund.setTradeNo(this.tradeNo);
			//核心商户编号	来自输入
			refund.setCoreMerchantNo(tradeRquest.getHead().getCore_merchant_no());
			//资金池编号	来自输入
			refund.setFundPoolNo(tradeRquest.getHead().getFund_pool_no());
			//原交易编号	来自输入
			refund.setOriginalTradeNo(body.getOriginal_trade_no());
			//交易客户保留字段1~10	来自输入
			refund.setMerchantExtendField1(body.getMerchant_extend_field_1());
			refund.setMerchantExtendField2(body.getMerchant_extend_field_2());
			refund.setMerchantExtendField3(body.getMerchant_extend_field_3());
			refund.setMerchantExtendField4(body.getMerchant_extend_field_4());
			refund.setMerchantExtendField5(body.getMerchant_extend_field_5());
			refund.setMerchantExtendField6(body.getMerchant_extend_field_6());
			refund.setMerchantExtendField7(body.getMerchant_extend_field_7());
			refund.setMerchantExtendField8(body.getMerchant_extend_field_8());
			refund.setMerchantExtendField9(body.getMerchant_extend_field_9());
			refund.setMerchantExtendField10(body.getMerchant_extend_field_10());
			//交易发起方发起请求编号	来自输入
			refund.setOutTradeNoExt(body.getOut_trade_no_ext());
			//交易发起方业务系统订单号	来自输入
			refund.setOutTradeNo(body.getOut_trade_no());
			//交易创建日期时间	系统时间
			refund.setGmtCreated(gmtCreated);
			//最后变更日期时间	系统时间
			refund.setGmtModifiedLatest(gmtCreated);
			//最后变更交易请求类型	本次请求类型
			refund.setLatestTradeReqType(tradeRquest.getHead().getRequest_code());
			//最后变更交易请求编号	来自交易请求记录
			refund.setLatestReqNo(this.reqNo);
			//交易结束状态	【结束】
			refund.setCloseStatus(TradeConstants.TRADE_CLOSE_STATUS_END);
			//交易状态	【退款成功】
			refund.setStatus(TradeConstants.TRADE_PAYMENT_REFUND_SUCCESS);
			//交易组别	1【内部支付退款】
			refund.setTradeCatatory(TradeConstants.TRADE_PAYMENT_REFUND_INNER);
			//交易版本	0
			refund.setVersion(0);
			
			//退款扣帐日期时间	系统时间
			refund.setGmtRefundDeduct(gmtCreated);
			String thisTradePayType = body.getRefund_pay_type();
			String orig_payType = originalTradeSinglePayment.getPayType();//原交易付出方式
			String orig_receiveType = originalTradeSinglePayment.getReceiveType();//原收款方收到方式
			//账户、损益充退时与【付款方账户退回人账金额】相同；损益直付时与【付款方损益退回收到金额】相同
			if(TradeConstants.TRADE_REFUND_PAY_ACCOUNT.equals(thisTradePayType)||TradeConstants.TRADE_REFUND_PAY_LOSS_ACCOUNT.equals(thisTradePayType)){//账户、损益充退时
				//付款方账户退回人账金额
				refund.setRefundPayAmt(str2Long(body.getRefund_pay_account_in_accounting_amt()));
			}else if(TradeConstants.TRADE_REFUND_PAY_LOSS.equals(thisTradePayType)){//损益直退
				//付款方损益退回收到金额
				refund.setRefundPayAmt(str2Long(body.getRefund_pay_profit_loss_amt()));
			}
			//付款方退回收到方式
			if(TradeConstants.PAY_BY_ACCOUNT.equals(orig_payType)){
				refund.setRefundPayType(TradeConstants.TRADE_REFUND_PAY_ACCOUNT);
			}else if(TradeConstants.PAY_BY_PROFIT_LOSS.equals(orig_payType)){
				refund.setRefundPayType(TradeConstants.TRADE_REFUND_PAY_LOSS);
			}else if(TradeConstants.PAY_BY_THIRD_ACCOUNT.equals(orig_payType)){
				refund.setRefundPayType(TradeConstants.TRADE_REFUND_PAY_ACCOUNT);
			}else if(TradeConstants.PAY_BY_PROFIT_LOSS_ACCOUNT.equals(orig_payType)){
				refund.setRefundPayType(body.getRefund_pay_type());
			}
			if(!StringUtils.isEmpty(this.ledger_no_effe_pay_pr_refund)){
				refund.setRefundPayProfitLossLedgerNo(this.ledger_no_effe_pay_pr_refund);
				long refund_pay_profit_loss_amt = str2Long(body.getRefund_pay_profit_loss_amt());
				refund.setRefundPayProfitLossAmt(refund_pay_profit_loss_amt);
			}
			if(ca_in_account_flag){
				refundToCA(tradeRquest, refund);
			}
			//付款方手续费账户退回账户编号	来自原交易【付款方手续费账户账户编号】
			refund.setRefundPayFeeAccountNo(originalTradeSinglePayment.getPayFeeAccountNo());
			//付款方手续费账户退回所属商户编号	来自原交易【付款方手续费账户所属商户编号】
			refund.setRefundPayFeeAccountMerchantNo(originalTradeSinglePayment.getPayFeeAccountMerchantNo());
			//付款方手续费账户退回入账金额	来自输入【付款方手续费账户退回入账金额】
			refund.setRefundPayFeeAccountInAccountingAmt(str2Long(body.getRefund_pay_fee_account_in_accounting_amt()));
			//付款方手续费账户退回入账子账户类型	【可用】
			refund.setRefundPayFeeAccountInAccountingType(TradeConstants.TRADE_ACCONUT_AVAILABLE_TYPE);
			//付款方手续费账户退回入账账务历史1~3	来自输入
			refund.setRefundPayFeeAccountInAccountingHis1(body.getRefund_pay_fee_account_in_accounting_his1());
			refund.setRefundPayFeeAccountInAccountingHis2(body.getRefund_pay_fee_account_in_accounting_his2());
			refund.setRefundPayFeeAccountInAccountingHis3(body.getRefund_pay_fee_account_in_accounting_his3());			
			
			//收款方退回付出金额	来自输入
			refund.setRefundReceiveAmt(str2Long(body.getRefund_receive_amt()));
			//收款方退回付出方式	
			refund.setRefundReceiveType(originalTradeSinglePayment.getReceiveType());
			
			if(TradeConstants.RECEIVE_BY_THIRD_PROFIT_LOSS.equals(orig_receiveType)){
				//收款方损益退回业务台账编号	来自台账
				refund.setRefundReceiveProfitLossLedgerNo(this.refundReceivedLossLedgerNo);
				//收款方损益退回付款金额	来自台账
				refund.setRefundReceiveProfitLossAmt(str2Long(body.getRefund_receive_amt()));
			}else if(TradeConstants.RECEIVE_BY_THIRD_ACCOUNT.equals(orig_receiveType)){
				//收款方账户退回账户编号	来自原交易【收款方账户账户编号】
				refund.setRefundReceiveAccountNo(originalTradeSinglePayment.getReceiveAccountNo());
				//收款方账户退回所属商户编号	来自原交易【收款方账户所属商户编号】
				refund.setRefundReceiveAccountMerchantNo(originalTradeSinglePayment.getReceiveAccountMerchantNo());
				//收款方账户退回出账金额	来自输入【收款方退回付出金额】
				refund.setRefundReceiveAccountOutAccountingAmt(str2Long(body.getRefund_receive_amt()));
				//收款方账户退回出账账务历史1	来自输入
				refund.setRefundReceiveAccountOutAccountingHis1(body.getRefund_receive_account_out_accounting_his1());
				//收款方账户退回出账账务历史2	来自输入
				refund.setRefundReceiveAccountOutAccountingHis2(body.getRefund_receive_account_out_accounting_his2());
				//收款方账户退回出账账务历史3	来自输入
				refund.setRefundReceiveAccountOutAccountingHis3(body.getRefund_receive_account_out_accounting_his3());
			}
			//收款方手续费部分
			//收款方手续费账户退回账户编号	来自原交易【收款方手续费账户账户编号】
			refund.setRefundReceiveFeeAccountNo(originalTradeSinglePayment.getReceiveFeeAccountNo());
			//收款方手续费账户退回所属商户编号	来自原交易【收款方手续费账户所属商户编号】
			refund.setRefundReceiveFeeAccountMerchantNo(originalTradeSinglePayment.getReceiveFeeAccountMerchantNo());
			//收款方手续费账户退回入账金额	来自输入
			refund.setRefundReceiveFeeAccountInAccountingAmt(str2Long(body.getRefund_receive_fee_account_in_accounting_amt()));
			//收款方手续费账户退回入账子账户类型	来自原交易【收款方手续费账户出账子账户类型】
			refund.setRefundReceiveFeeAccountInAccountingType(originalTradeSinglePayment.getReceiveFeeAccountOutAccountingType());
			//收款方手续费账户退回入账账务历史1	来自输入
			refund.setRefundReceiveFeeAccountInAccountingHis1(body.getRefund_receive_fee_account_in_accounting_his1());
			//收款方手续费账户退回入账账务历史2	来自输入
			refund.setRefundReceiveFeeAccountInAccountingHis2(body.getRefund_receive_fee_account_in_accounting_his2());
			//收款方手续费账户退回入账账务历史3	来自输入
			refund.setRefundReceiveFeeAccountInAccountingHis3(body.getRefund_receive_fee_account_in_accounting_his3());
			 
			 //其他损益台账部分
			 refund.setRefundExtendProfitLoss(jsonList);
			 //付出方冻结余额	0
			 refund.setPayFreezeBal(0l);
			 //收到方冻结余额	0
			 refund.setReceiveFreezeBal(0l);
			 paymentRefund = refundService.createInnerTradeForRefundOfSuccess(refund);
			
		} catch (Exception e) {
			ca_in_account_flag =false;
			LOGGER.error("-->创建退款扣款成功交易记录失败："+e.getMessage(),e);
			e.printStackTrace();
		}
	}


	private void refundToCA(TradeRequest<TradePacketReqBodyInnerRefund> tradeRquest, TradeSinglePaymentRefund refund) {
		//付款方账户退回账户编号	来自原交易【付款方账户账户编号】
		refund.setRefundPayAccountNo(originalTradeSinglePayment.getPayAccountNo());
		//付款方账户退回所属商户编号	来自原交易【付款方账户所属商户编号】
		refund.setRefundPayAccountMerchantNo(originalTradeSinglePayment.getPayAccountMerchantNo());
		//付款方账户退回入账金额	来自输入【付款方账户退回入账金额】
		refund.setRefundPayAccountInAccountingAmt(str2Long(tradeRquest.getBody().getRefund_pay_account_in_accounting_amt()));
		//付款方账户退回出账子账户类型	【可用】
		refund.setRefundPayAccountOutAccountingType(TradeConstants.TRADE_ACCONUT_AVAILABLE_TYPE);
		//付款方账户退回出账账务历史1~3	来自输入
		refund.setRefundPayAccountOutAccountingHis1(tradeRquest.getBody().getRefund_pay_account_out_accounting_his1());
		refund.setRefundPayAccountOutAccountingHis2(tradeRquest.getBody().getRefund_pay_account_out_accounting_his2());
		refund.setRefundPayAccountOutAccountingHis3(tradeRquest.getBody().getRefund_pay_account_out_accounting_his3());
		//付款方账户退回入账子账户类型	【可用】
		refund.setRefundPayAccountInAccountingType(TradeConstants.TRADE_ACCONUT_AVAILABLE_TYPE);
		//付款方账户退回入账账务历史1~3	来自输入
		refund.setRefundPayAccountInAccountingHis1(tradeRquest.getBody().getRefund_pay_account_in_accounting_his1());
		refund.setRefundPayAccountInAccountingHis2(tradeRquest.getBody().getRefund_pay_account_in_accounting_his2());
		refund.setRefundPayAccountInAccountingHis3(tradeRquest.getBody().getRefund_pay_account_in_accounting_his3());
	}

	@Override
	public void formatValidate(TradeRequest<TradePacketReqBodyInnerRefund> tradeRquest) throws Exception {
		String msg = TradeValidationUtil.validateRequestWeak(tradeRquest);
		if (!StringUtils.isEmpty(msg)) {
			throw exceptionService.buildBusinessException("JY00060020011000100", msg);
		}
		//【income_incr、income_decr、cost_incr、cost_decr】有且有且只能有一个金额存在
//		validateOtherProfitLossList(tradeRquest.getBody().getProfit_loss_list());
	}

	/**
	 * @Title:validateOtherProfitLossList 
	 * @Description: 验证【income_incr、income_decr、cost_incr、cost_decr】有且只有一个金额存在
	 * @param bodyOtherPofitLossLedgers
	 * @return:void
	 * @author:zoran.huang
	 * @date:2016年6月14日 上午10:47:33
	 */
	private void validateOtherProfitLossList(List<TradePacketReqBodyOtherPofitLossLedger> bodyOtherPofitLossLedgers){
		if(bodyOtherPofitLossLedgers != null ){
			for(TradePacketReqBodyOtherPofitLossLedger pofitLossLedger : bodyOtherPofitLossLedgers){
				int incomeIncr = StringUtils.isEmpty(pofitLossLedger.getIncome_incr())?0:1;
				int iincomeDecr = StringUtils.isEmpty(pofitLossLedger.getIncome_decr())?0:1;
				int costIncr = StringUtils.isEmpty(pofitLossLedger.getCost_incr())?0:1;
				int costDecr = StringUtils.isEmpty(pofitLossLedger.getCost_decr())?0:1;
				int result = (incomeIncr+iincomeDecr+costIncr+costDecr);
				if(result != 1 ){
					LOGGER.info("-->输入参数校验不通过： 【income_incr、income_decr、cost_incr、cost_decr】有且有且只能有一个金额存在");
					throw exceptionService.buildBusinessException("JY00060020011000100", "其他损益属性【income_incr、income_decr、cost_incr、cost_decr】有且只有一个金额存在");
				}
				/*boolean isHaveIncomeIncr = StringUtils.isEmpty(pofitLossLedger.getIncome_incr());
				boolean isHaveIncomeDecr = StringUtils.isEmpty(pofitLossLedger.getIncome_decr());
				boolean isHaveCostIncr = StringUtils.isEmpty(pofitLossLedger.getCost_incr());
				boolean isHaveCostDecr = StringUtils.isEmpty(pofitLossLedger.getCost_decr());
				//如果其他损益台账List收入增加金额有值
				if(!isHaveIncomeIncr){
					//判断其他三个属性必须全部为空
					if( !(isHaveIncomeDecr && isHaveCostIncr && isHaveCostDecr)){
						logger.info("-->输入参数校验不通过： 【income_incr、income_decr、cost_incr、cost_decr】有且有且只能有一个金额存在");
						throw exceptionService.buildBusinessException("JY00060020021000100", "其他损益属性【income_decr、cost_incr、cost_decr】，必须为空");
					}
					return ;
				}
				//如果其他损益台账List收入减少金额有值
				if(!isHaveIncomeDecr){
					//判断其他两个属性必须全部为空
					if(!(isHaveCostIncr && isHaveCostDecr)){
						logger.info("-->输入参数校验不通过： 【income_incr、income_decr、cost_incr、cost_decr】有且有且只能有一个金额存在");
						throw exceptionService.buildBusinessException("JY00060020021000100", "其他损益属性【cost_incr、cost_decr】，必须为空");
					}
					return ;
				}
				//如果其他损益台账List成本费用增加金额有值
				if(!isHaveCostIncr){
					//判断剩余的属性
					if(!isHaveCostDecr){
						logger.info("-->输入参数校验不通过： 【income_incr、income_decr、cost_incr、cost_decr】有且只能有一个金额存在");
						throw exceptionService.buildBusinessException("JY00060020021000100", "其他损益属性【cost_decr】，必须为空");
					}
					return ;
				}
				//如果其他损益台账List成本费用减少金额为空
				if(isHaveIncomeDecr){
					logger.info("-->输入参数校验不通过： 【income_incr、income_decr、cost_incr、cost_decr】有且有且只能有一个金额存在");
					throw exceptionService.buildBusinessException("JY00060020021000100", "其他损益属性【income_incr、income_decr、cost_incr、cost_decr】，全部为空");
				}*/
			}
		}
	}
	
	@Override
	public void bizValidate(TradeRequest<TradePacketReqBodyInnerRefund> tradeRquest) throws Exception {
		//1	 校验【核心商户编号】、【资金池编号】对应关系
		TradeCommonValidation.validateCoreMerchantNoAndFundPoolNo(tradeRquest,false,exceptionService.buildBusinessException("JY00060020011000200"));
		
//		validateCoreMerchantNoAndFundPoolNo(tradeRquest);
		
		//2	校验【交易发起方发起请求编号】是否重复
		TradeSinglePaymentRefundExample example = new TradeSinglePaymentRefundExample();
		Criteria criteria = example.createCriteria();
		criteria.andFundPoolNoEqualTo(tradeRquest.getHead().getFund_pool_no())
		.andOutTradeNoExtEqualTo(tradeRquest.getBody().getOut_trade_no_ext());
		List<TradeSinglePaymentRefund> list = refundService.queryByExample(example);
		if(!CollectionUtils.isEmpty(list)){
			throw exceptionService.buildBusinessException("JY00060020011000400");
		}
			
		//3	校验【核心商户编号】、【资金池编号】、【原交易编号】，原交易类型对应关系
		//这一步给原交易赋值
		validateCoreMerchAndFoundPoolAndOirTradeNoAndOirTradeType(tradeRquest);
		//4	校验原交易状态
		String status = this.originalTradeSinglePayment.getStatus();
		if(!TradeConstants.TRADE_PAYMENT_SUCCESS.equals(status)){
			throw exceptionService.buildBusinessException("JY00060020011000600");
		}
		//5	校验原交易是否已关闭
		String continueTradeStatus = this.originalTradeSinglePayment.getContinueTradeStatus();
		if(!TradeConstants.CONTINUE_TRADE_STATUS_OPEN.equals(continueTradeStatus)){
			throw exceptionService.buildBusinessException("JY00060020011000700");
		}
		//6	原交易【交易组别】和原交易【付款方付出方式】本交易【付款方退回收到方式】校验
		validateOirTradeGroupAndOirTradePayTypeAndRefundPayType(tradeRquest);
		//7	校验退款金额关系
		validate7(tradeRquest);
		//8	校验累计退款各项金额是否超过原交易各项金额
		validateSumAmtAndOirTradeSumAmt(tradeRquest);
		
	}

//	private void validateCoreMerchantNoAndFundPoolNo(TradeRequest<TradePacketReqBodyInnerRefund> tradeRquest)
//			throws Exception {
//		try {
//			PoolQueryInnerReq req = new PoolQueryInnerReq();
//			PoolQueryInnerRsp rsp = new PoolQueryInnerRsp();
//			req.setCoreMerchantNo(tradeRquest.getHead().getCore_merchant_no());
//			req.setPoolNo(tradeRquest.getHead().getFund_pool_no());
//			HttpConfig httpConfig = new HttpConfig();
//			httpConfig.setHost(PropertyUtil.getProperty("trade.host.ip",
//					"/config/properties/servers.properties"));
//			httpConfig.setConnectionTimeout(100000);
//			httpConfig.setSoTimeout(200000);
//			httpConfig.setCharset("UTF-8");
//			long start = System.currentTimeMillis();
//			rsp = PoolClient.poolQuery(req, httpConfig);
//			LOGGER.info("调用接口消耗【" + (System.currentTimeMillis() - start) + "】");
//			if (rsp != null && "S".equals(rsp.getSuccess())
//					&& rsp.getList() != null && rsp.getList().size() > 0) {
//				return;
//			}
//		} catch (Exception e) {
//			e.printStackTrace();
//			LOGGER.error(e.getMessage(), e);
//		}
//		throw exceptionService.buildBusinessException("JY00060020011000200");
//	}
	
	/**
	 * 校验【核心商户编号】、【资金池编号】、【原交易编号】，原交易类型对应关系
	 * @param tradeRquest
	 */
	private void validateCoreMerchAndFoundPoolAndOirTradeNoAndOirTradeType(TradeRequest<TradePacketReqBodyInnerRefund> tradeRquest) {
		TradeSinglePaymentExample example = new TradeSinglePaymentExample();
		com.liantuo.trade.orm.pojo.TradeSinglePaymentExample.Criteria criteria = example.createCriteria();
		criteria.andCoreMerchantNoEqualTo(tradeRquest.getHead().getCore_merchant_no())
		.andFundPoolNoEqualTo(tradeRquest.getHead().getFund_pool_no())
		.andTradeNoEqualTo(tradeRquest.getBody().getOriginal_trade_no());
		List<TradeSinglePayment> list = paymentService.queryByExample(example);
		if(CollectionUtils.isEmpty(list)){
			throw exceptionService.buildBusinessException("JY00060020011000500");
		}
		TradeSinglePayment originalTradeSinglePayment = list.get(0);
		this.originalTradeSinglePayment = originalTradeSinglePayment;
	}
	/**
	 * 属于以下情况校验通过
		原交易组别	原交易付出方式	本交易付款方退回方式
		内部支付类	账户			空（账户）
		内部支付类	损益直付		空（损益直退）
		内部支付类	损益充付		账户
		内部支付类	损益充付		损益充退
		外部支付类	第三方充付		空（账户）
	 *	原交易【交易组别】和原交易【付款方付出方式】本交易【付款方退回收到方式】校验	
	 * @param tradeRquest
	 */
	private void validateOirTradeGroupAndOirTradePayTypeAndRefundPayType(TradeRequest<TradePacketReqBodyInnerRefund> tradeRquest) {
		//原交易组别
		String tradeCatagory = this.originalTradeSinglePayment.getTradeCatagory();
		//原交易付出方式
		String payType = this.originalTradeSinglePayment.getPayType();
		//本交易付款退回方式
		String local_refund_pay_type = tradeRquest.getBody().getRefund_pay_type();
		//付款方账户退回入账金额	原交易付款方付出方式为【账户】、【损益充付】、【第三方充付】时必须填写 
		String amt = tradeRquest.getBody().getRefund_pay_account_in_accounting_amt();
		if(TradeConstants.TRADE_PAYMENT_REFUND_INNER.equals(tradeCatagory)){//内部支付类
			if(TradeConstants.TRADE_REFUND_PAY_ACCOUNT.equals(payType)){//原交易付出方式为账户
				if(StringUtils.isEmpty(amt)){
					LOGGER.info("原交易付款方付出方式为【账户】 付款方账户退回入账金额不能为空！");
					throw exceptionService.buildBusinessException("JY00060020011000800");
				}
				if(!(StringUtils.isEmpty(local_refund_pay_type)||TradeConstants.TRADE_REFUND_PAY_ACCOUNT.equals(local_refund_pay_type))){
					LOGGER.info("外部支付原交易组别、原交易付出方式与本次付款退回方式不匹配！");
					throw exceptionService.buildBusinessException("JY00060020011000800");
				}
			}else if(TradeConstants.PAY_BY_PROFIT_LOSS.equals(payType)){//原交易付出方式为损益直付
				if(!(StringUtils.isEmpty(local_refund_pay_type)||TradeConstants.TRADE_REFUND_PAY_LOSS.equals(local_refund_pay_type))){
					LOGGER.info("外部支付原交易组别、原交易付出方式与本次付款退回方式不匹配！");
					throw exceptionService.buildBusinessException("JY00060020011000800");
				}
			}else if(TradeConstants.PAY_BY_PROFIT_LOSS_ACCOUNT.equals(payType)){//原交易付出方式为损益充付
				if(StringUtils.isEmpty(amt)){
					LOGGER.info("原交易付出方式为损益充付, 付款方账户退回入账金额不能为空！");
					throw exceptionService.buildBusinessException("JY00060020011000800");
				}
				if(!(TradeConstants.TRADE_REFUND_PAY_ACCOUNT.equals(local_refund_pay_type)||TradeConstants.TRADE_REFUND_PAY_LOSS_ACCOUNT.equals(local_refund_pay_type))){
					LOGGER.info("外部支付原交易组别、原交易付出方式与本次付款退回方式不匹配！");
					throw exceptionService.buildBusinessException("JY00060020011000800");
				}
			}else if(TradeConstants.PAY_BY_THIRD_ACCOUNT.equals(payType)){//原交易付款方式为第三方充付
				if(StringUtils.isEmpty(amt)){
					LOGGER.info("原交易付款方式为第三方充付, 付款方账户退回入账金额不能为空！");
					throw exceptionService.buildBusinessException("JY00060020011000800");
				}
			}else {//其它不在条件范围内均返回异常
				LOGGER.info("外部支付原交易组别、原交易付出方式与本次付款退回方式不匹配！");
				throw exceptionService.buildBusinessException("JY00060020011000800");
			}
		}else if(TradeConstants.TRADE_PAYMENT_REFUND_OUTER.equals(tradeCatagory)){//外部支付类
				if(!(TradeConstants.PAY_BY_THIRD_ACCOUNT.equals(payType)&&(StringUtils.isEmpty(local_refund_pay_type)||TradeConstants.TRADE_REFUND_PAY_ACCOUNT.equals(local_refund_pay_type)))){
					LOGGER.info("外部支付原交易组别、原交易付出方式与本次付款退回方式不匹配！");
					throw exceptionService.buildBusinessException("JY00060020011000800");
				}
		}else{
			LOGGER.info("外部支付原交易组别、原交易付出方式与本次付款退回方式不匹配！");
			throw exceptionService.buildBusinessException("JY00060020011000800");
		}
	}
	
	/**
	 * 校验以下各项金额，按照正负向分别汇总后应相等，若不等，校验失败，返回【失败】
		付款方损益退回金额			负向，【付款方退回收到方式】为【损益充退】时不计算
		付款方账户退回入账金额			负向
		付款方手续费账户退回入账金额		负向
		收款方退回付出金额			正向
		收款方手续费账户退回入账金额		负向
		其他损益台账X收入增加金额		负向
		其他损益台账X收入减少金额		正向
		其他损益台账X成本费用增加金额		正向
		其他损益台账X成本费用减少金额		负向
	 *	校验退款金额关系
	 * @param tradeRquest
	 */
	private void validate7(TradeRequest<TradePacketReqBodyInnerRefund> tradeRquest) {
		boolean isNotEmpty = false;
		TradePacketReqBodyInnerRefund body = tradeRquest.getBody();
		//本交易付款退回方式
		String local_refund_pay_type = tradeRquest.getBody().getRefund_pay_type();
		//1	 <0
		//付款方损益退回金额
		long refund_pay_profit_loss_amt = str2Long(body.getRefund_pay_profit_loss_amt());
		if(TradeConstants.TRADE_REFUND_PAY_LOSS_ACCOUNT.equals(local_refund_pay_type)){
			refund_pay_profit_loss_amt = 0l;
		}
		//付款方账户退回入账金额
		long refund_pay_account_in_accounting_amt = str2Long(body.getRefund_pay_account_in_accounting_amt());
		//付款方手续费账户退回入账金额
		long refund_pay_fee_account_in_accounting_amt = str2Long(body.getRefund_pay_fee_account_in_accounting_amt());
		//收款方手续费账户退回入账金额
		long refund_receive_fee_account_in_accounting_amt = str2Long(body.getRefund_receive_fee_account_in_accounting_amt());
		//其他损益台账X收入增加金额
		long income_incr = 0;
		//其他损益台账X成本费用减少金额
		long cost_decr = 0;
		List<TradePacketReqBodyOtherPofitLossLedger> profit_loss_list = body.getProfit_loss_list();
		if(!CollectionUtils.isEmpty(profit_loss_list)){
			isNotEmpty = true;
			for (TradePacketReqBodyOtherPofitLossLedger item : profit_loss_list) {
				 income_incr = str2Long(item.getIncome_incr());
				 cost_decr = str2Long(item.getCost_decr());
			}
		}
		//2	 >0
		//收款方退回付出金额
		long refund_receive_amt = str2Long(body.getRefund_receive_amt());
		//其他损益台账X收入减少金额
		long income_decr = 0;
		//其他损益台账X成本费用增加金额
		long cost_incr =0;
		if(isNotEmpty){
			for (TradePacketReqBodyOtherPofitLossLedger item : profit_loss_list) {
				 income_decr = str2Long(item.getIncome_decr());
				 cost_incr = str2Long(item.getCost_incr());
			}
		}
		long negative = refund_pay_profit_loss_amt+refund_pay_account_in_accounting_amt+refund_pay_fee_account_in_accounting_amt+refund_receive_fee_account_in_accounting_amt
				+income_incr+cost_decr;
		long positive = refund_receive_amt+income_decr+cost_incr;
		LOGGER.info("负向金额："+negative);
		LOGGER.info("正向金额："+positive);
		if((negative-positive)!=0){
			LOGGER.info("正负向金额不相等!");
			throw exceptionService.buildBusinessException("JY00060020011000900");
		}
	}
	
	/**
	 * 
		累计收款方退款金额	原交易【累计收款方退款金额】+输入【收款方退回付出金额】<=原交易【收款方收到金额】
		（注：即使分润了，也可以先行退款，所以不考虑分润）
		累计付款方账户手续费退回金额	原交易【累计付款方账户手续费退回金额】+输入【付款方手续费账户退回入账金额】<=原交易【付款方手续费账户出账金额】
		累计收款方账户手续费退回金额	原交易【累计收款方账户手续费退回金额】+输入【收款方手续费账户退回入账金额】<=原交易【收款方手续费账户出账金额】
		累计付款方账户出账退回金额	当输入【付款方退回收到方式】为【账户】或【损益充退】时校验，原交易【累计付款方账户出账退回金额】+输入【付款方账户退回入账金额】<=原交易【付款方账户出账金额】　
		累计付款方台账退回金额	当输入【付款方退回收到方式】为【损益直退】或【损益充退】时校验，原交易【累计付款方台账退回金额】+输入【付款方损益退回金额】<=原交易【付款方损益收入减少金额】+【付款方损益成本费用增加金额】
	 *	校验累计退款各项金额是否超过原交易各项金额
	 * @param tradeRquest
	 */
	private void validateSumAmtAndOirTradeSumAmt(TradeRequest<TradePacketReqBodyInnerRefund> tradeRquest) {
		Long sumRefundedReceiveAccountAmt = AmountUtils.ifNullOrElse(this.originalTradeSinglePayment.getSumRefundedReceiveAccountAmt());
		Long refund_receive_amt = str2Long(tradeRquest.getBody().getRefund_receive_amt());
		Long receiveAmt = AmountUtils.ifNullOrElse(this.originalTradeSinglePayment.getReceiveAmt());
		if(sumRefundedReceiveAccountAmt+refund_receive_amt>receiveAmt){
			LOGGER.info("原交易【累计收款方退款金额】+输入【收款方退回付出金额】>原交易【收款方收到金额】");
			throw exceptionService.buildBusinessException("JY00060020011001200");
		}
		Long sumRefundedPayFeeAccountAmt = AmountUtils.ifNullOrElse(this.originalTradeSinglePayment.getSumRefundedPayFeeAccountAmt());
		long payFeeInAmt = str2Long(tradeRquest.getBody().getRefund_pay_fee_account_in_accounting_amt());
		Long payFeeAccountOutAccountingAmt = AmountUtils.ifNullOrElse(this.originalTradeSinglePayment.getPayFeeAccountOutAccountingAmt());
		if(sumRefundedPayFeeAccountAmt+payFeeInAmt>payFeeAccountOutAccountingAmt){
			LOGGER.info("原交易【累计付款方账户手续费退回金额】+输入【付款方手续费账户退回入账金额】>原交易【付款方手续费账户出账金额】");
			throw exceptionService.buildBusinessException("JY00060020011001200");
		}
		
		Long sumRefundedReceiveFeeAccountAmt = AmountUtils.ifNullOrElse(this.originalTradeSinglePayment.getSumRefundedReceiveFeeAccountAmt());
		long receiveFeeInAmt = str2Long(tradeRquest.getBody().getRefund_receive_fee_account_in_accounting_amt());
		Long receiveFeeAccountOutAccountingAmt = AmountUtils.ifNullOrElse(this.originalTradeSinglePayment.getReceiveFeeAccountOutAccountingAmt());
		if(sumRefundedReceiveFeeAccountAmt+receiveFeeInAmt>receiveFeeAccountOutAccountingAmt){
			LOGGER.info("原交易【累计收款方账户手续费退回金额】+输入【收款方手续费账户退回入账金额】>原交易【收款方手续费账户出账金额】");
			throw exceptionService.buildBusinessException("JY00060020011001200");
		}
		//付款方退回收到方式
		String refund_pay_type = tradeRquest.getBody().getRefund_pay_type();
		//累计付款方账户出账退回金额
		Long sumRefundedPayAccountOutAccountingAmt = AmountUtils.ifNullOrElse(this.originalTradeSinglePayment.getSumRefundedPayAccountOutAccountingAmt());
		//付款方账户退回入账金额
		long refund_pay_account_in_accounting_amt = str2Long(tradeRquest.getBody().getRefund_pay_account_in_accounting_amt());
		//付款方账户出账金额
		Long payAccountOutAccountingAmt = AmountUtils.ifNullOrElse(this.originalTradeSinglePayment.getPayAccountOutAccountingAmt());
		
		if(TradeConstants.TRADE_REFUND_PAY_ACCOUNT.equals(refund_pay_type)||TradeConstants.TRADE_REFUND_PAY_LOSS_ACCOUNT.equals(refund_pay_type)){
			long result = sumRefundedPayAccountOutAccountingAmt+refund_pay_account_in_accounting_amt-payAccountOutAccountingAmt;
			if(result>0){
				LOGGER.info("原交易【累计付款方账户出账退回金额】+输入【付款方账户退回入账金额】>原交易【付款方账户出账金额】");
				throw exceptionService.buildBusinessException("JY00060020011001200");
			}
		}
		//累计付款方台账退回金额
		Long sumRefundedPayLedgerAmt = AmountUtils.ifNullOrElse(this.originalTradeSinglePayment.getSumRefundedPayLedgerAmt());
		//付款方损益退回金额
		long refund_pay_profit_loss_amt = str2Long(tradeRquest.getBody().getRefund_pay_profit_loss_amt());
		//付款方损益收入减少金额
		Long payProfitLossIncomeDecr = AmountUtils.ifNullOrElse(this.originalTradeSinglePayment.getPayProfitLossIncomeDecr());
		//付款方损益成本费用增加金额
		long payProfitLossCostIncr =  AmountUtils.ifNullOrElse(this.originalTradeSinglePayment.getPayProfitLossCostIncr());
		if(TradeConstants.TRADE_REFUND_PAY_LOSS_ACCOUNT.equals(refund_pay_type)||TradeConstants.TRADE_REFUND_PAY_LOSS.equals(refund_pay_type)){
			long result = sumRefundedPayLedgerAmt+refund_pay_profit_loss_amt-payProfitLossIncomeDecr-payProfitLossCostIncr;
			if(result>0){
				LOGGER.info("原交易【累计付款方台账退回金额】+输入【付款方损益退回金额】>原交易【付款方损益收入减少金额】+【付款方损益成本费用增加金额】");
				throw exceptionService.buildBusinessException("JY00060020011001200");
			}
		}
	}
	
	/**
	 * 创建在事务里 这里空实现
	 */
	@Override
	public void tradeCreate(TradeRequest<TradePacketReqBodyInnerRefund> tradeRquest) throws Exception {
		
	}

	@Override
	public void tradeUpdateForFail(TradeRequest<TradePacketReqBodyInnerRefund> tradeRquest) throws Exception {
		TradeSinglePaymentRefund refund = new TradeSinglePaymentRefund() ; 
		try {
			//合作商户编号
			refund.setPartnerId(tradeRquest.getHead().getPartner_id());
			//交易编号	系统自动产生
			refund.setTradeNo(this.tradeNo);
			//核心商户编号	来自输入
			refund.setCoreMerchantNo(tradeRquest.getHead().getCore_merchant_no());
			//资金池编号	来自输入
			refund.setFundPoolNo(tradeRquest.getHead().getFund_pool_no());
			//原交易编号	来自输入
			refund.setOriginalTradeNo(tradeRquest.getBody().getOriginal_trade_no());
			//交易客户保留字段1~10	来自输入
			refund.setMerchantExtendField1(tradeRquest.getBody().getMerchant_extend_field_1());
			refund.setMerchantExtendField2(tradeRquest.getBody().getMerchant_extend_field_2());
			refund.setMerchantExtendField3(tradeRquest.getBody().getMerchant_extend_field_3());
			refund.setMerchantExtendField4(tradeRquest.getBody().getMerchant_extend_field_4());
			refund.setMerchantExtendField5(tradeRquest.getBody().getMerchant_extend_field_5());
			refund.setMerchantExtendField6(tradeRquest.getBody().getMerchant_extend_field_6());
			refund.setMerchantExtendField7(tradeRquest.getBody().getMerchant_extend_field_7());
			refund.setMerchantExtendField8(tradeRquest.getBody().getMerchant_extend_field_8());
			refund.setMerchantExtendField9(tradeRquest.getBody().getMerchant_extend_field_9());
			refund.setMerchantExtendField10(tradeRquest.getBody().getMerchant_extend_field_10());
			//交易发起方发起请求编号	来自输入
			refund.setOutTradeNoExt(tradeRquest.getBody().getOut_trade_no_ext());
			//交易发起方业务系统订单号	来自输入
			refund.setOutTradeNo(tradeRquest.getBody().getOut_trade_no());
			//交易创建日期时间	系统时间
			refund.setGmtCreated(gmtCreated);
			//最后变更日期时间	系统时间
			refund.setGmtModifiedLatest(gmtCreated);
			//最后变更交易请求类型	本次请求类型
			refund.setLatestTradeReqType(tradeRquest.getHead().getRequest_code());
			//最后变更交易请求编号	来自交易请求记录
			refund.setLatestReqNo(this.reqNo);
			//交易结束状态	【结束】
			refund.setCloseStatus(TradeConstants.TRADE_CLOSE_STATUS_END);
			//交易状态	【退款失败】
			refund.setStatus(TradeConstants.TRADE_PAYMENT_REFUND_FAIL);
			//交易组别	1【内部支付退款】
			refund.setTradeCatatory(TradeConstants.TRADE_PAYMENT_REFUND_INNER);
			//交易版本	0
			refund.setVersion(0);
			
			//退款失败日期时间	系统时间
			refund.setGmtFailRefund(gmtCreated);
			
			String thisTradePayType = tradeRquest.getBody().getRefund_pay_type();
//			String orig_payType = originalTradeSinglePayment.getPayType();//原交易付出方式
			String orig_receiveType = originalTradeSinglePayment.getReceiveType();//原收款方收到方式
			//账户、损益充退时与【付款方账户退回人账金额】相同；损益直付时与【付款方损益退回收到金额】相同
			if(TradeConstants.TRADE_REFUND_PAY_ACCOUNT.equals(thisTradePayType)||TradeConstants.TRADE_REFUND_PAY_LOSS_ACCOUNT.equals(thisTradePayType)){//账户、损益充退时
				//付款方账户退回人账金额
				refund.setRefundPayAmt(str2Long(tradeRquest.getBody().getRefund_pay_account_in_accounting_amt()));
			}else if(TradeConstants.TRADE_REFUND_PAY_LOSS.equals(thisTradePayType)){//损益直退
				//付款方损益退回收到金额
				refund.setRefundPayAmt(str2Long(tradeRquest.getBody().getRefund_pay_profit_loss_amt()));
			}
			//付款方退回收到方式
			refund.setRefundPayType(tradeRquest.getBody().getRefund_pay_type());
			if(!StringUtils.isEmpty(this.ledger_no_effe_pay_pr_refund)){
//				refund.setRefundPayProfitLossLedgerNo(this.ledger_no_effe_pay_pr_refund);
				long refund_pay_profit_loss_amt = str2Long(tradeRquest.getBody().getRefund_pay_profit_loss_amt());
				refund.setRefundPayProfitLossAmt(refund_pay_profit_loss_amt);
				this.ledger_no_effe_pay_pr_refund=null;
			}
			if(ca_in_account_flag){
				ca_in_account_flag = false;
				refundToCA(tradeRquest, refund);
			}
			
			
			//付款方手续费账户退回账户编号	来自原交易【付款方手续费账户账户编号】
			refund.setRefundPayFeeAccountNo(originalTradeSinglePayment.getPayFeeAccountNo());
			//付款方手续费账户退回所属商户编号	来自原交易【付款方手续费账户所属商户编号】
			refund.setRefundPayFeeAccountMerchantNo(originalTradeSinglePayment.getPayFeeAccountMerchantNo());
			//付款方手续费账户退回入账金额	来自输入【付款方账户手续费退回入账金额】 
			refund.setRefundPayFeeAccountInAccountingAmt(str2Long(tradeRquest.getBody().getRefund_pay_fee_account_in_accounting_amt()));
			//付款方手续费账户退回入账子账户类型	【可用】
			refund.setRefundPayFeeAccountInAccountingType(TradeConstants.TRADE_ACCONUT_AVAILABLE_TYPE);
			//付款方手续费账户退回入账账务历史1~3	来自输入
			refund.setRefundPayFeeAccountInAccountingHis1(tradeRquest.getBody().getRefund_pay_fee_account_in_accounting_his1());
			refund.setRefundPayFeeAccountInAccountingHis2(tradeRquest.getBody().getRefund_pay_fee_account_in_accounting_his2());
			refund.setRefundPayFeeAccountInAccountingHis3(tradeRquest.getBody().getRefund_pay_fee_account_in_accounting_his3());			
			
			//收款方退回付出金额	来自输入
			refund.setRefundReceiveAmt(str2Long(tradeRquest.getBody().getRefund_receive_amt()));
			//收款方退回付出方式	
			refund.setRefundReceiveType(originalTradeSinglePayment.getRefundPayType());
			
			if(TradeConstants.RECEIVE_BY_THIRD_PROFIT_LOSS.equals(orig_receiveType)){
				//收款方损益退回业务台账编号	来自台账
//				refund.setRefundReceiveProfitLossLedgerNo(this.refundReceivedLossLedgerNo);
				//收款方损益退回付款金额	来自台账
				refund.setRefundReceiveProfitLossAmt(str2Long(tradeRquest.getBody().getRefund_receive_amt()));
			}
			if(TradeConstants.RECEIVE_BY_THIRD_ACCOUNT.equals(orig_receiveType)){
				//收款方账户退回账户编号	来自原交易【收款方账户账户编号】
				refund.setRefundReceiveAccountNo(originalTradeSinglePayment.getReceiveAccountNo());
				//收款方账户退回所属商户编号	来自原交易【收款方账户所属商户编号】
				refund.setRefundReceiveAccountMerchantNo(originalTradeSinglePayment.getReceiveAccountMerchantNo());
				//收款方账户退回出账金额	来自输入【收款方退回付出金额】
				refund.setRefundReceiveAccountOutAccountingAmt(str2Long(tradeRquest.getBody().getRefund_receive_amt()));
				//收款方账户退回出账账务历史1	来自输入
				refund.setRefundReceiveAccountOutAccountingHis1(tradeRquest.getBody().getRefund_receive_account_out_accounting_his1());
				//收款方账户退回出账账务历史2	来自输入
				refund.setRefundReceiveAccountOutAccountingHis2(tradeRquest.getBody().getRefund_receive_account_out_accounting_his2());
				//收款方账户退回出账账务历史3	来自输入
				refund.setRefundReceiveAccountOutAccountingHis3(tradeRquest.getBody().getRefund_receive_account_out_accounting_his3());
			}
			//收款方手续费部分
			//收款方手续费账户退回账户编号	来自原交易【收款方手续费账户账户编号】
			refund.setRefundReceiveFeeAccountNo(originalTradeSinglePayment.getReceiveFeeAccountNo());
			//收款方手续费账户退回所属商户编号	来自原交易【收款方手续费账户所属商户编号】
			refund.setRefundReceiveFeeAccountMerchantNo(originalTradeSinglePayment.getReceiveFeeAccountMerchantNo());
			//收款方手续费账户退回入账金额	来自输入
			refund.setRefundReceiveFeeAccountInAccountingAmt(str2Long(tradeRquest.getBody().getRefund_receive_fee_account_in_accounting_amt()));
			//收款方手续费账户退回入账子账户类型	来自原交易【收款方手续费账户出账子账户类型】
			refund.setRefundReceiveFeeAccountInAccountingType(originalTradeSinglePayment.getReceiveFeeAccountOutAccountingType());
			//收款方手续费账户退回入账账务历史1	来自输入
			refund.setRefundReceiveFeeAccountInAccountingHis1(tradeRquest.getBody().getRefund_receive_fee_account_in_accounting_his1());
			//收款方手续费账户退回入账账务历史2	来自输入
			refund.setRefundReceiveFeeAccountInAccountingHis2(tradeRquest.getBody().getRefund_receive_fee_account_in_accounting_his2());
			//收款方手续费账户退回入账账务历史3	来自输入
			refund.setRefundReceiveFeeAccountInAccountingHis3(tradeRquest.getBody().getRefund_receive_fee_account_in_accounting_his3());
			 
			 //其他损益台账部分
			 initRefundExtendProfitLoss(tradeRquest.getBody().getProfit_loss_list());
			 refund.setRefundExtendProfitLoss(jsonList);
			 jsonList="";
			 //付出方冻结余额	0
			 refund.setPayFreezeBal(0l);
			 //收到方冻结余额	0
			 refund.setReceiveFreezeBal(0l);
			 paymentRefund = refundService.createInnerTradeForRefundOfFailure(refund);
		} catch (Exception e) {
			jsonList = "";
			e.printStackTrace();
			LOGGER.error("-->创建退款失败交易记录创建失败："+e.getMessage(),e);
			throw exceptionService.buildBusinessException("JY00060020011001100");
		}
		
	}

	@Override
	public void setReqNo(String reqNo) {
		this.reqNo=reqNo;
	}

	@Override
	public Object getTradeDetails() {
		TradeSinglePaymentRefundVo vo = new TradeSinglePaymentRefundVo();
		if(paymentRefund!=null){
			BeanUtils.copyProperties(paymentRefund,vo);
		}
		return vo;
	}
	
	private void initRefundExtendProfitLoss(List<TradePacketReqBodyOtherPofitLossLedger> otherLossLedgerList) throws Exception{
		if(otherLossLedgerList !=null ){
			List<OtherPofitLossLedgerForRefund> otherLossLedgerListForRefund = new ArrayList<OtherPofitLossLedgerForRefund>();
			for(TradePacketReqBodyOtherPofitLossLedger otherPofitLossLedger : otherLossLedgerList){
				OtherPofitLossLedgerForRefund lossLedgerForRefund= new OtherPofitLossLedgerForRefund();
				lossLedgerForRefund.setIncome_incr(otherPofitLossLedger.getIncome_incr());
		        //其他损益台账List退回收入减少金额	来自输入
				lossLedgerForRefund.setIncome_decr(otherPofitLossLedger.getIncome_decr());
		        //其他损益台账List退回成本费用增加金额	来自输入
				lossLedgerForRefund.setCost_incr(otherPofitLossLedger.getCost_incr());
		        //其他损益台账List退回成本费用减少金额	来自输入
				lossLedgerForRefund.setCost_decr(otherPofitLossLedger.getCost_decr());
				otherLossLedgerListForRefund.add(lossLedgerForRefund);
			}
			this.jsonList = ObjectJsonUtil.object2String(otherLossLedgerListForRefund);
		}
	}

	/**
     * @Description: 将字符串转换为数字
     */
    private long str2Long(String str) {
        long parseNum = 0;
        if (!StringUtils.isEmpty(str)) {
            try {
                parseNum = AmountUtils.bizAmountConvertToLong(str);
            } catch (AmountConvertException e) {
                e.printStackTrace();
                LOGGER.error("-->金额转换异常，输入金额：" + str, e);
                throw exceptionService.buildBusinessException("JY000000000000201");
            }
        }
        return parseNum;
    }
}
