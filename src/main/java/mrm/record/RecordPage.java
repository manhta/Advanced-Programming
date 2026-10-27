package mrm.record;
import java.util.List;
public record RecordPage(List<RecordSummary> items, long total, int page, int size) {
    public long pages() { return (total+size-1)/size; }
}
