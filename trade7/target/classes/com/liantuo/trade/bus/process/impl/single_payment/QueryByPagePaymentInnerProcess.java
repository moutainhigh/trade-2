package com.liantuo.trade.bus.process.impl.single_payment;

import com.liantuo.trade.bus.process.TradePageQueryInterface;
import com.liantuo.trade.bus.service.ExceptionService;
import com.liantuo.trade.bus.service.v1_1.BizTradePageQueryService;
import com.liantuo.trade.bus.template.impl.v1_1.query.ATradePageQueryNoPaymentTemp;
import com.liantuo.trade.client.trade.packet.TradeRequest;
import com.liantuo.trade.client.trade.packet.TradeResponse;
import com.liantuo.trade.client.trade.packet.body.single_payment.InnerQueryPage4SinlePaymentDetail;
import com.liantuo.trade.common.validate.TradeValidationUtil;
import com.liantuo.trade.exception.BusinessException;
import com.liantuo.trade.spring.annotation.JobFlow;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;

@JobFlow(value = "0006_001_999", version = "1.0", template = ATradePageQueryNoPaymentTemp.class)
public class QueryByPagePaymentInnerProcess implements TradePageQueryInterface<InnerQueryPage4SinlePaymentDetail> {

    @Resource(name = "paymentPageQueryServiceImpl")
    private BizTradePageQueryService bizTradePageQueryServ;

    @Resource
    protected ExceptionService exceptionService;

    @Override
    public void formatValidate(TradeRequest<InnerQueryPage4SinlePaymentDetail> tradeRquest) throws Exception {
        String errMsg;
        errMsg = TradeValidationUtil.validateRequest(tradeRquest,  HEADR_EQUIRED_INNER, HEAD_FORMAT, "*");
        if (!StringUtils.isEmpty(errMsg)) {
            throw exceptionService.buildBusinessException("JY00060019991000100", BusinessException.class, new Object[]{errMsg});
        }
    }

    @Override
    public Object query(TradeRequest<InnerQueryPage4SinlePaymentDetail> tradeRquest, TradeResponse tradeResponse)
            throws Exception {
        return this.bizTradePageQueryServ.queryByPage(tradeRquest, tradeResponse);
    }

}