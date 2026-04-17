package com.stocktest.service;

import com.stocktest.mapper.MockInvestmentMapper;
import com.stocktest.model.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MockInvestmentService {

    private final MockInvestmentMapper mapper;
    private final StockService stockService;

    private static final long INITIAL_BALANCE = 10_000_000L;

    public VirtualAccount getOrCreateAccount(Long userId) {
        VirtualAccount account = mapper.findAccountByUserId(userId);
        if (account == null) {
            account = new VirtualAccount();
            account.setUserId(userId);
            account.setBalance(INITIAL_BALANCE);
            mapper.createAccount(account);
        }
        return account;
    }

    public PortfolioResponse getPortfolio(Long userId) {
        VirtualAccount account = getOrCreateAccount(userId);
        List<Portfolio> holdings = mapper.findPortfolioByUserId(userId);

        List<PortfolioItem> items = new ArrayList<>();
        long totalInvested = 0L;
        long totalCurrentValue = 0L;

        for (Portfolio p : holdings) {
            PortfolioItem item = new PortfolioItem();
            item.setStockCode(p.getStockCode());
            item.setStockName(p.getStockName());
            item.setQuantity(p.getQuantity());
            item.setAvgPrice(p.getAvgPrice());
            item.setTotalInvested((long) p.getQuantity() * p.getAvgPrice());

            try {
                StockPrice price = stockService.getPrice(p.getStockCode());
                item.setCurrentPrice(price.getCurrentPrice());
                item.setCurrentValue(price.getCurrentPrice() * p.getQuantity());
            } catch (Exception e) {
                item.setCurrentPrice(p.getAvgPrice());
                item.setCurrentValue(p.getAvgPrice() * p.getQuantity());
            }

            item.setPnl(item.getCurrentValue() - item.getTotalInvested());
            item.setPnlRate(item.getTotalInvested() > 0
                    ? (double) item.getPnl() / item.getTotalInvested() * 100 : 0);

            items.add(item);
            totalInvested += item.getTotalInvested();
            totalCurrentValue += item.getCurrentValue();
        }

        PortfolioResponse res = new PortfolioResponse();
        res.setBalance(account.getBalance());
        res.setTotalInvested(totalInvested);
        res.setTotalCurrentValue(totalCurrentValue);
        res.setTotalAssets(account.getBalance() + totalCurrentValue);
        res.setTotalPnl(totalCurrentValue - totalInvested);
        res.setTotalPnlRate(totalInvested > 0
                ? (double) (totalCurrentValue - totalInvested) / totalInvested * 100 : 0);
        res.setHoldings(items);
        return res;
    }

    @Transactional
    public void buy(Long userId, String code, int quantity) {
        if (quantity <= 0) throw new IllegalArgumentException("수량은 1주 이상이어야 합니다.");

        StockPrice price = stockService.getPrice(code);
        long totalCost = price.getCurrentPrice() * quantity;

        VirtualAccount account = getOrCreateAccount(userId);
        if (account.getBalance() < totalCost) {
            throw new IllegalArgumentException(
                    "잔고가 부족합니다. (필요: " + totalCost + "원, 잔고: " + account.getBalance() + "원)");
        }

        // 잔고 차감
        mapper.updateBalance(userId, account.getBalance() - totalCost);

        // 포트폴리오 업데이트
        Portfolio existing = mapper.findPortfolioByUserIdAndCode(userId, code);
        if (existing == null) {
            Portfolio portfolio = new Portfolio();
            portfolio.setUserId(userId);
            portfolio.setStockCode(code);
            portfolio.setStockName(price.getName() != null ? price.getName() : code);
            portfolio.setQuantity(quantity);
            portfolio.setAvgPrice(price.getCurrentPrice());
            mapper.insertPortfolio(portfolio);
        } else {
            long newAvgPrice = (existing.getAvgPrice() * existing.getQuantity()
                                + price.getCurrentPrice() * quantity)
                               / (existing.getQuantity() + quantity);
            existing.setQuantity(existing.getQuantity() + quantity);
            existing.setAvgPrice(newAvgPrice);
            mapper.updatePortfolio(existing);
        }

        // 거래내역 기록
        Transaction tx = new Transaction();
        tx.setUserId(userId);
        tx.setType("BUY");
        tx.setStockCode(code);
        tx.setStockName(price.getName() != null ? price.getName() : code);
        tx.setQuantity(quantity);
        tx.setPrice(price.getCurrentPrice());
        tx.setTotalAmount(totalCost);
        mapper.insertTransaction(tx);
    }

    @Transactional
    public void sell(Long userId, String code, int quantity) {
        if (quantity <= 0) throw new IllegalArgumentException("수량은 1주 이상이어야 합니다.");

        Portfolio portfolio = mapper.findPortfolioByUserIdAndCode(userId, code);
        if (portfolio == null || portfolio.getQuantity() < quantity) {
            throw new IllegalArgumentException(
                    "보유 수량이 부족합니다. (보유: " + (portfolio == null ? 0 : portfolio.getQuantity()) + "주)");
        }

        StockPrice price = stockService.getPrice(code);
        long totalAmount = price.getCurrentPrice() * quantity;

        VirtualAccount account = getOrCreateAccount(userId);
        mapper.updateBalance(userId, account.getBalance() + totalAmount);

        if (portfolio.getQuantity().equals(quantity)) {
            mapper.deletePortfolio(portfolio.getId());
        } else {
            portfolio.setQuantity(portfolio.getQuantity() - quantity);
            mapper.updatePortfolio(portfolio);
        }

        Transaction tx = new Transaction();
        tx.setUserId(userId);
        tx.setType("SELL");
        tx.setStockCode(code);
        tx.setStockName(price.getName() != null ? price.getName() : code);
        tx.setQuantity(quantity);
        tx.setPrice(price.getCurrentPrice());
        tx.setTotalAmount(totalAmount);
        mapper.insertTransaction(tx);
    }

    public List<Transaction> getTransactions(Long userId) {
        return mapper.findTransactionsByUserId(userId);
    }

    @Transactional
    public void reset(Long userId) {
        mapper.deleteAllPortfolioByUserId(userId);
        mapper.deleteAllTransactionsByUserId(userId);
        VirtualAccount account = mapper.findAccountByUserId(userId);
        if (account != null) {
            mapper.updateBalance(userId, INITIAL_BALANCE);
        } else {
            VirtualAccount newAccount = new VirtualAccount();
            newAccount.setUserId(userId);
            newAccount.setBalance(INITIAL_BALANCE);
            mapper.createAccount(newAccount);
        }
    }
}
