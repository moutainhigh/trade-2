package com.liantuo.trade.common.constants;

public class TradeConstants {
    public static final String TRADE_URL = "/trade/service/tx.in";
    public static final String EBILL_KEY = "trade.key";

    public static final String ENCODING = "UTF-8";

    // 请求成功 code
    public static final String TRADE_SUCCESS = "S";
    /**
     * client网络连接失败
     */
    public static final String CLIENT_ERROR_CONNECT_TIMEOUT_CODE = "JY000100101";
    /**
     * client网络连接失败
     */
    public static final String CLIENT_ERROR_CONNECT_TIMEOUT_MESSAGE = "网络连接异常";
    /**
     * client网络连接超时
     */
    public static final String CLIENT_ERROR_SOCKET_TIMEOUT_CODE = "JY000100101";
    /**
     * client网络连接超时
     */
    public static final String CLIENT_ERROR_SOCKET_TIMEOUT_MESSAGE = "网络异常SocketTimetout";

    /**
     * 格式校验失败
     */
    public static final String CLINET_VALID_ERROR_CODE = "JY000100100";

    public static final String CLINET_VALID_ERROR_MESSAGE = "格式校验未通过";

    /**
     * 支付中心统一错误码
     */
    public static final String PAYMENT_CENTER_GLOBLE_ERROR_CODE = "ZF000000100";

    /**
     * 支付中心渠道编号
     */
    public static final String PAYMENT_CENTER_PAY_CHANNEL = "ZF0001_01_001";
    public static final String ZF0001_01_002 = "ZF0001_01_002";
    public static final String ZF0001_02_001 = "ZF0001_02_001";
    public static final String ALI_ASYN_PAY_CHANNEL = "ZF0001_02_002";
    public static final String ZF0001_02_003 = "ZF0001_02_003";
    public static final String ZF0003_01_001 = "ZF0003_01_001";
    public static final String WX_ASYN_PAY_CHANNEL = "ZF0003_01_002";
    public static final String ZF0003_01_003 = "ZF0003_01_003";

    // 交易系统错误
    public static final String trade_error_ = "-1";

    //交易请求类型    成员冻结解冻
    public static final String TRADE_TYPE_PAY_NO_PWD_MERCHANT_FREEZE = "0001_001_001";
    //交易请求类型    线下付款成功,剩余解冻
    public static final String TRADE_TYPE_PAY_NO_PWD_UN_FREEZE_OFFLINE_SETTLE = "0001_001_002";
    //交易请求类型    全额解冻
    public static final String TRADE_TYPE_PAY_NO_PWD_FULL_UN_FREEEZE = "0001_001_003";
    //交易请求类型    CA付款成功,剩余解冻
    public static final String TRADE_TYPE_PAY_NO_PWD_UN_FREEEZE_ONLINE_SETTLE = "0001_001_004";
    //交易请求类型    交易列表查询  付款
    public static final String TRADE_TYPE_PAY_NO_PWD_PAGE_DETAILS = "0001_001_998";
    //交易请求 交易数据修改
    public static final String TRADE_TYPE_TRADE_PAYMENT_MODIFY_INFO = "0001_001_997";

    //交易请求类型    线下付款退回
    public static final String TRADE_TYPE_REFUND_NO_PWD_OFFLINE_SETTLE = "0001_002_001";
    //交易请求类型    交易列表查询  退款
    public static final String TRADE_TYPE_REFUND_NO_PWD_PAGE_DETAILS = "0001_002_998";

    /**
     * 提现交易 -- 扣减    <li> 0002_001_001
     */
    public static final String TRADE_WITHDRAW_DEDUCT = "0002_001_001";
    /**
     * 提现交易 -- 出款   <li> 0002_001_002
     */
    public static final String TRADE_WITHDRAW_PAY = "0002_001_002";
    /**
     * 提现交易 -- 出款通知    <li> 0002_001_003
     */
    public static final String TRADE_WITHDRAW_NOTIFY = "0002_001_003";
    /**
     * 提现交易 -- 出款查询    <li> 0002_001_004
     */
    public static final String TRADE_WITHDRAW_PAY_QUERY = "0002_001_004";
    /**
     * 提现交易 -- 退回   <li> 0002_001_005
     */
    public static final String TRADE_WITHDRAW_REFUND = "0002_001_005";
    /**
     * 提现交易 -- 列表查询  <li> 0002_001_998
     */
    public static final String TRADE_WITHDRAW_QUERY = "0002_001_998";


    /**
     * CA台账调整  <li> 0003_001_001
     */
    public static final String TRADE_ADJUST_COUNT = "0003_001_001";
    /**
     * CA台账入金  <li> 0003_001_002
     */
    public static final String TRADE_ADJUST_COUNT_LEDGER_IN = "0003_001_002";
    /**
     * CA台账出金   <li> 0003_001_003
     */
    public static final String TRADE_ADJUST_COUNT_LEDGER_OUT = "0003_001_003";
    /**
     * CA台账调整 -- 列表查询  <li> 0003_001_998
     */
    public static final String TRADE_ADJUST_COUNT_LEDGER_OUT_QUERY = "0003_001_998";
    /**
     * CA台账调整-- 内部列表查询  <li> 0003_001_999
     */
    public static final String TRADE_ADJUST_COUNT_LEDGER_INNER_QUERY = "0003_001_999";

    /**
     * 线下充值
     */
    public static final String TRADE_OFFLINE_RECHARGE = "0005_001_001";
    /**
     * 补充入账
     */
    public static final String TRADE_ADD_COUNT = "0005_001_002";
    /**
     * 在线充值批量付款请求
     */
    public static final String TRADE_APLIPAY_ONLINE_RECHARGE_REQUEST = "0005_001_004";
    /**
     * 在线充值批量付款异步通知
     */
    public static final String TRADE_APLIPAY_ONLINE_RECHARGE_ASY_NOTIFY = "0005_001_005";
    /**
     * 在线充值交易查询 0005_001_006
     */
    public static final String TRADE_ALIPAY_ONLINE_QUERY = "0005_001_006";
    /**
     * 成员充值-交易列表查询V1.0-公用0005_001_998
     */
    public static final String TRADE_MERCHANT_RECHARGE_OUTER_QUERY = "0005_001_998";
    /**
     * 成员充值-交易列表查询V1.0-内部接口0005_001_999
     */
    public static final String TRADE_MERCHANT_RECHARGE_INNER_QUERY = "0005_001_999";

    /**
     * 内部支付
     */
    public static final String TRADE_INNER_PAYMENT = "0006_001_001";
    /**
     * 内部支付类型
     */
    public static final String TRADE_INNER_PAYMENT_TYPE = "0006_001";
    /**
     * 外部支付
     */
    public static final String TRADE_OUTER_PAYMENT = "0006_001_002";
    /**
     * 外部支付类型
     */
    public static final String TRADE_OUTER_PAYMENT_TYPE = "0006_001";

    /**
     * 外部支付同步支付类型
     */
    public static final String TRADE_OUTER_PAYMENT_SYN = "0006_001_006";

    /**
     * 支付退款
     */
    public static final String TRADE_REFUND = "0006_002";
    /**
     * 内部支付退款
     */
    public static final String TRADE_INNER_PR = "0006_002_001";
    /**
     * 外部支付退款扣帐
     */
    public static final String TRADE_OUTER_PR_DEBIT = "0006_002_002";
    /**
     * 支付外部退款成功
     */
    public static final String TRADE_OUTER_PR_SUCC = "0006_002_003";
    /**
     * 支付外部退款其他渠道成功
     */
    public static final String TRADE_OUTER_PR_OTHER_CHANNEL_SUCC = "0006_002_005";
    /**
     * 外部支付收款补充入账（公用）
     */
    public static final String TRADE_OUTER_PAYMENT_SUPPLEMENT_IN_ACCOUNTING = "0006_001_003";
    /**
     * 外部收款同步获取连接
     */
    public static final String TRADE_OUTER_SINGLE_PAYMENT_ASYN_ONLINE = "0006_001_007";
    /**
     * 外部收款异步通知
     */
    public static final String TRADE_OUTER_SINGLE_PAYMENT_ASYN_NOTIFY = "0006_001_008";
    /**
     * 外部支付检查
     */
    public static final String TRADE_OUTER_PAYMENT_QUERY = "0006_001_009";
    /**
     * 外部支付收款补充入账（公用）
     */
    public static final String TRADE_OUTER_PAYMENT_RECEIVE_ACCOUNT_THAW = "0006_001_010";
    /**
     * 交易查询外部
     */
    public static final String TRADE_SINGLE_PAYMENT_PAGE_QUERY = "0006_001_998";

    /**
     * CA成员转账 0007_001_001
     */
    public static final String MERCHANT_TRANSFER = "0007_001_001";
    /**
     * CA成员转账--列表查询  0007_001_998
     */
    public static final String TRADE_MERCHANT_TRANSFER_OUT_QUERY = "0007_001_998";
    /**
     * CA成员转账--内部列表查询  0007_001_999
     */
    public static final String TRADE_MERCHANT_TRANSFER_INNER_QUERY = "0007_001_999";

    /**
     * trade系统异常
     */
    public static final String TRAED_SYSTEM_ERROR = "JY000000000000101";

    /**
     * 交易处理响应状态 交易系统处理完成 非交易系统异常
     */
    public static final String TRADE_RESPONSE_STATUS_SUCCESS = "S";

    /**
     * 交易处理响应状态 交易系统处理失败 非交易系统异常
     */
    public static final String TRADE_RESPONSE_STATUS_FAILURE = "F";

    /**
     * 交易处理响应状态 交易系统处理失败 非交易系统异常(结果未知)
     */
    public static final String TRADE_RESPONSE_STATUS_UNKNOWN = "P";

    /**
     * 交易结束状态 未结束
     */
    public static final String TRADE_CLOSE_STATUS_INIT = "0";
    /**
     * 交易结束状态 结束
     */
    public static final String TRADE_CLOSE_STATUS_END = "1";

    // 台帐状态
    public static final String LEDGER_STATUS_INIT = "00"; // 00创建
    public static final String LEDGER_STATUS_EFFECTIVITY = "01"; // 01生效
    public static final String LEDGER_STATUS_FAILED = "02"; // 02失效

    //收益台账状态 add  zoran.huang
    public static final String PROFIT_LOSS_LEDGER_STATUS_INIT = "00"; // 00空白
    public static final String PROFIT_LOSS_LEDGER_STATUS_EFFECTIVITY = "01"; // 01生效
    public static final String PROFIT_LOSS_LEDGER_STATUS_FAILED = "02"; // 02失效
    /**
     * 0001
     */
    public static final String TRADE_STATUS_INIT = "00";//00创建
    public static final String TRADE_STATUS_INIT_FAILED = "10";//10 创建失败
    public static final String TRADE_STATUS_EXCEPTION = "99";//99异常中止
    public static final String TRADE_STATUS_FREEZE = "01";//冻结成功
    public static final String TRADE_STATUS_UNFREEZE_OFFLINESETTLE = "02";//02线下付款成功,解冻，单边CA-
    public static final String TRADE_STATUS_UNFREEZE_ONLINESETTLE = "03";//03CA付款成功，解冻，双边CA-
    //    public static final String TRADE_STATUS_PAY_SUCCESS = "04";//04支付付款成功 暂不使用
    public static final String TRADE_STATUS_UNFREEZE_SUCCESS = "05";//全额解冻成功
    public static final String TRADE_STATUS_REFUND = "01";//退款成功
    /**
     * 0002
     */
    public static final String TRADE_WITHDRAW_DEDUCTING = "01"; // 01：扣减处理中 
    public static final String TRADE_WITHDRAW_DEDUCT_SUCCESS = "02"; // 02：扣减成功 
    public static final String TRADE_WITHDRAW_DEDUCT_FAILED = "03"; // 03：扣减失败 
    public static final String TRADE_WITHDRAW_PAYING = "04"; // 04：出款处理中 
    public static final String TRADE_WITHDRAW_PAY_SUCCESS = "05"; // 05:出款成功 
    public static final String TRADE_WITHDRAW_PAY_FAILED = "06"; // 06：出款失败 
    public static final String TRADE_WITHDRAW_REFUND_SUCCESS = "07"; // 07：退回成功
    public static final String TRADE_WITHDRAW_EXECPTION = "08"; // 08：交易异常处理终止

    /**
     * 0003
     * 00：创建 01：创建失败 02：调账成功 03：调账处理中 05：调账撤销  99：异常终止
     * 01：调账 02：出金外部 03： 入金外部 04： 出金损益 05：入金损益
     * 01：收入 02：成本
     */
    public static final String TRADE_ADJUST_ACCOUNT_INIT = "00";//00创建
    public static final String TRADE_ADJUST_ACCOUNT_FAILED = "01";//01创建失败
    public static final String TRADE_ADJUST_ACCOUNT_SUCCESS = "02";//02调账成功
    public static final String TRADE_ADJUST_ACCOUNT_ING = "03";//03调账处理中
    public static final String TRADE_ADJUST_ACCOUNT_REVERSE = "05";//05调账撤销
    public static final String TRADE_ADJUST_ACCOUNT_EXECPTION = "99";//99异常终止

    /**
     * 0005_001
     * 交易状态，必填属性。00：等待支付、01：等待入账、02：充值成功、03：充值失败、04：充值退回、99：交易异常处理终止
     * 请求组别，必填属性，1:线下，2：支付宝批量转账有密
     * 充值补差付出方式，可空。1：台账、2：账户
     * 充值补差台账类别，可空。1：收入减少、2：成本费用增加。充值补差付出方式为1时必输
     * 充值手续费台账类别，可空。1：收入增加、2：成本费用减少，充值手续费付出方式为1时必输
     */
    public static final String TRADE_RECHARGE_WAIT_PAY = "00";//00等待支付
    public static final String TRADE_RECHARGE_WAIT_ENTER = "01";//01等待入账
    public static final String TRADE_RECHARGE_SUCCESS = "02";//02充值成功
    public static final String TRADE_RECHARGE_PAY_FAIL = "03";//03充值失败
    public static final String TRADE_RECHARGE_RETURN = "04";//04充值退回
    public static final String TRADE_RECHARGE_EXECPTION = "99";//99异常终止

    /**
     * 0006 交易状态
     * 00：等待付款，01：等待入账，02：支付成功，03：支付失败，04：等待退回，05：支付退回, 99： 异常终止 :
     */
    public static final String TRADE_PAYMENT_WAIT_PAY = "00";
    /**
     * 0006 交易状态
     * 00：等待付款，01：等待入账，02：支付成功，03：支付失败，04：等待退回，05：支付退回, 99： 异常终止 :
     */
    public static final String TRADE_PAYMENT_WAIT_ENTER = "01";
    /**
     * 0006 交易状态
     * 00：等待付款，01：等待入账，02：支付成功，03：支付失败，04：等待退回，05：支付退回, 99： 异常终止 :
     */
    public static final String TRADE_PAYMENT_SUCCESS = "02";
    /**
     * 0006 交易状态
     * 00：等待付款，01：等待入账，02：支付成功，03：支付失败，04：等待退回，05：支付退回, 99： 异常终止 :
     */
    public static final String TRADE_PAYMENT_PAY_FAIL = "03";
    /**
     * 0006 交易状态
     * 00：等待付款，01：等待入账，02：支付成功，03：支付失败，04：等待退回，05：支付退回, 99： 异常终止 :
     */
    public static final String TRADE_PAYMENT_WAIT_RETURN = "04";
    /**
     * 0006 交易状态
     * 00：等待付款，01：等待入账，02：支付成功，03：支付失败，04：等待退回，05：支付退回, 99： 异常终止 :
     */
    public static final String TRADE_PAYMENT_RETURN = "05";
    /**
     * 0006 交易状态
     * 00：等待付款，01：等待入账，02：支付成功，03：支付失败，04：等待退回，05：支付退回, 99： 异常终止 :
     */
    public static final String TRADE_PAYMENT_EXECPTION = "99";

    // add zoran.huang start 2016-06-03

    /**
     * 0006_002 交易状态
     * 00：退款扣帐成功（等待退款结果）、01:退款成功、02:退款失败、03:退款拒绝、99:异常终止
     */
    public static final String TRADE_PAYMENT_REFUND_DEBIT_SUCCESS = "00";//退款扣帐成功（等待退款结果）
    public static final String TRADE_PAYMENT_REFUND_SUCCESS = "01";//退款成功
    public static final String TRADE_PAYMENT_REFUND_FAIL = "02";//退款失败
    public static final String TRADE_PAYMENT_REFUND_REFUSE = "03"; //退款拒绝
    public static final String TRADE_PAYMENT_REFUND_EXECPTION = "99";//异常终止

    /**
     * 0006_002 交易组别
     * 1:内部支付退款、2:外部支付退款
     */
    public static final String TRADE_PAYMENT_REFUND_INNER = "1"; //内部支付退款
    public static final String TRADE_PAYMENT_REFUND_OUTER = "2"; //外部支付退款
    public static final String TRADE_PAYMENT_THAW = "3"; //解冻

    /**
     * 0006_002  收款方退回付出方式
     * 1:账户、2:损益
     */
    public static final String TRADE_PAYMENT_REFUND_ACCOUNT = "1";//账户
    public static final String TRADE_PAYMENT_REFUND_LOSS = "2";//损益

    /**
     * 0006 账户类型
     * 1：可用、2:冻结
     */
    /**
     * 0006_002  付款方退回收到方式
     * 1:账户、2:第三方直退、3:第三方充退、4:损益直退、5:损益充退
     */
    public static final String TRADE_REFUND_PAY_ACCOUNT = "1";//账户
    public static final String TRADE_REFUND_PAY_THIRD = "2";//第三方直退
    public static final String TRADE_REFUND_PAY_THIRD_ACCOUNT = "3";//第三方充退
    public static final String TRADE_REFUND_PAY_LOSS = "4";//损益直退
    public static final String TRADE_REFUND_PAY_LOSS_ACCOUNT = "5";//损益充退

    //add zoran.huang end  2016-06-03
    public static final String TRADE_ACCONUT_AVAILABLE_TYPE = "1";//可用
    public static final String TRADE_ACCOUNT_FREEZE_TYPE = "2";//冻结

    /**
     * 充值补差付出方式，可空。1：台账、2：账户
     */
    public static final String SUPPLY_WAY_LEDGER = "1";
    /**
     * 充值补差付出方式，可空。1：台账、2：账户
     */
    public static final String SUPPLY_WAY_CA = "2";
    //充值补差台账类别
    public static final String SUPPLY_LEDGER_TYPE_INCOME_DEC = "1";//1 收入减少
    public static final String SUPPLY_LEDGER_TYPE_COSET_INC = "2";//2  成本增加
    //充值手续费台账类别
    public static final String HANDING_LEDGER_TYPE_INCOME_INC = "1";//收入增加
    public static final String HANDING_LEDGER_TYPE_COSET_DEC = "2";//成本费用减少
    /**
     * 充值手续费接收方式：1、台账；2、账户
     */
    public static final String FEE_WAY_LEDGER = "1";
    /**
     * 充值手续费接收方式：1、台账；2、账户
     */
    public static final String FEE_WAY_CA = "2";

    /**
     * 充值补差入账方式（暗补）
     */
    public static final String SUPPLY_RECHARGE_TYPE_1 = "1";

    /**
     * 0007
     */
    public static final String TRADE_TRANSFER_ACCOUNT_INIT = "00";//00创建
    public static final String TRADE_TRANSFER_ACCOUNT_FAILED = "01";//01创建失败
    public static final String TRADE_TRANSFER_ACCOUNT_SUCCESS = "02";//02转账成功
    public static final String TRADE_TRANSFER_ACCOUNT_EXECPTION = "99";//99异常终止

    public static final String TRADE_GROUP_ADJUST_ACCOUNT = "01";//调账
    public static final String TRADE_GROUP_LEDGER_OUT = "02";//出金外部
    public static final String TRADE_GROUP_LEDGER_IN = "03";//入金外部
    public static final String TRADE_GROUP_PROFIT_LOSS_OUT = "04";//出金损益
    public static final String TRADE_GROUP_PROFIT_LOSS_IN = "05";//入金损益

    public static final String TRADE_PROFIT_LOSS_TYPE_COST = "01";//收入
    public static final String TRADE_PROFIT_LOSS_TYPE_INCOME = "02";//成本

    /**
     * 支付方式组别   【1】内部支付，【2】外部支付
     */
    public static final String PAY_TYPE_BY_INNER = "1";
    /**
     * 支付方式组别   【1】内部支付，【2】外部支付
     */
    public static final String PAY_TYPE_BY_THIRD = "2";


    /**
     * 请求组别，必填属性，1:线下，2：支付宝批量转账有密
     * 1线下支付
     */
    public static final String REQUEST_GROUP_OF_OFFLINE = "1";
    /**
     * 请求组别，必填属性，1:线下，2：支付宝批量转账有密
     * 2支付宝批量转账有密
     */
    public static final String REQUEST_GROUP_OF_HASCODE = "2";

    /**
     * 提现组别
     */
    public static final String TRADE_GROUP_WITHDRAW_ALIPAY = "1"; // 提现组别-1：支付宝

    // 支付中心--支付宝--批量支付--结果
    public static final String PAYMENTCENTER_RETURN_FAILED = "-1";
    public static final String PAYMENTCENTER_RETURN_SUCCESS = "0";
    // 支付宝查询处理结果
    /**
     * -2 支付宝批次状态处理中
     */
    public static final String ALIPAY_RET_CODE_PROCESS = "-2";
    /**
     * -3 支付中心请求支付宝失败
     */
    public static final String ALIPAY_RET_CODE_FAILED = "-3";
    /**
     * -4 支付中心处理异常
     */
    public static final String ALIPAY_RET_CODE_EXCEPTION = "-4";

    // 支付中心 payinfo 返回成功
    public static final String ALIPAY_PAY_INFO_SUCCESS = "S";
    // 支付中心 payinfo 返回失败
    public static final String ALIPAY_PAY_INFO_FAILURE = "F";
    // 支付中心返回连接超时异常
    public static final String ALIPAY_PAY_INFO_CONNECT_TIME_OUT = "ZF00000000000201";
    // 支付中心返回通信超时异常
    public static final String ALIPAY_PAY_INFO_PROCCESS_TIME_OUT = "ZF00000000000301";
    // 支付中心返回网络未知异常
    public static final String ALIPAY_PAY_INFO_HTTP_ERROR = "ZF00000000000401";


    /**
     * 通知成功字符 <li> SUCCESS
     */
    public static final String NOTIFY_RESPONSE_SUCCESS_TEXT = "SUCCESS";

    public static final String NOTIFY_SOURCE_TYPE = "1";  // 来源（1：第三方）

    public static final String SERIAL_NUMBER = "001"; //请求第三方默认流水号

    public static final String WITHDRAW_ALIPAY_TRADE_TYPE = "36";//支付宝提现trade_type
    public static final String WITHDRAW_ALIPAY_REQUESTER = "1";//支付宝提现默认请求业务类型
    public static final String WITHDRAW_ALIPAY_BATCH_QUERY = "18";//支付宝提现 -- 批量查询

    public static final String WITHDRAW_ALIPAY_BANK_TYPE = "1";//提现账号编号1支付宝
    public static final String WITHDRAW_TENPAY_BANK_TYPE = "2";//提现账号编号2财付通
    public static final String WITHDRAW_CARD_BANK_TYPE = "3";//提现账号编号3银行卡

    public static final String WITHDRAW_REQUEST_GROUP = "1";//请求组别


    public static final String TRANSFER_SUCCESSFUL = "02";//转账成功


    public static final String TRADE_SINGLE_WITHDRAW_PAY_RESULT_CALL_BACK = "0512"; // 提现通知 回调通知请求前置
    //支付中心异步通知交易中心，交易中心通知前置
    public static final String TRADE_SINGLE_NOTIFY_PAY_RESULT_CALL_BACK = "5004";
    public static final String TRADE_SINGLE_EXTERNAL_PAYMENT_PAY_RESULT_CALLBACK = "6108";


    // 乐观锁默认版本
    public static final Integer DEFAULT_LOCK_VRESION = 0;

    public static final int TRADE_TYPE_PAYMENT_MAXRECORDS = 1000;//成员委托付款交易查询最大记录数

    public static final int TRADE_TYPE_REFUND_MAXRECORDS = 1000;//退款交易查询最大记录数
    /**
     * 规则：支付时间</br>
     * 正则：<font color="red">\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}</font></br>
     */
    //public final static String TRADE_REGEX_FORMAT_PAY_DATE = "\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}";

    /**
     * 支持从  2000-2399 年 的时间格式及正确性校验
     */
    public final static String TRADE_REGEX_FORMAT_PAY_DATE = "(((2[0-3][\\d]{2})-([0][13578]|1[02])-(0[1-9]|[12][\\d]|3[01]))|((2[0-3][\\d]{2})-([0][469]|11)-(0[1-9]|[12][\\d]|30))|((2[0-3][\\d]{2})-(02)-(0[1-9]|1[\\d]|2[0-8]))|(((2[0-3])([02468][048]|[13579][26]))-(02)-(29))) ([01][\\d]|2[0-3])(:[0-5][\\d]){2}";
    public final static String TRADE_REGEX_FORMAT_PAY_DATE_CAN_BLANK = "\\s||((((2[0-3][\\d]{2})-([0][13578]|1[02])-(0[1-9]|[12][\\d]|3[01]))|((2[0-3][\\d]{2})-([0][469]|11)-(0[1-9]|[12][\\d]|30))|((2[0-3][\\d]{2})-(02)-(0[1-9]|1[\\d]|2[0-8]))|(((2[0-3])([02468][048]|[13579][26]))-(02)-(29))) ([01][\\d]|2[0-3])(:[0-5][\\d]){2})";
    public final static String TRADE_REGEX_FORMAT_WX_PAY_TIME_CAN_BLANK = "\\s||(((2[0-3][\\d]{2})([0][13578]|1[02])(0[1-9]|[12][\\d]|3[01]))|((2[0-3][\\d]{2})([0][469]|11)(0[1-9]|[12][\\d]|30))|((2[0-3][\\d]{2})(02)(0[1-9]|1[\\d]|2[0-8]))|(((2[0-3])([02468][048]|[13579][26]))(02)(29)))([01][\\d]|2[0-3])([0-5][\\d]){2}";

    /**
     * 十三位长度
     */
    public static final int DIGIT_AMOUNT_LENGTH = 13;

    /**
     * 0006 付款方付出方式
     * 1-账户   2-损益直付  3-损益充付   4-第三方直付    5-第三方充付
     */
    public static final String PAY_BY_ACCOUNT = "1";
    /**
     * 0006 付款方付出方式
     * 1-账户   2-损益直付  3-损益充付   4-第三方直付    5-第三方充付
     */
    public static final String PAY_BY_PROFIT_LOSS = "2";
    /**
     * 0006 付款方付出方式
     * 1-账户   2-损益直付  3-损益充付   4-第三方直付    5-第三方充付
     */
    public static final String PAY_BY_PROFIT_LOSS_ACCOUNT = "3";
    /**
     * 0006 付款方付出方式
     * 1-账户   2-损益直付  3-损益充付   4-第三方直付    5-第三方充付
     */
    public static final String PAY_BY_THIRD = "4";
    /**
     * 0006 付款方付出方式
     * 1-账户   2-损益直付  3-损益充付   4-第三方直付    5-第三方充付
     */
    public static final String PAY_BY_THIRD_ACCOUNT = "5";

    /**
     * 0006 收款方收到方式
     * 1  账户   2  损益
     */
    public static final String RECEIVE_BY_THIRD_ACCOUNT = "1";
    /**
     * 0006 收款方收到方式
     * 1  账户   2  损益
     */
    public static final String RECEIVE_BY_THIRD_PROFIT_LOSS = "2";

    /**
     * 0006 后续交易开关
     * 0: 允许  1 ：禁止
     */
    public static final String CONTINUE_TRADE_STATUS_OPEN = "0";

    /**
     * 0006 后续交易开关
     * 0: 允许  1 ：禁止
     */
    public static final String CONTINUE_TRADE_STATUS_END = "1";


}
