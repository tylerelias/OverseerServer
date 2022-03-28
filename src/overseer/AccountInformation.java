package overseer;

import java.io.Serializable;

public class AccountInformation implements Serializable {

    private String ownerId;
    private Integer accountId;
    private Integer bankId;
    private long currentBalance;

    public AccountInformation(String ownerId, Integer accountId, Integer bankId, long currentBalance) {
        this.ownerId = ownerId;
        this.accountId = accountId;
        this.bankId = bankId;
        this.currentBalance = currentBalance;
    }

    public Integer getBankId() {
        return bankId;
    }

    public void setBankId(Integer bankId) {
        this.bankId = bankId;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
    }

    public Integer getAccountId() {
        return accountId;
    }

    public void setAccountId(Integer accountId) {
        this.accountId = accountId;
    }

    public long getCurrentBalance() {
        return currentBalance;
    }

    public long setCurrentBalance(long amount) {
        return this.currentBalance = amount;
    }
}
