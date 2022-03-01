package overseer;

import java.io.Serializable;

public class AccountInformation implements Serializable {

    private String ownerId;
    private Integer accountId;
    private Integer bankId;

    public AccountInformation(String ownerId, Integer accountId, Integer bankId) {
        this.ownerId = ownerId;
        this.accountId = accountId;
        this.bankId = bankId;
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
}
