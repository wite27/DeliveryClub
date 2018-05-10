package models;

import java.util.Objects;

public class CheckEntry {
    private String checkId;
    private String producerId;

    public CheckEntry(String checkId, String producerId) {
        this.checkId = checkId;
        this.producerId = producerId;
    }

    public String getCheckId() {
        return checkId;
    }

    public String getProducerId() {
        return producerId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CheckEntry that = (CheckEntry) o;
        return Objects.equals(checkId, that.checkId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(checkId);
    }
}
