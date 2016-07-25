package com.liantuo.trade.bus.process.impl.single_payment;

import com.alibaba.druid.util.StringUtils;
import com.liantuo.deposit.advanceaccount.bus.service.AdvanceAccountService;
import com.liantuo.deposit.advanceaccount.bus.vo.AdvanceAccountVO;
import com.liantuo.deposit.advanceaccount.bus.vo.RealTimeAccountingRsp;
import com.liantuo.deposit.advanceaccount.bus.vo.RealTimeAccountingVo;
import com.liantuo.trade.bus.process.TradeModSingleTxHasPaymentInterface;
import com.liantuo.trade.bus.service.BizAccountService;
import com.liantuo.trade.bus.service.ExceptionService;
import com.liantuo.trade.bus.service.ProfitLossLedgerService;
import com.liantuo.trade.bus.service.SinglePaymentService;
import com.liantuo.trade.bus.template.impl.v1_1.mod.ATradeModSingleTxHasPaymentTemp;
import com.liantuo.trade.bus.vo.RealTimeAccountVO;
import com.liantuo.trade.client.trade.packet.TradeRequest;
import com.liantuo.trade.client.trade.packet.body.TradePacketReqBodyOtherPofitLossLedger;
import com.liantuo.trade.client.trade.packet.body.single_payment.TradePacketReqBodyInnerPayment;
import com.liantuo.trade.client.trade.packet.head.TradePacketHead;
import com.liantuo.trade.common.constants.TradeConstants;
import com.liantuo.trade.common.util.amount.AmountUtils;
import com.liantuo.trade.common.util.json.ObjectJsonUtil;
import com.liantuo.trade.common.util.trade.TradeCommonValidation;
import com.liantuo.trade.common.util.trade.TradeUtilCommon;
import com.liantuo.trade.common.validate.TradeValidationUtil;
import com.liantuo.trade.exception.AmountConvertException;
import com.liantuo.trade.exception.BusinessException;
import com.liantuo.trade.orm.pojo.TradeProfitLossLedger;
import com.liantuo.trade.orm.pojo.TradeSinglePayment;
import com.liantuo.trade.orm.pojo.TradeSinglePaymentExample;
import com.liantuo.trade.orm.pojo.subvo.TradeSinglePaymentVo;
import com.liantuo.trade.seqno.IdFactory;
import com.liantuo.trade.spring.annotation.JobFlow;
import org.apache.commons.lang.reflect.FieldUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 内部支付
 */
@JobFlow(value = "0006_001_001", version = "1.0", template = ATradeModSingleTxHasPaymentTemp.class)
public class SingleTradeInnerPaymentPayProcess implements TradeModSingleTxHasPaymentInterface<TradePacketReqBodyInnerPayment> {
    private static Logger logger = LoggerFactory.getLogger(SingleTradeInnerPaymentPayProcess.class);

    @Autowired
    private ExceptionService exceptionService;
    @Resource(name = "singlePaymentServiceImpl")
    private SinglePaymentService singlePaymentServiceImpl;
    @Resource(name = "advanceAccountServiceImpl")
    private AdvanceAccountService advanceAccountService;
    @Resource(name = "profitLossLedgerServiceImpl")
    private ProfitLossLedgerService profitLossLedgerService;    //损益台账service
    @Resource(name = "idFactorySinglePaymentTradeNo")
    private IdFactory idFactorySinglePaymentTradeNo;
    @Resource(name = "bizAccountServiceImpl")
    private BizAccountService bizAccountService;

    private String payMerchantNo;
    private String payFeeMerchantNo;
    private String receiveMerchantNo;
    private String receiveFeeMerchantNo;
    private String reqNo;
    private String tradeNo;
    private String paymentLedgerNo;
    private String receiveLedgerNo;
    private TradeSinglePayment singlePayment;
    private List<TradeProfitLossLedger> otherProfitList = new ArrayList<TradeProfitLossLedger>();

    @Override
    public void formatValidate(TradeRequest<TradePacketReqBodyInnerPayment> tradeRquest)
            throws Exception {
        String msg = TradeValidationUtil.validateRequestWeak(tradeRquest);
        if (!StringUtils.isEmpty(msg)) {
            logger.info("--> " + msg);
            throw exceptionService.buildBusinessException("JY00060010011000100", msg);
        }

        TradePacketReqBodyInnerPayment body = tradeRquest.getBody();

        //check receive_fee_account_out_accounting_type
        if (!StringUtils.isEmpty(body.getReceive_fee_account_out_accounting_amt())
                && TradeConstants.TRADE_ACCOUNT_FREEZE_TYPE.equals(body.getReceive_account_in_accounting_type())) {
            if (!body.getReceive_fee_account_no().equals(body.getReceive_account_no())
                    && !TradeConstants.TRADE_ACCONUT_AVAILABLE_TYPE.equals(body.getReceive_fee_account_out_accounting_type())) {
                logger.info("--> " + "收款方手续费账户出账子账户类型不正确");
                throw exceptionService.buildBusinessException("JY00060010011000100", "收款方手续费账户出账子账户类型不正确");
            }
        } else if (!StringUtils.isEmpty(body.getReceive_fee_account_out_accounting_amt())
                && TradeConstants.TRADE_ACCONUT_AVAILABLE_TYPE.equals(body.getReceive_account_in_accounting_type())) {
            if (!TradeConstants.TRADE_ACCONUT_AVAILABLE_TYPE.equals(body.getReceive_fee_account_out_accounting_type())) {
                logger.info("--> " + "收款方手续费账户出账子账户类型不正确");
                throw exceptionService.buildBusinessException("JY00060010011000100", "收款方手续费账户出账子账户类型不正确");
            }
        }

        //check receive_fee_account_out_accounting_amt receive_account_in_accounting_amt
        if (TradeConstants.RECEIVE_BY_THIRD_ACCOUNT.equals(body.getReceive_type())
                && !StringUtils.isEmpty(body.getReceive_fee_account_out_accounting_amt())) {
            if (!compareAmt(body.getReceive_fee_account_out_accounting_amt(), body.getReceive_account_in_accounting_amt())) {
                logger.info("--> " + "收款方手续费账户出账金额需要小于或等于收款方收到金额");
                throw exceptionService.buildBusinessException("JY00060010011000100", "收款方手续费账户出账金额需要小于或等于收款方收到金额");
            }
        } else if (TradeConstants.RECEIVE_BY_THIRD_PROFIT_LOSS.equals(body.getReceive_type())
                && !StringUtils.isEmpty(body.getReceive_fee_account_out_accounting_amt())) {
            if (!StringUtils.isEmpty(body.getReceive_profit_loss_income_incr())
                    && !compareAmt(body.getReceive_fee_account_out_accounting_amt(), body.getReceive_profit_loss_income_incr())) {
                logger.info("--> " + "收款方手续费账户出账金额需要小于或等于收款方收到金额");
                throw exceptionService.buildBusinessException("JY00060010011000100", "收款方手续费账户出账金额需要小于或等于收款方收到金额");
            } else if (!StringUtils.isEmpty(body.getReceive_profit_loss_cost_decr())
                    && !compareAmt(body.getReceive_fee_account_out_accounting_amt(), body.getReceive_profit_loss_cost_decr())) {
                logger.info("--> " + "收款方手续费账户出账金额需要小于或等于收款方收到金额");
                throw exceptionService.buildBusinessException("JY00060010011000100", "收款方手续费账户出账金额需要小于或等于收款方收到金额");
            }
        }

    }

    @Override
    public void bizValidate(TradeRequest<TradePacketReqBodyInnerPayment> tradeRquest)
            throws Exception {
    	 //校验【合作商户编号】【核心商户编号】、【资金池编号】对应关系
        TradeCommonValidation.validateCoreMerchantNoAndFundPoolNo(tradeRquest, true, exceptionService.buildBusinessException("JY00060010011000200"));
        //校验订单号是否重复
        checkDuplicate(tradeRquest);
        //交易各CA账户与【核心商户编号】、【资金池编号】、【所属成员商户编号】对应关系
        checkAccount(tradeRquest);
        //校验金额关系
        checkAmount(tradeRquest.getBody());
        
      }

    /**
     * @Description: 金额比较
     */
    private boolean compareAmt(String receiveFeeAccountOutAccountingAmt, String receiveAccountInAccountingAmt) {
        long receive_fee_amt = strParseToLong(receiveFeeAccountOutAccountingAmt);
        long receive_account_amt = strParseToLong(receiveAccountInAccountingAmt);
        return receive_fee_amt <= receive_account_amt;
    }

    /**
     * 金额大小比较
     *
     * @param param1
     * @param param2
     * @return
     */
    private int compare(String param1, String param2) {
        long receive_fee_amt = strParseToLong(param1);
        long receive_account_amt = strParseToLong(param2);
        return new Long(receive_fee_amt).compareTo(new Long(receive_account_amt));
    }

    private void checkAccount(TradeRequest<TradePacketReqBodyInnerPayment> tradeRquest) {

        //校验【付款方账户账户编号】
        if (!StringUtils.isEmpty(tradeRquest.getBody().getPay_account_no())) {
            AdvanceAccountVO accountVo = getCreditAccountInfo(tradeRquest.getBody().getPay_account_no());
            if (accountVo == null || !(tradeRquest.getHead().getCore_merchant_no().equals(accountVo.getCoreMerchantNo()) && tradeRquest.getHead().getFund_pool_no().equals(accountVo.getPoolNo()))) {
                logger.info("--> " + "【付款方账户账户编号】与【核心商户编号】、【资金池编号】、【所属成员商户编号】 不对应");
                throw exceptionService.buildBusinessException("JY00060010011000500");
            } else {
                payMerchantNo = accountVo.getMerchantNo();
            }
        }
        //校验【付款方手续费账户账户编号】
        if (!StringUtils.isEmpty(tradeRquest.getBody().getPay_fee_account_no())) {
            AdvanceAccountVO accountVo = getCreditAccountInfo(tradeRquest.getBody().getPay_fee_account_no());
            if (accountVo == null || !(tradeRquest.getHead().getCore_merchant_no().equals(accountVo.getCoreMerchantNo()) && tradeRquest.getHead().getFund_pool_no().equals(accountVo.getPoolNo()))) {
                logger.info("--> " + "【付款方手续费账户账户编号】与【核心商户编号】、【资金池编号】、【所属成员商户编号】 不对应");
                throw exceptionService.buildBusinessException("JY00060010011000600");
            } else {
                payFeeMerchantNo = accountVo.getMerchantNo();
            }
        }
        //校验【收款方账户账户编号】
        if (!StringUtils.isEmpty(tradeRquest.getBody().getReceive_account_no())) {
            AdvanceAccountVO accountVo = getCreditAccountInfo(tradeRquest.getBody().getReceive_account_no());
            if (accountVo == null || !(tradeRquest.getHead().getCore_merchant_no().equals(accountVo.getCoreMerchantNo()) && tradeRquest.getHead().getFund_pool_no().equals(accountVo.getPoolNo()))) {
                logger.info("--> " + "【收款方账户账户编号】与【核心商户编号】、【资金池编号】、【所属成员商户编号】 不对应");
                throw exceptionService.buildBusinessException("JY00060010011000700");
            } else {
                receiveMerchantNo = accountVo.getMerchantNo();
            }
        }
        //校验【收款方手续费账户账户编号】
        if (!StringUtils.isEmpty(tradeRquest.getBody().getReceive_fee_account_no())) {
            AdvanceAccountVO accountVo = getCreditAccountInfo(tradeRquest.getBody().getReceive_fee_account_no());
            if (accountVo == null || !(tradeRquest.getHead().getCore_merchant_no().equals(accountVo.getCoreMerchantNo()) && tradeRquest.getHead().getFund_pool_no().equals(accountVo.getPoolNo()))) {
                logger.info("--> " + "【收款方手续费账户账户编号】与【核心商户编号】、【资金池编号】、【所属成员商户编号】 不对应");
                throw exceptionService.buildBusinessException("JY00060010011000800");
            } else {
                receiveFeeMerchantNo = accountVo.getMerchantNo();
            }
        }
    }


    /**
     * 账户主体匹配检查
     *
     * @param tradeRquest
     * @param accounts
     * @throws IllegalAccessException
     * @throws NoSuchMethodException
     * @throws InvocationTargetException
     * @throws NoSuchFieldException
     */
    private void checkAccount(TradeRequest<TradePacketReqBodyInnerPayment> tradeRquest, Map<String, String[]> accounts)
            throws IllegalAccessException, NoSuchMethodException, InvocationTargetException, NoSuchFieldException {
        AdvanceAccountVO accountVo;
        String accountNo, coreMerchantNo, fundPoolNo;
        
        for (String account : accounts.keySet()) {
            accountNo = org.apache.commons.beanutils.BeanUtils.getProperty(tradeRquest.getBody(), account);

            if (!StringUtils.isEmpty(accountNo)) {
                accountVo = getCreditAccountInfo(accountNo);

                if (null == accountVo) {
                    logger.info("--> " + "【付款方账户账户编号】与【核心商户编号】、【资金池编号】、【所属成员商户编号】 不对应");
                    throw exceptionService.buildBusinessException(accounts.get(account)[0]);
                } else {
                    coreMerchantNo = org.apache.commons.beanutils.BeanUtils.getProperty(tradeRquest.getHead(), "core_merchant_no");
                    fundPoolNo = org.apache.commons.beanutils.BeanUtils.getProperty(tradeRquest.getHead(), "fund_pool_no");
                    if (!(coreMerchantNo.equals(accountVo.getCoreMerchantNo()) && fundPoolNo.equals(accountVo.getPoolNo()))) {
                        logger.info("--> " + "【付款方账户账户编号】与【核心商户编号】、【资金池编号】、【所属成员商户编号】 不对应");
                        throw exceptionService.buildBusinessException(accounts.get(account)[0]);
                    }

                    FieldUtils.writeDeclaredField(this, accounts.get(account)[1], accountVo.getMerchantNo());
                }
            }
        }
    }


    private void checkAmount(TradePacketReqBodyInnerPayment payment) {
        long innerAmonut = 0;
        long outerAmount = 0;

        if (!(TradeConstants.PAY_BY_ACCOUNT.equals(payment.getPay_type())
                || TradeConstants.PAY_BY_PROFIT_LOSS_ACCOUNT.equals(payment.getPay_type()))) {
            innerAmonut += strParseToLong(payment.getPay_profit_loss_income_decr());
            innerAmonut += strParseToLong(payment.getPay_profit_loss_cost_incr());
        }

        if (!TradeConstants.PAY_BY_PROFIT_LOSS.equals(payment.getPay_type())) {
            innerAmonut += strParseToLong(payment.getPay_account_out_accounting_amt());
        }

        innerAmonut = innerAmonut + strParseToLong(payment.getPay_fee_account_out_accounting_amt()) +
                strParseToLong(payment.getReceive_fee_account_out_accounting_amt());

        outerAmount += strParseToLong(payment.getReceive_account_in_accounting_amt()) +
                strParseToLong(payment.getReceive_profit_loss_income_incr()) +
                strParseToLong(payment.getReceive_profit_loss_cost_decr());

        if (null != payment.getProfit_loss_list()) {
            for (TradePacketReqBodyOtherPofitLossLedger otherProfitLossLedger : payment.getProfit_loss_list()) {
                innerAmonut += strParseToLong(otherProfitLossLedger.getIncome_decr());//计算其他损益台账正向金额
                innerAmonut += strParseToLong(otherProfitLossLedger.getCost_incr());//计算其他损益台账正向金额
                outerAmount += strParseToLong(otherProfitLossLedger.getIncome_incr());//计算其他损益台账负向金额
                outerAmount += strParseToLong(otherProfitLossLedger.getCost_decr());//计算其他损益台账负向金额
            }
        }

        if (outerAmount != innerAmonut) {
            logger.info("--> " + "【正负向金额】 不对应");
            throw exceptionService.buildBusinessException("JY00060010011000900");
        }

    }

    /**
     * @Description: 将字符串转换为数字
     */
    private long strParseToLong(String str) {
        long parseNum = 0;
        if (!StringUtils.isEmpty(str)) {
            try {
                parseNum = AmountUtils.bizAmountConvertToLong(str);
            } catch (AmountConvertException e) {
                throw exceptionService.buildBusinessException("JY000000000000201");
            }
        }
        return parseNum;
    }

    private void checkDuplicate(TradeRequest<TradePacketReqBodyInnerPayment> tradeRquest) {
        TradeSinglePaymentExample tradeRechargeExample = new TradeSinglePaymentExample();
        tradeRechargeExample.createCriteria().andFundPoolNoEqualTo(tradeRquest.getHead().getFund_pool_no()).andOutTradeNoExtEqualTo(tradeRquest.getBody().getOut_trade_no_ext());
        List<TradeSinglePayment> list = singlePaymentServiceImpl.queryByExample(tradeRechargeExample);
        if (!CollectionUtils.isEmpty(list)) {
            throw exceptionService.buildBusinessException("JY00060010011000400");
        }

    }

    /**
     * 验证【核心商户编号】、【资金池编号】与【CA账户编号】对应关系
     *
     * @param //coreMerchantNo 核心商户编号
     * @param //fundPoolNo     资金池编号
     * @param accountNo        CA账户编号
     * @Title:correspond
     * @Description:验证【核心商户编号】、【资金池编号】与【CA账户编号】对应关系
     * @return:void
     * @author:zoran.huang
     * @date:2016年5月6日 上午10:12:10
     */
    private AdvanceAccountVO getCreditAccountInfo(String accountNo) {
        return advanceAccountService.selectByAccountNo(accountNo);
    }

    @Override
    public void transaction(TradeRequest<TradePacketReqBodyInnerPayment> tradeRquest)
            throws Exception {
        tradeNo = idFactorySinglePaymentTradeNo.generate().toString();

        //创建生效付款损益台账
        if (TradeConstants.PAY_BY_PROFIT_LOSS.equals(tradeRquest.getBody().getPay_type())
                || TradeConstants.PAY_BY_PROFIT_LOSS_ACCOUNT.equals(tradeRquest.getBody().getPay_type())) {
            createEffectPaymentProfitLedger(tradeRquest);
        }

        //执行账务操作
        handlerRealAcount(tradeRquest);

        //收款方收到方式为损益，则创建生效收款损益台账
        if (TradeConstants.RECEIVE_BY_THIRD_PROFIT_LOSS.equals(tradeRquest.getBody().getReceive_type())) {
            createEffectReceiveProfitLedger(tradeRquest);
        }

        //创建生效其他损益台账
        if (!CollectionUtils.isEmpty(tradeRquest.getBody().getProfit_loss_list())) {
            List<TradePacketReqBodyOtherPofitLossLedger> list = tradeRquest.getBody().getProfit_loss_list();
            for (TradePacketReqBodyOtherPofitLossLedger profitLoss : list) {
                createEffectOtherProfitLedger(tradeRquest, profitLoss);
            }
        }

        //创建成功支付交易记录
        createSinglePaymentSuccess(tradeRquest);

    }

    private void handlerRealAcount(TradeRequest<TradePacketReqBodyInnerPayment> tradeRquest) {
        TradePacketReqBodyInnerPayment body = tradeRquest.getBody();
        List<RealTimeAccountingVo> request_list = new ArrayList<RealTimeAccountingVo>();

        // 【付款方付出方式】为【损益充付】时,执行付款方充付入账
        if (TradeConstants.PAY_BY_PROFIT_LOSS_ACCOUNT.equals(body.getPay_type())) {
            request_list.add(paymentInRealAccount(tradeRquest));
        }

        // 【付款方付出方式】为【账户】或【损益充付】时，执行付款方出账处理
        if (TradeConstants.PAY_BY_ACCOUNT.equals(body.getPay_type()) || TradeConstants.PAY_BY_PROFIT_LOSS_ACCOUNT.equals(body.getPay_type())) {
            request_list.add(paymentOutRealAccount(tradeRquest));
        }
        // 【付款方手续费账户出账金额】不为空，执行付款方手续费出账处理
        if (!StringUtils.isEmpty(body.getPay_fee_account_out_accounting_amt())) {
            request_list.add(paymentFeeOutRealAccount(tradeRquest));
        }

        // 【收款方收到方式】为【账户】时，执行收款方账户入账处理
        if (TradeConstants.RECEIVE_BY_THIRD_ACCOUNT.equals(body.getReceive_type())) {
            request_list.add(receiveInRealAccount(tradeRquest));
        }

        // 【收款方手续费账户出账金额】不为空，执行收款方手续费账户出账处理
        if (!StringUtils.isEmpty(body.getReceive_fee_account_out_accounting_amt())) {
            request_list.add(receiveFeeOutRealAccount(tradeRquest));
        }

        try {
            List<RealTimeAccountingRsp> list = this.bizAccountService.senderRequestToAccount(request_list);
            RealTimeAccountingRsp rsp;
            for (RealTimeAccountingRsp aList : list) {
                rsp = aList;
                if ("F".equals(rsp.getSuccess())) {
                    throw new BusinessException(rsp.getRetCode(), rsp.getErrMessage());
                }
            }
        } catch (BusinessException e) {
        	e.printStackTrace();
            throw new BusinessException(e.getErrorCode(), e.getErrorMessage());
        } catch (Exception e) {
        	e.printStackTrace();
            throw exceptionService.buildBusinessException("JY000000000000401");
        }
    }

    private RealTimeAccountingVo paymentInRealAccount(TradeRequest<TradePacketReqBodyInnerPayment> tradeRquest) {
        TradePacketReqBodyInnerPayment body = tradeRquest.getBody();
        RealTimeAccountVO accountvo = new RealTimeAccountVO();

        accountvo.setCoreMerchantNo(tradeRquest.getHead().getCore_merchant_no()); // 核心商户编号
        accountvo.setAccountNo(body.getPay_account_no());
        accountvo.setPoolNo(tradeRquest.getHead().getFund_pool_no()); // 资金池编号
        accountvo.setReservedFields1(body.getPay_account_in_accounting_his1());
        accountvo.setReservedFields2(body.getPay_account_in_accounting_his2());
        accountvo.setReservedFields3(body.getPay_account_in_accounting_his3());

        accountvo.setSequenceNo(body.getOut_trade_no()); // 本交易业务系统订单号
        accountvo.setTradeCode(TradeUtilCommon.getTradeType(TradeConstants.TRADE_INNER_PAYMENT)); // 交易类型0006_001
        accountvo.setTradeNo(tradeNo); // 本交易编号
        accountvo.setTradeGmtCreated(new Date()); // 本交易创建时间
        accountvo.setTradeReqCode(TradeConstants.TRADE_INNER_PAYMENT); // 交易请求类型0006_001_001
        accountvo.setTradeStepNo(reqNo);// 交易请求编号
        if(!StringUtils.isEmpty(body.getPay_profit_loss_income_decr())) {
            accountvo.setAmount(strParseToLong(body.getPay_profit_loss_income_decr()));
        } else {
            accountvo.setAmount(strParseToLong(body.getPay_profit_loss_cost_incr()));
        }
        return bizAccountService.avlBalIncrWrapper(accountvo);
    }

    private RealTimeAccountingVo paymentOutRealAccount(TradeRequest<TradePacketReqBodyInnerPayment> tradeRquest) {
        TradePacketReqBodyInnerPayment body = tradeRquest.getBody();
        RealTimeAccountVO accountvo = new RealTimeAccountVO();

        accountvo.setCoreMerchantNo(tradeRquest.getHead().getCore_merchant_no()); // 核心商户编号
        accountvo.setAccountNo(body.getPay_account_no());
        accountvo.setPoolNo(tradeRquest.getHead().getFund_pool_no()); // 资金池编号
        accountvo.setReservedFields1(body.getPay_account_out_accounting_his1());
        accountvo.setReservedFields2(body.getPay_account_out_accounting_his2());
        accountvo.setReservedFields3(body.getPay_account_out_accounting_his3());
        accountvo.setSequenceNo(body.getOut_trade_no()); // 本交易业务系统订单号
        accountvo.setTradeCode(TradeUtilCommon.getTradeType(TradeConstants.TRADE_INNER_PAYMENT)); // 交易类型0006_001
        accountvo.setTradeNo(tradeNo); // 本交易编号
        accountvo.setTradeGmtCreated(new Date()); // 本交易创建时间
        accountvo.setTradeReqCode(TradeConstants.TRADE_INNER_PAYMENT); // 交易请求类型0006_001_001
        accountvo.setTradeStepNo(reqNo);// 交易请求编号
        accountvo.setAmount(strParseToLong(body.getPay_account_out_accounting_amt()));

        return bizAccountService.avlBalDecrWrapper(accountvo);
    }

    private RealTimeAccountingVo paymentFeeOutRealAccount(TradeRequest<TradePacketReqBodyInnerPayment> tradeRquest) {
        TradePacketReqBodyInnerPayment body = tradeRquest.getBody();
        RealTimeAccountVO accountvo = new RealTimeAccountVO();

        accountvo.setCoreMerchantNo(tradeRquest.getHead().getCore_merchant_no()); // 核心商户编号
        accountvo.setAccountNo(body.getPay_fee_account_no());
        accountvo.setPoolNo(tradeRquest.getHead().getFund_pool_no()); // 资金池编号
        accountvo.setReservedFields1(body.getPay_fee_account_out_accounting_his1());
        accountvo.setReservedFields2(body.getPay_fee_account_out_accounting_his2());
        accountvo.setReservedFields3(body.getPay_fee_account_out_accounting_his3());
        accountvo.setSequenceNo(body.getOut_trade_no()); // 本交易业务系统订单号
        accountvo.setTradeCode(TradeUtilCommon.getTradeType(TradeConstants.TRADE_INNER_PAYMENT)); // 交易类型0006_001
        accountvo.setTradeNo(tradeNo); // 本交易编号
        accountvo.setTradeGmtCreated(new Date()); // 本交易创建时间
        accountvo.setTradeReqCode(TradeConstants.TRADE_INNER_PAYMENT); // 交易请求类型0006_001_001
        accountvo.setTradeStepNo(reqNo);// 交易请求编号
        accountvo.setAmount(strParseToLong(body.getPay_fee_account_out_accounting_amt()));

        return bizAccountService.avlBalDecrWrapper(accountvo);
    }

    private RealTimeAccountingVo receiveInRealAccount(TradeRequest<TradePacketReqBodyInnerPayment> tradeRquest) {
        TradePacketReqBodyInnerPayment body = tradeRquest.getBody();
        RealTimeAccountVO accountvo = new RealTimeAccountVO();

        accountvo.setCoreMerchantNo(tradeRquest.getHead().getCore_merchant_no()); // 核心商户编号
        accountvo.setAccountNo(body.getReceive_account_no());
        accountvo.setPoolNo(tradeRquest.getHead().getFund_pool_no()); // 资金池编号
        accountvo.setReservedFields1(body.getReceive_account_in_accounting_his1());
        accountvo.setReservedFields2(body.getReceive_account_in_accounting_his2());
        accountvo.setReservedFields3(body.getReceive_account_in_accounting_his3());
        accountvo.setSequenceNo(body.getOut_trade_no()); // 本交易业务系统订单号
        accountvo.setTradeCode(TradeUtilCommon.getTradeType(TradeConstants.TRADE_INNER_PAYMENT)); // 交易类型0006_001
        accountvo.setTradeNo(tradeNo); // 本交易编号
        accountvo.setTradeGmtCreated(new Date()); // 本交易创建时间
        accountvo.setTradeReqCode(TradeConstants.TRADE_INNER_PAYMENT); // 交易请求类型0006_001_001
        accountvo.setTradeStepNo(reqNo);// 交易请求编号

        if (TradeConstants.TRADE_ACCOUNT_FREEZE_TYPE.equals(body.getReceive_account_in_accounting_type())) {
            accountvo.setFrozenTradeCode(TradeUtilCommon.getTradeType(TradeConstants.TRADE_INNER_PAYMENT));
            accountvo.setFrozenTradeNo(tradeNo);
            accountvo.setAmount(strParseToLong(body.getReceive_account_in_accounting_amt()));
            return bizAccountService.FrozenBalIncrAmtWrapper(accountvo);
        } else {
            accountvo.setAmount(strParseToLong(body.getReceive_account_in_accounting_amt()));
            return bizAccountService.avlBalIncrWrapper(accountvo);
        }
    }

    private RealTimeAccountingVo receiveFeeOutRealAccount(TradeRequest<TradePacketReqBodyInnerPayment> tradeRquest) {
        TradePacketReqBodyInnerPayment body = tradeRquest.getBody();
        RealTimeAccountVO accountvo = new RealTimeAccountVO();

        accountvo.setCoreMerchantNo(tradeRquest.getHead().getCore_merchant_no()); // 核心商户编号
        accountvo.setAccountNo(body.getReceive_fee_account_no());
        accountvo.setPoolNo(tradeRquest.getHead().getFund_pool_no()); // 资金池编号
        accountvo.setReservedFields1(body.getReceive_fee_account_out_accounting_his1());
        accountvo.setReservedFields2(body.getReceive_fee_account_out_accounting_his2());
        accountvo.setReservedFields3(body.getReceive_fee_account_out_accounting_his3());
        accountvo.setSequenceNo(body.getOut_trade_no()); // 本交易业务系统订单号
        accountvo.setTradeCode(TradeUtilCommon.getTradeType(TradeConstants.TRADE_INNER_PAYMENT)); // 交易类型0006_001
        accountvo.setTradeNo(tradeNo); // 本交易编号
        accountvo.setTradeGmtCreated(new Date()); // 本交易创建时间
        accountvo.setTradeReqCode(TradeConstants.TRADE_INNER_PAYMENT); // 交易请求类型0006_001_001
        accountvo.setTradeStepNo(reqNo);// 交易请求编号

        if (TradeConstants.TRADE_ACCOUNT_FREEZE_TYPE.equals(body.getReceive_fee_account_out_accounting_type())) {
            accountvo.setFrozenTradeCode(TradeUtilCommon.getTradeType(TradeConstants.TRADE_INNER_PAYMENT));
            accountvo.setFrozenTradeNo(tradeNo);
            accountvo.setAmount(strParseToLong(body.getReceive_fee_account_out_accounting_amt()));
            return bizAccountService.FrozenBalDecrAmtWrapper(accountvo);
        } else {
            accountvo.setAmount(strParseToLong(body.getReceive_fee_account_out_accounting_amt()));
            return bizAccountService.avlBalDecrWrapper(accountvo);
        }
    }

    private void createSinglePaymentSuccess(TradeRequest<TradePacketReqBodyInnerPayment> tradeRquest) throws Exception {
        TradeSinglePayment payment = initSinglePayment(tradeRquest);

        //付款方付出方式：损益充付
        if (TradeConstants.PAY_BY_PROFIT_LOSS_ACCOUNT.equals(tradeRquest.getBody().getPay_type())
                || TradeConstants.PAY_BY_PROFIT_LOSS.equals(tradeRquest.getBody().getPay_type())) {
            payment.setPayProfitLossLedgerNo(paymentLedgerNo);
        }
        //收款方付出方式：损益
        if (TradeConstants.RECEIVE_BY_THIRD_PROFIT_LOSS.equals(tradeRquest.getBody().getReceive_type())) {
            payment.setReceiveProfitLossLedgerNo(receiveLedgerNo);
        }
        //支付成功日期时间
        payment.setGmtSuccessPay(new Date());

        //其他损益台账部分tradeRquest.getBody().getProfit_loss_list()
        payment.setExtendProfitLoss(ObjectJsonUtil.object2String(otherProfitList));

        try {
            singlePaymentServiceImpl.createTradeForPayOfSuccess(payment);
        } catch (Exception e) {
            e.printStackTrace();
            throw exceptionService.buildBusinessException("JY00060010011001000");
        }
        singlePayment = payment;
    }

    private void createEffectOtherProfitLedger(TradeRequest<TradePacketReqBodyInnerPayment> tradeRquest, TradePacketReqBodyOtherPofitLossLedger profitLoss) {
        TradeProfitLossLedger ledger = new TradeProfitLossLedger();
        TradeProfitLossLedger otherLedger = new TradeProfitLossLedger();
        try {
            //核心商户编号
            ledger.setCoreMerchantNo(tradeRquest.getHead().getCore_merchant_no());
            //资金池编号
            ledger.setFundPoolNo(tradeRquest.getHead().getFund_pool_no());
            //业务台账客户保留字段1
            ledger.setMerchantExtendField1(profitLoss.getMerchant_extend_field_1());
            //业务台账客户保留字段2
            ledger.setMerchantExtendField2(profitLoss.getMerchant_extend_field_2());
            //业务台账客户保留字段3
            ledger.setMerchantExtendField3(profitLoss.getMerchant_extend_field_3());
            //所属业务交易类型
            ledger.setTradeType(TradeConstants.TRADE_INNER_PAYMENT);
            //所属业务交易编号
            ledger.setTradeNo(tradeNo);
            //所属业务交易创建日期
            ledger.setGmtTradeCreated(new Date());
            //生效交易请求类型
            ledger.setEffectiveReqType(TradeConstants.TRADE_INNER_PAYMENT);
            //生效交易请求编号
            ledger.setEffectiveReqNo(this.reqNo);
            //交易发起方发起请求编号
            ledger.setOutTradeNoExt(tradeRquest.getBody().getOut_trade_no_ext());
            //交易发起方业务系统订单号
            ledger.setOutTradeNo(tradeRquest.getBody().getOut_trade_no());
            ledger.setProfitLossAttr1(profitLoss.getProfit_loss_attr_1());
            ledger.setProfitLossAttr2(profitLoss.getProfit_loss_attr_2());
            ledger.setProfitLossAttr3(profitLoss.getProfit_loss_attr_3());
            ledger.setProfitLossAttr4(profitLoss.getProfit_loss_attr_4());
            ledger.setProfitLossAttr5(profitLoss.getProfit_loss_attr_5());
            ledger.setProfitLossAttr6(profitLoss.getProfit_loss_attr_6());
            ledger.setProfitLossAttr7(profitLoss.getProfit_loss_attr_7());
            ledger.setProfitLossAttr8(profitLoss.getProfit_loss_attr_8());
            ledger.setProfitLossAttr9(profitLoss.getProfit_loss_attr_9());
            ledger.setProfitLossAttr10(profitLoss.getProfit_loss_attr_10());

            if (!StringUtils.isEmpty(profitLoss.getIncome_incr())) {
                long income_incr = strParseToLong(profitLoss.getIncome_incr());
                ledger.setIncomeIncr(income_incr);
                otherLedger.setIncomeIncr(income_incr);
            }
            if (!StringUtils.isEmpty(profitLoss.getIncome_decr())) {
                long income_decr = strParseToLong(profitLoss.getIncome_decr());
                ledger.setIncomeDecr(income_decr);
                otherLedger.setIncomeDecr(income_decr);
            }
            if (!StringUtils.isEmpty(profitLoss.getCost_incr())) {
                long cost_incr = strParseToLong(profitLoss.getCost_incr());
                ledger.setCostIncr(cost_incr);
                otherLedger.setCostIncr(cost_incr);
            }
            if (!StringUtils.isEmpty(profitLoss.getCost_decr())) {
                long cost_decr = strParseToLong(profitLoss.getCost_decr());
                ledger.setCostDecr(cost_decr);
                otherLedger.setCostDecr(cost_decr);
            }
            profitLossLedgerService.createEffectiveProfitLossLedger(ledger);
            otherLedger.setLedgerNo(ledger.getLedgerNo());
            otherProfitList.add(otherLedger);
        } catch (Exception e) {
            e.printStackTrace();
            logger.info("-->创建生效其他损益台账失败：" + e.getMessage(), e);
            throw exceptionService.buildBusinessException("TZ000200101");
        }
    }

    /**
     * 创建生效收款损益台账
     *
     * @Title:createEffectReceiveProfitLedger
     * @Description: 创建生效收款损益台账
     * @return:void
     * @author:zzd
     */
    private void createEffectReceiveProfitLedger(TradeRequest<TradePacketReqBodyInnerPayment> tradeRquest) {
        TradeProfitLossLedger ledger = new TradeProfitLossLedger();
        TradePacketReqBodyInnerPayment body = tradeRquest.getBody();
        try {
            //核心商户编号
            ledger.setCoreMerchantNo(tradeRquest.getHead().getCore_merchant_no());
            //资金池编号
            ledger.setFundPoolNo(tradeRquest.getHead().getFund_pool_no());
            //业务台账客户保留字段1
            ledger.setMerchantExtendField1(body.getReceive_profit_loss_extend_field_1());
            //业务台账客户保留字段2
            ledger.setMerchantExtendField2(body.getReceive_profit_loss_extend_field_2());
            //业务台账客户保留字段3
            ledger.setMerchantExtendField3(body.getReceive_profit_loss_extend_field_3());
            //所属业务交易类型
            ledger.setTradeType(TradeConstants.TRADE_INNER_PAYMENT);
            //所属业务交易编号
            ledger.setTradeNo(tradeNo);
            //所属业务交易创建日期
            ledger.setGmtTradeCreated(new Date());
//             //生效交易请求类型
            ledger.setEffectiveReqType(TradeConstants.TRADE_INNER_PAYMENT);
//             //生效交易请求编号
            ledger.setEffectiveReqNo(this.reqNo);
//             //交易发起方发起请求编号
            ledger.setOutTradeNoExt(body.getOut_trade_no_ext());
//             //交易发起方业务系统订单号
            ledger.setOutTradeNo(body.getOut_trade_no());
            ledger.setProfitLossAttr1(body.getReceive_profit_loss_attr_1());
            ledger.setProfitLossAttr2(body.getReceive_profit_loss_attr_2());
            ledger.setProfitLossAttr3(body.getReceive_profit_loss_attr_3());
            ledger.setProfitLossAttr4(body.getReceive_profit_loss_attr_4());
            ledger.setProfitLossAttr5(body.getReceive_profit_loss_attr_5());
            ledger.setProfitLossAttr6(body.getReceive_profit_loss_attr_6());
            ledger.setProfitLossAttr7(body.getReceive_profit_loss_attr_7());
            ledger.setProfitLossAttr8(body.getReceive_profit_loss_attr_8());
            ledger.setProfitLossAttr9(body.getReceive_profit_loss_attr_9());
            ledger.setProfitLossAttr10(body.getReceive_profit_loss_attr_10());
            if(!StringUtils.isEmpty(body.getReceive_profit_loss_income_incr())) {
                ledger.setIncomeIncr(strParseToLong(body.getReceive_profit_loss_income_incr()));
            } else if(!StringUtils.isEmpty(body.getReceive_profit_loss_cost_decr())) {
                ledger.setCostDecr(strParseToLong(body.getReceive_profit_loss_cost_decr()));
            }
            receiveLedgerNo = profitLossLedgerService.createEffectiveProfitLossLedger(ledger);
        } catch (Exception e) {
            e.printStackTrace();
            logger.info("-->创建生效收款损益台账失败：" + e.getMessage(), e);
            throw exceptionService.buildBusinessException("TZ000200101");
        }

    }

    /**
     * 创建生效付款损益台账
     *
     * @Title:createEffectPaymentProfitLedger
     * @Description: 创建生效付款损益台账
     * @return:void
     * @author:zzd
     */
    private void createEffectPaymentProfitLedger(TradeRequest<TradePacketReqBodyInnerPayment> tradeRquest) {
        TradeProfitLossLedger ledger = new TradeProfitLossLedger();
        TradePacketReqBodyInnerPayment body = tradeRquest.getBody();
        try {
            //核心商户编号
            ledger.setCoreMerchantNo(tradeRquest.getHead().getCore_merchant_no());
            //资金池编号
            ledger.setFundPoolNo(tradeRquest.getHead().getFund_pool_no());
            //业务台账客户保留字段1
            ledger.setMerchantExtendField1(body.getPay_profit_loss_extend_field_1());
            //业务台账客户保留字段2
            ledger.setMerchantExtendField2(body.getPay_profit_loss_extend_field_2());
            //业务台账客户保留字段3
            ledger.setMerchantExtendField3(body.getPay_profit_loss_extend_field_3());
            //所属业务交易类型
            ledger.setTradeType(TradeConstants.TRADE_INNER_PAYMENT);
            //所属业务交易编号
            ledger.setTradeNo(tradeNo);
            //所属业务交易创建日期
            ledger.setGmtTradeCreated(new Date());
            //生效交易请求类型
            ledger.setEffectiveReqType(TradeConstants.TRADE_INNER_PAYMENT);
            //生效交易请求编号
            ledger.setEffectiveReqNo(this.reqNo);
            //交易发起方发起请求编号
            ledger.setOutTradeNoExt(body.getOut_trade_no_ext());
            //交易发起方业务系统订单号
            ledger.setOutTradeNo(body.getOut_trade_no());
            ledger.setGmtEffective(new Date());
            ledger.setProfitLossAttr1(body.getPay_profit_loss_attr_1());
            ledger.setProfitLossAttr2(body.getPay_profit_loss_attr_2());
            ledger.setProfitLossAttr3(body.getPay_profit_loss_attr_3());
            ledger.setProfitLossAttr4(body.getPay_profit_loss_attr_4());
            ledger.setProfitLossAttr5(body.getPay_profit_loss_attr_5());
            ledger.setProfitLossAttr6(body.getPay_profit_loss_attr_6());
            ledger.setProfitLossAttr7(body.getPay_profit_loss_attr_7());
            ledger.setProfitLossAttr8(body.getPay_profit_loss_attr_8());
            ledger.setProfitLossAttr9(body.getPay_profit_loss_attr_9());
            ledger.setProfitLossAttr10(body.getPay_profit_loss_attr_10());

            if (!StringUtils.isEmpty(body.getPay_profit_loss_income_decr())) {
                ledger.setIncomeDecr(strParseToLong(body.getPay_profit_loss_income_decr()));
            } else if (!StringUtils.isEmpty(body.getPay_profit_loss_cost_incr())) {
                ledger.setCostIncr(strParseToLong(body.getPay_profit_loss_cost_incr()));
            }
            paymentLedgerNo = profitLossLedgerService.createEffectiveProfitLossLedger(ledger);
        } catch (Exception e) {
            e.printStackTrace();
            logger.info("-->创建生效付款损益台账失败：" + e.getMessage(), e);
            throw exceptionService.buildBusinessException("TZ000200101");
        }
    }


    @Override
    public Object getTradeDetails() {
        TradeSinglePaymentVo paymentVo = new TradeSinglePaymentVo();
        if (singlePayment != null) {
            BeanUtils.copyProperties(singlePayment, paymentVo);
            return paymentVo;
        }
        return null;
    }

    @Override
    public void setReqNo(String reqNo) {
        this.reqNo = reqNo;
    }

    @Override
    public void createTradeFailRecode(TradeRequest<TradePacketReqBodyInnerPayment> tradeRquest) throws Exception {
        TradeSinglePayment payment = initSinglePayment(tradeRquest);
        //其他损益台账部分
        List<TradePacketReqBodyOtherPofitLossLedger> profit_loss_list = tradeRquest.getBody().getProfit_loss_list();
        if (!CollectionUtils.isEmpty(profit_loss_list)) {
            for (TradePacketReqBodyOtherPofitLossLedger profitLoss : profit_loss_list) {
                if (!StringUtils.isEmpty(profitLoss.getIncome_incr())) {
                    profitLoss.setIncome_incr(String.valueOf(strParseToLong(profitLoss.getIncome_incr())));
                }
                if (!StringUtils.isEmpty(profitLoss.getIncome_decr())) {
                    profitLoss.setIncome_decr(String.valueOf(strParseToLong(profitLoss.getIncome_decr())));
                }
                if (!StringUtils.isEmpty(profitLoss.getCost_incr())) {
                    profitLoss.setCost_incr(String.valueOf(strParseToLong(profitLoss.getCost_incr())));
                }
                if (!StringUtils.isEmpty(profitLoss.getCost_decr())) {
                    profitLoss.setCost_decr(String.valueOf(strParseToLong(profitLoss.getCost_decr())));
                }
            }
        }

        payment.setExtendProfitLoss(ObjectJsonUtil.object2String(profit_loss_list));
        payment.setGmtFailPay(new Date());
        TradeSinglePayment _singlePayment;
        try {
            _singlePayment = singlePaymentServiceImpl.createTradeFailureRecode(payment);
        } catch (Exception e) {
            e.printStackTrace();
            throw exceptionService.buildBusinessException("JY00060010011001100");
        }

        singlePayment = _singlePayment;
    }

    private TradeSinglePayment initSinglePayment(TradeRequest<TradePacketReqBodyInnerPayment> tradeRquest) throws Exception {
        TradeSinglePayment payment = new TradeSinglePayment();

        TradePacketHead head = tradeRquest.getHead();
        TradePacketReqBodyInnerPayment body = tradeRquest.getBody();

        payment.setCoreMerchantNo(head.getCore_merchant_no());
        payment.setFundPoolNo(head.getFund_pool_no());
        payment.setPartnerId(head.getPartner_id());
        payment.setTradeNo(tradeNo);
        payment.setMerchantExtendField1(body.getMerchant_extend_field_1());
        payment.setMerchantExtendField2(body.getMerchant_extend_field_2());
        payment.setMerchantExtendField3(body.getMerchant_extend_field_3());
        payment.setMerchantExtendField4(body.getMerchant_extend_field_4());
        payment.setMerchantExtendField5(body.getMerchant_extend_field_5());
        payment.setMerchantExtendField6(body.getMerchant_extend_field_6());
        payment.setMerchantExtendField7(body.getMerchant_extend_field_7());
        payment.setMerchantExtendField8(body.getMerchant_extend_field_8());
        payment.setMerchantExtendField9(body.getMerchant_extend_field_9());
        payment.setMerchantExtendField10(body.getMerchant_extend_field_10());
        payment.setOutTradeNoExt(body.getOut_trade_no_ext());
        payment.setOutTradeNo(body.getOut_trade_no());
        payment.setLatestTradeReqType(TradeConstants.TRADE_INNER_PAYMENT);
        payment.setLatestReqNo(this.reqNo);

        if (TradeConstants.PAY_BY_ACCOUNT.equals(body.getPay_type()) || TradeConstants.PAY_BY_PROFIT_LOSS_ACCOUNT.equals(body.getPay_type())) {
            payment.setPayAmt(strParseToLong(body.getPay_account_out_accounting_amt()));
            payment.setPayAccountNo(body.getPay_account_no());
            payment.setPayAccountMerchantNo(payMerchantNo);
            payment.setPayAccountOutAccountingAmt(strParseToLong(body.getPay_account_out_accounting_amt()));
            payment.setPayAccountOutAccountingType(TradeConstants.TRADE_ACCONUT_AVAILABLE_TYPE);
            payment.setPayAccountOutAccountingHis1(body.getPay_account_out_accounting_his1());
            payment.setPayAccountOutAccountingHis2(body.getPay_account_out_accounting_his2());
            payment.setPayAccountOutAccountingHis3(body.getPay_account_out_accounting_his3());
            if (TradeConstants.PAY_BY_PROFIT_LOSS_ACCOUNT.equals(body.getPay_type())) {//损益充付
                payment.setPayAccountInAccountingType(TradeConstants.TRADE_ACCONUT_AVAILABLE_TYPE);
                payment.setPayAccountInAccountingHis1(body.getPay_account_in_accounting_his1());
                payment.setPayAccountInAccountingHis2(body.getPay_account_in_accounting_his2());
                payment.setPayAccountInAccountingHis3(body.getPay_account_in_accounting_his3());
            }
        } else if (TradeConstants.PAY_BY_PROFIT_LOSS.equals(body.getPay_type())) {
            if (!StringUtils.isEmpty(body.getPay_profit_loss_income_decr())) {
                payment.setPayAmt(strParseToLong(body.getPay_profit_loss_income_decr()));
            } else if (!StringUtils.isEmpty(body.getPay_profit_loss_cost_incr())) {
                payment.setPayAmt(strParseToLong(body.getPay_profit_loss_cost_incr()));
            }
        }
        payment.setPayType(body.getPay_type());
        if (TradeConstants.PAY_BY_PROFIT_LOSS_ACCOUNT.equals(body.getPay_type()) || TradeConstants.PAY_BY_PROFIT_LOSS.equals(body.getPay_type())) {
            if (!StringUtils.isEmpty(body.getPay_profit_loss_income_decr())) {
                payment.setPayProfitLossIncomeDecr(strParseToLong(body.getPay_profit_loss_income_decr()));
            } else if (!StringUtils.isEmpty(body.getPay_profit_loss_cost_incr())) {
                payment.setPayProfitLossCostIncr(strParseToLong(body.getPay_profit_loss_cost_incr()));
            }
        }

        //付款方手续费部分
        payment.setPayFeeAccountNo(body.getPay_fee_account_no());
        payment.setPayFeeAccountMerchantNo(payFeeMerchantNo);
        payment.setPayFeeAccountOutAccountingAmt(strParseToLong(body.getPay_fee_account_out_accounting_amt()));
        payment.setPayFeeAccountOutAccountingType(TradeConstants.TRADE_ACCONUT_AVAILABLE_TYPE);
        payment.setPayFeeAccountOutAccountingHis1(body.getPay_fee_account_out_accounting_his1());
        payment.setPayFeeAccountOutAccountingHis2(body.getPay_fee_account_out_accounting_his2());
        payment.setPayFeeAccountOutAccountingHis3(body.getPay_fee_account_out_accounting_his3());

        //收到方基本部分
        payment.setReceiveType(body.getReceive_type());
        if (TradeConstants.RECEIVE_BY_THIRD_ACCOUNT.equals(body.getReceive_type())) {
            payment.setReceiveAmt(strParseToLong(body.getReceive_account_in_accounting_amt()));
            payment.setReceiveAccountNo(body.getReceive_account_no());
            payment.setReceiveAccountMerchantNo(receiveMerchantNo);
            payment.setReceiveAccountInAccountingAmt(strParseToLong(body.getReceive_account_in_accounting_amt()));
            payment.setReceiveAccountInAccountingType(body.getReceive_account_in_accounting_type());
            payment.setReceiveAccountInAccountingHis1(body.getReceive_account_in_accounting_his1());
            payment.setReceiveAccountInAccountingHis2(body.getReceive_account_in_accounting_his2());
            payment.setReceiveAccountInAccountingHis3(body.getReceive_account_in_accounting_his3());
        } else if (TradeConstants.RECEIVE_BY_THIRD_PROFIT_LOSS.equals(body.getReceive_type())) {//如果是损益，则与台账收入增加相同
            if (!StringUtils.isEmpty(body.getReceive_profit_loss_income_incr())) {
                payment.setReceiveAmt(strParseToLong(body.getReceive_profit_loss_income_incr()));
                payment.setReceiveProfitLossIncomeIncr(strParseToLong(body.getReceive_profit_loss_income_incr()));
            } else if (!StringUtils.isEmpty(body.getReceive_profit_loss_cost_decr())) {
                payment.setReceiveAmt(strParseToLong(body.getReceive_profit_loss_cost_decr()));
                payment.setReceiveProfitLossCostDecr(strParseToLong(body.getReceive_profit_loss_cost_decr()));
            }
        }

        //收款方手续费
        payment.setReceiveFeeAccountNo(body.getReceive_fee_account_no());
        payment.setReceiveFeeAccountMerchantNo(receiveFeeMerchantNo);
        payment.setReceiveFeeAccountOutAccountingAmt(strParseToLong(body.getReceive_fee_account_out_accounting_amt()));
        payment.setReceiveFeeAccountOutAccountingType(body.getReceive_fee_account_out_accounting_type());
        payment.setReceiveFeeAccountOutAccountingHis1(body.getReceive_fee_account_out_accounting_his1());
        payment.setReceiveFeeAccountOutAccountingHis2(body.getReceive_fee_account_out_accounting_his2());
        payment.setReceiveFeeAccountOutAccountingHis3(body.getReceive_fee_account_out_accounting_his3());

        long receive_in_amt = 0;
        long receive_fee_out_amt = 0;
        if (TradeConstants.TRADE_ACCOUNT_FREEZE_TYPE.equals(body.getReceive_account_in_accounting_type())) {
            receive_in_amt = strParseToLong(body.getReceive_account_in_accounting_amt());
        }
        if (TradeConstants.TRADE_ACCOUNT_FREEZE_TYPE.equals(body.getReceive_fee_account_out_accounting_type())) {
            receive_fee_out_amt = strParseToLong(body.getReceive_fee_account_out_accounting_amt());
        }
        payment.setReceiveFreezeBal(receive_in_amt - receive_fee_out_amt);

        return payment;
    }

}