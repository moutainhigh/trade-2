package com.liantuo.deposit.testsuite.service;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.liantuo.deposit.advanceaccount.bus.service.AccountForbidServiceTest;
import com.liantuo.deposit.advanceaccount.bus.service.CreditAccountServiceMockTest;
import com.liantuo.deposit.advanceaccount.bus.service.CreditAccountTradeHistoryServiceTest;

/**
 * 账务模块pool子模块所有Service单元测试 com.liantuo.deposit.advanceaccount.*.service.*
 *
 */
@RunWith(Suite.class)
@SuiteClasses({ CreditAccountTradeHistoryServiceTest.class,
		CreditAccountServiceMockTest.class, 
		AccountForbidServiceTest.class })
public class AllAdvanceAccountServiceTestSuite {

}
