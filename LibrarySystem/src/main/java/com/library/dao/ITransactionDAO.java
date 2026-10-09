package com.library.dao;

import com.library.model.Transaction;
import java.util.List;

public interface ITransactionDAO {
    void issueBook(Transaction transaction);

    void returnBook(Transaction transaction);

    List<Transaction> getTransactionsByMember(int memberId);

    List<Transaction> getOverdueTransactions();

    Transaction getTransactionById(int transactionId);

    List<Transaction> getActiveTransactions();

    void clearFine(int transactionId);

    void clearAllFinesForMember(int memberId);
}