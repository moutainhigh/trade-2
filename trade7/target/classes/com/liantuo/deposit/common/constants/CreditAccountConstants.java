package com.liantuo.deposit.common.constants;

public class CreditAccountConstants
{
    /** 账户属性 1-企业; 2-个人; 3-内部账户*/
    /** 账户属性 1-企业; */
    public static final Integer ACCOUNT_TYPE_ENT = 1;

    /** 账户属性 2-个人; */
    public static final Integer ACCOUNT_TYPE_IND  = 2;

    /** 账户属性 3-内部; */
    public static final Integer ACCOUNT_TYPE_INS  = 3;

    /**
     * 账户状态：0-不可用；1-正常；2-冻结状态；3-待确认；4-废弃
     */
    /** creditAccount.status:disabled */
    public static final int CREDIT_ACCOUNT_STATUS_DIABLED = 0;

    /** creditAccount.status:normal */
    public static final int CREDIT_ACCOUNT_STATUS_NORMAL = 1;

    /** 表示虚拟账户冻结状态 */
    public static final int CREDIT_ACCOUNT_STATUS_CONGEAL = 2;

    /** 表示开户后还没有确认的状态 */
    public static final int CREDIT_ACCOUNT_STATUS_CONFIRM = 3;

    /** 表示开户后账户有资金冻结 */
    public static final int CREDIT_ACCOUNT_STATUS_FBALANCE = 4;

    /** 账户类型 0-余额账户;1-授信;2-收益; 3-支付手续费账号;4-资金账户;5-待清算;6-现金账户;7-钱包待清算账户; */

    /** 账户类型 0-余额; */
    public static final Integer CREDIT_TYPE_DEBIT = 0;

    /** 账户类型 1-授信; */
    public static final Integer CREDIT_TYPE_CREDIT = 1;

    /** 账户类型 3-支付手续费账号; */
    public static final Integer CREDIT_TYPE_FEE  = 3;

    /** 账户类型 4-资金账户; */
    public static final Integer CREDIT_TYPE_FUND  = 4;

    /** 账户类型 5-待清算; */
    public static final Integer CREDIT_TYPE_CLEAR = 5;

    /** 账户类型 6-现金账户; */
    public static final Integer CREDIT_TYPE_CASH  = 6;

    /** 账户类型 7-钱包待清算账户; */
    public static final Integer CREDIT_TYPE_WALLET_CLEAR  = 7;

    /** 余额可以为负标志 0-不能为负;1-可以为负; */
    /** 余额可以为负标志 0-不能为负; */
    public static final String BALANCE_N_FLAG_N="0";

    /** 余额可以为负标志 1-可以为负; */
    public static final String BALANCE_N_FLAG_Y="1";

    public static final String PAY_CHANNEL_EBILL_BAL = "111001";// -EBILL余额

    /**
     * 系统手续费和其中的支付宝手续费 承担角色 1.无系统手续费，支付宝费用平台承担 2.核心企业承担系统和其中支付宝手续费
     * 3.成员商户承担系统和其中支付宝手续费
     */

    public static final int POLICY_ORDER_SYSTEMCHARGE_SYSTEM = 1;

    public static final int POLICY_ORDER_SYSTEMCHARGE_SUPERAGENT = 2;

    public static final int POLICY_ORDER_SYSTEMCHARGE_AGENT = 3;

}
