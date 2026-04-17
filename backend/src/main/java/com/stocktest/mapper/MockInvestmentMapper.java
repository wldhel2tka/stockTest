package com.stocktest.mapper;

import com.stocktest.model.Portfolio;
import com.stocktest.model.Transaction;
import com.stocktest.model.VirtualAccount;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface MockInvestmentMapper {

    // 계좌
    VirtualAccount findAccountByUserId(Long userId);
    void createAccount(VirtualAccount account);
    void updateBalance(@Param("userId") Long userId, @Param("balance") long balance);

    // 포트폴리오
    List<Portfolio> findPortfolioByUserId(Long userId);
    Portfolio findPortfolioByUserIdAndCode(@Param("userId") Long userId, @Param("stockCode") String stockCode);
    void insertPortfolio(Portfolio portfolio);
    void updatePortfolio(Portfolio portfolio);
    void deletePortfolio(Long id);
    void deleteAllPortfolioByUserId(Long userId);

    // 거래내역
    void insertTransaction(Transaction transaction);
    List<Transaction> findTransactionsByUserId(Long userId);
    void deleteAllTransactionsByUserId(Long userId);
}
