package eureca.capstone.project.batch.component.listener;

import org.springframework.batch.core.ItemWriteListener;
import org.springframework.batch.item.Chunk;
import org.springframework.stereotype.Component;

@Component
public class CustomWriterListener implements ItemWriteListener<Object> {

    @Override
    public void beforeWrite(Chunk<? extends Object> chunk) { /* 공통 로직 */ }

    @Override
    public void afterWrite(Chunk<? extends Object> chunk) { /* 공통 로직 */ }

    @Override
    public void onWriteError(Exception ex, Chunk<? extends Object> chunk) { /* 공통 실패 처리 */ }
}
