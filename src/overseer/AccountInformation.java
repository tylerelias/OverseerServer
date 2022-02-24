package overseer;

import java.io.Serializable;

public class AccountInformation implements Serializable {

    private String ownerId;
    private Integer accountId;
    private String bankId;

    public AccountInformation(String ownerId, Integer accountId, String bankId) {
        this.ownerId = ownerId;
        this.accountId = accountId;
        this.bankId = bankId;
    }

    public String getBankId() {
        return bankId;
    }

    public void setBankId(String bankId) {
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
