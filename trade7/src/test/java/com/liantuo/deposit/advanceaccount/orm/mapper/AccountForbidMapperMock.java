package com.liantuo.deposit.advanceaccount.orm.mapper;

import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.doReturn;
import java.util.List;
import com.liantuo.deposit.advanceaccount.orm.mapper.AccountForbidTradeMapper;
import com.liantuo.deposit.advanceaccount.orm.pojo.AccountForbidTrade;
import com.liantuo.unittest.mockito.IsTClass;

public class AccountForbidMapperMock {
	
	public static void mockInsert(AccountForbidTradeMapper accountForbidTradeMapper,int returnObject){
		doReturn(returnObject).when(accountForbidTradeMapper).insert(
				argThat(new IsTClass<AccountForbidTrade>(new AccountForbidTrade())));
	}
	public static void mockSelectByAccouNo(AccountForbidTradeMapper accountForbidTradeMapper,List<AccountForbidTrade> returnObject){
		doReturn(returnObject).when(accountForbidTradeMapper).selectByAccouNo(
				argThat(new IsTClass<String>(new String())));
	}
	public static void mockSelectByCAAndForbidCode(AccountForbidTradeMapper accountForbidTradeMapper,List<AccountForbidTrade> returnObject){
		doReturn(returnObject).when(accountForbidTradeMapper).selectByCAAndForbidCode(
				argThat(new IsTClass<AccountForbidTrade>(new AccountForbidTrade())));
	}
	public static void mockSelectByPrimaryKey(AccountForbidTradeMapper accountForbidTradeMapper,AccountForbidTrade returnObject){
		doReturn(returnObject).when(accountForbidTradeMapper).selectByPrimaryKey(
				argThat(new IsTClass<Integer>(new Integer(0))));
	}
	public static void mockSelectWhetherForbidAllByCA(AccountForbidTradeMapper accountForbidTradeMapper,AccountForbidTrade returnObject){
		doReturn(returnObject).when(accountForbidTradeMapper).selectWhetherForbidAllByCA(
				argThat(new IsTClass<String>(new String())));
	}
	public static void mockSelectWhetherForbidThisTrade(AccountForbidTradeMapper accountForbidTradeMapper,AccountForbidTrade returnObject){
		doReturn(returnObject).when(accountForbidTradeMapper).selectWhetherForbidThisTrade(
				argThat(new IsTClass<AccountForbidTrade>(new AccountForbidTrade())));
	}

	public static void mockDeleteByPrimaryKey(AccountForbidTradeMapper accountForbidTradeMapper,int returnObject){
		doReturn(returnObject).when(accountForbidTradeMapper).deleteByPrimaryKey(
				argThat(new IsTClass<Integer>(new Integer(0))));
	}
	 
}
