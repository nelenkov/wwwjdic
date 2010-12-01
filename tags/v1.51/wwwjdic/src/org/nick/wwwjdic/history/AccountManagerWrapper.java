package org.nick.wwwjdic.history;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.content.Context;
import android.os.Bundle;

public class AccountManagerWrapper {

    public static final String KEY_AUTHTOKEN = "authtoken";
    public static final String KEY_INTENT = "intent";

    private static AccountManagerWrapper instance;

    private AccountManager manager;
    private List<Account> googleAccounts = new ArrayList<Account>();

    public static AccountManagerWrapper getInstance(Context context) {
        if (instance == null) {
            instance = new AccountManagerWrapper(context);
        }

        return instance;
    }

    private AccountManagerWrapper(Context context) {
        manager = AccountManager.get(context);
    }

    public String[] getGoogleAccounts() {
        Account[] accounts = manager.getAccountsByType("com.google");
        String[] result = new String[accounts.length];

        googleAccounts.clear();
        for (int i = 0; i < accounts.length; i++) {
            googleAccounts.add(accounts[i]);
            result[i] = accounts[i].name;
        }

        return result;
    }

    public Bundle getAuthToken(String accountName, String tokenType)
            throws OperationCanceledException, AuthenticatorException,
            IOException {
        Account account = findAccount(accountName);
        Bundle bundle = manager.getAuthToken(account, tokenType, true, null,
                null).getResult();

        return bundle;
    }

    private Account findAccount(String accountName) {
        for (Account account : googleAccounts) {
            if (accountName.equals(account.name)) {
                return account;
            }
        }

        return null;
    }

    public void invalidateAuthToken(String token) {
        manager.invalidateAuthToken("com.google", token);
    }
}
