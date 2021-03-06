package com.liantuo.trade.bus.template.impl.v1_1.create;

import com.liantuo.payment.client.pay.PaymentResponse;
import com.liantuo.trade.bus.process.TradeCreateSingleTxHasPaymentInterface;
import com.liantuo.trade.bus.service.impl.TradeCallPaymentCenterLogServiceImpl;
import com.liantuo.trade.bus.template.AbstractTradeTemplate;
import com.liantuo.trade.client.trade.packet.TradeRequest;
import com.liantuo.trade.client.trade.packet.TradeResponse;
import com.liantuo.trade.common.annotation.Template;
import com.liantuo.trade.common.constants.TradeConstants;
import com.liantuo.trade.common.util.json.ObjectJsonUtil;
import com.liantuo.trade.exception.BusinessException;
import com.liantuo.trade.orm.pojo.TradeOutPaymentLogWithBLOBs;
import com.liantuo.trade.orm.pojo.TradeReqLogWithBLOBs;
import com.liantuo.trade.orm.service.TradeReqLogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;
import java.util.Date;

/**
 * @version V1.0
 * @Description: 创建类.有清算.单事务
 */
@Template
public class ATradeCreateSingleTxHasPaymentTemp
        extends AbstractTradeTemplate<TradeCreateSingleTxHasPaymentInterface> {

    @Resource(name = "tradeReqLogServiceImpl")
    private TradeReqLogService tradeReqLogService;
    @Resource(name = "tradeCallPaymentCenterLogService")
    private TradeCallPaymentCenterLogServiceImpl outPayLogService;//交易请求支付日志服务

    protected TradeReqLogWithBLOBs tradeLog;
    private static final Logger logger = LoggerFactory.getLogger(ATradeCreateSingleTxHasPaymentTemp.class);

    @Override
    public TradeResponse execute(TradeRequest<?> request) throws Exception {
        //数据准备
        Date startTime = new Date();
        //第零步，允许失败，需要返回系统错误
        try {
            tradeLog = tradeReqLogService.insertTradeRequestLog(request, ip, requestXML);//插入请求log
        } catch (Exception e) {
            e.printStackTrace();
            response = buildSystemExceptionResponse(e);
            return response;
        }
        process.setReqNo(tradeLog.getReqNo());

        //第一步验证
        try {
            process.validate(request);
        } catch (BusinessException e) {
            response = buildSystemExceptionResponse(e);
            try {
                tradeReqLogService.updateTradeReqLogById(tradeLog.getId(), requestXML, response, startTime);//部分更新请求log
            } catch (Exception e0) {
                e0.printStackTrace();
            }
            logger.info("在线支付---支付请求--返回数据：" + response.toString());
            return response;
        }

        //第二步,执行事务
        try {
            process.transaction(request);//事务处理
        } catch (Exception e) {//事务失败返回结果
            logger.error(e.getMessage());
            e.printStackTrace();
            response = buildSystemExceptionResponse(e);
            try {
                process.createPayFail(request);//事务失败创建充值失败记录
                response.getBody().setTrade_details(ObjectJsonUtil.object2String(process.getTradeDetails()));
            } catch (Exception ee) {
                logger.error(ee.getMessage());
                ee.printStackTrace();
                response = buildSystemExceptionResponse(ee);
            }
            try {
                tradeReqLogService.updateTradeReqLogById(tradeLog.getId(), requestXML, response, startTime);//部分更新请求log
            } catch (Exception e0) {
                logger.error(e0.getMessage());
                e0.printStackTrace();
            }
            logger.info("在线充值---支付请求--返回数据：" + response.toString());
            return response;
        }

        //############## 调用支付中心 ############
        String xml = process.createPaymentRequest(request);// 组织请求报文
        // 记录 交易系统请求支付中心时日志数据(见交易公共需求【交易系统请求支付中心需记录数据】)
        TradeOutPaymentLogWithBLOBs tradeoutLog = outPayLogService.createPaymentOutLog(request, xml);
        PaymentResponse paymentResponse;

        try {
            // 调用支付中心 暂定返回Object对象 有一个超时异常需要在实现处理并组织返回
            paymentResponse = process.requestPayment();
            // // 修改 交易系统请求支付中心时日志数据(见交易公共需求【交易系统请求支付中心需记录数据】)
            outPayLogService.updatePaymentOutLog(tradeoutLog, process.getResponseXml());
            // 从调用结果解析状态 是否获取到期望值 没有就抛异常
            process.parseObject(paymentResponse);
        } catch (Exception e) {// 请求支付中心或者解析结果异常
            logger.error(e.getMessage());
            e.printStackTrace();
            response = buildSystemExceptionResponse(e);
            // 更新交易记录为充值失败
            try {
                process.updateTradeToFailure(request);
            } catch (Exception e0) {
                logger.error(e0.getMessage());
                response = buildSystemExceptionResponse(e0);
            }
            response.getBody().setTrade_details(ObjectJsonUtil.object2String(process.getTradeDetails()));
            try {
                tradeReqLogService.updateTradeReqLogById(tradeLog.getId(), requestXML, response, startTime);// 部分更新请求log
            } catch (Exception e0) {
                e0.printStackTrace();
            }
            logger.info("在线充值---支付请求--返回数据：" + response.toString());
            return response;
        }

        try {
            // 如果解析成功 且返回成功 则更新交易记录
            process.updateTradeRecord(request);
        } catch (Exception e) {// 改失败 直接组织返回
            logger.error(e.getMessage());
            response = buildSystemExceptionResponse(e);
            response.getBody().setTrade_details(ObjectJsonUtil.object2String(process.getTradeDetails()));
            try {
                tradeReqLogService.updateTradeReqLogById(tradeLog.getId(), requestXML, response, startTime);// 部分更新请求log
            } catch (Exception e0) {
                e0.printStackTrace();
            }
            logger.info("在线充值---支付请求--返回数据：" + response.toString());
            return response;
        }

        response.getBody().setIs_success(TradeConstants.TRADE_RESPONSE_STATUS_SUCCESS);
        response.getBody().setReturn_code(TradeConstants.TRADE_RESPONSE_STATUS_SUCCESS);
        response.getBody().setTrade_details(ObjectJsonUtil.object2String(process.getTradeDetails()));
        if (paymentResponse != null) {
            response.getBody().setThird_data(ObjectJsonUtil.object2String(paymentResponse));
        }
        //第三步，允许失败，但不影响业务，正常返回
        try {
            tradeReqLogService.updateTradeReqLogById(tradeLog.getId(), requestXML, response, startTime);//部分更新请求log
        } catch (Exception e) {
            e.printStackTrace();
        }
        logger.info("在线充值---支付请求--返回数据：" + response.toString());
        return response;
    }

}