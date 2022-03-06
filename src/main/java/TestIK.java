import org.elasticsearch.common.settings.Settings;
import org.wltea.analyzer.cfg.Configuration;
import org.wltea.analyzer.core.IKSegmenter;
import org.wltea.analyzer.core.Lexeme;

import java.io.IOException;
import java.io.StringReader;

public class TestIK {

    public static void main(String[] args) throws IOException {
        // ik的切词方式主要有两种，一种为smart模式，一种为ik_max_word即非smart模式
        testIkSegment();
        testIkSegmentSmart();
    }

    // TODO 不知道为啥可以分出且,处等词, 字典明明没有这个词.
    public static void testIkSegment() throws IOException {
        String t = "得饶人处且饶人<g>";
        Settings settings =  Settings.builder()
                .put("use_smart", false)
                .put("enable_lowercase", false)
                .put("enable_remote_dict", false)
                .build();
        Configuration configuration=new Configuration(null,settings).setUseSmart(false); // 读取配置，加载词典
        IKSegmenter segmenter = new IKSegmenter(new StringReader(t), configuration);
        Lexeme next;
        while ((next = segmenter.next())!=null){
            System.out.println(next.getLexemeText());
        }
    }

    public static void testIkSegmentSmart() throws IOException {
        String t = "得饶人处且饶人<g>";
        Settings settings =  Settings.builder()
                .put("use_smart", true)
                .put("enable_lowercase", false)
                .put("enable_remote_dict", false)
                .build();
        Configuration configuration=new Configuration(null,settings).setUseSmart(true); // 读取配置，加载词典 // 这个setUseSmart多余了
        IKSegmenter segmenter = new IKSegmenter(new StringReader(t), configuration);
        Lexeme next;
        while ((next = segmenter.next())!=null){
            System.out.println(next.getLexemeText());
        }
    }
}
